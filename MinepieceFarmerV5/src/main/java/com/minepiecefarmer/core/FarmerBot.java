package com.minepiecefarmer.core;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.combat.CombatHandler;
import com.minepiecefarmer.config.ModConfig;
import com.minepiecefarmer.data.PlayerData;
import com.minepiecefarmer.entity.EntityClassifier;
import com.minepiecefarmer.entity.TargetSelector;
import com.minepiecefarmer.movement.MovementHelper;
import com.minepiecefarmer.movement.PathHelper;
import com.minepiecefarmer.util.Constants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class FarmerBot {

    public enum State {
        IDLE("\u00A77Inactif"), SEARCHING("\u00A7eRecherche"),
        APPROACHING("\u00A76Approche"), ATTACKING("\u00A7cAttaque"),
        FLEEING("\u00A74Fuite !"), USING_FRUIT("\u00A7dFruit !"),
        DEAD("\u00A78Mort");
        public final String label;
        State(String l) { this.label = l; }
    }

    private final PlayerData data;
    private final EntityClassifier classifier;
    private final TargetSelector targetSelector;
    private final CombatHandler combatHandler;
    private final BossPatrol bossPatrol;
    private final Random random = new Random();

    private State state = State.IDLE;
    private boolean running = false;
    private int stateTicks = 0, tickCount = 0;
    private int noTargetTicks = 0, searchCooldown = 0;
    private Vec3d lastPos = null, lastTargetPos = null;
    private int stuckTicks = 0, approachTicks = 0, repathCooldown = 0;
    private int jitterTimer = 0, scanTimer = 0;

    public FarmerBot(PlayerData data) {
        this.data = data;
        this.classifier = new EntityClassifier();
        this.targetSelector = new TargetSelector(classifier);
        this.combatHandler = new CombatHandler(data);
        this.bossPatrol = new BossPatrol();
    }

    public State getState() { return state; }
    public boolean isRunning() { return running; }
    public Entity getCurrentTarget() { return targetSelector.getCurrentTarget(); }
    public int getMobCount() { return targetSelector.getMobCount(); }
    public CombatHandler getCombatHandler() { return combatHandler; }
    public EntityClassifier getClassifier() { return classifier; }
    public BossPatrol getBossPatrol() { return bossPatrol; }

    public void toggle(ModConfig config) { if (running) stop(config); else start(config); }

    public void start(ModConfig config) {
        running = true;
        setState(State.SEARCHING);
        targetSelector.reset(); classifier.reset();
        combatHandler.reset(MinecraftClient.getInstance(), config);
        bossPatrol.reset();
        noTargetTicks = 0; stuckTicks = 0; approachTicks = 0;
        lastPos = null; jitterTimer = 0; scanTimer = 0; tickCount = 0;
        data.resetSession();
        PathHelper.init();
        log("\u00A7aFarmer v5.2 demarre !" +
            (PathHelper.isAvailable() ? " \u00A7b(Baritone)" : ""));
    }

    public void stop(ModConfig config) {
        running = false; setState(State.IDLE);
        targetSelector.reset();
        combatHandler.reset(MinecraftClient.getInstance(), config);
        bossPatrol.reset();
        PathHelper.cancel();
        MinecraftClient c = MinecraftClient.getInstance();
        if (c != null) {
            MovementHelper.stopMovement(c);
            c.options.useKey.setPressed(false);
            c.options.attackKey.setPressed(false);
        }
    }

    // ═══════════ TICK ═══════════

    public void tick(MinecraftClient client, ModConfig config) {
        if (!running || client.player == null || client.world == null) return;
        ClientPlayerEntity player = client.player;
        tickCount++;

        if (player.getHealth() <= 0) {
            if (state != State.DEAD) {
                setState(State.DEAD);
                targetSelector.invalidateTarget();
                PathHelper.cancel(); MovementHelper.stopMovement(client);
            }
            return;
        }

        combatHandler.tick(client, config);
        if (searchCooldown > 0) searchCooldown--;
        if (repathCooldown > 0) repathCooldown--;
        stateTicks++; jitterTimer++;

        scanTimer++;
        if (scanTimer >= config.entity.scanInterval) {
            scanTimer = 0;
            classifier.scan(client, config);
            classifier.cleanup(client);
        }

        // Fruit en cours → rien d'autre
        if (combatHandler.isFruitActive()) return;

        // Check fruit (HP bas)
        if (state != State.DEAD && state != State.USING_FRUIT) {
            if (combatHandler.checkFruit(config)) {
                if (state != State.USING_FRUIT) { setState(State.USING_FRUIT); }
                return;
            }
        }

        // Fuite HP
        if (data.hp > 0 && data.hp < config.safety.fleeHp && state != State.FLEEING && state != State.DEAD) {
            setState(State.FLEEING);
            targetSelector.invalidateTarget();
            PathHelper.cancel(); MovementHelper.stopMovement(client);
        }

        // Exclusion zone check
        if (config.getActiveIsland().isExcluded(player.getX(), player.getZ())) {
            // On est dans une zone interdite → reculer
            MovementHelper.setMovement(client, false, true, false, false);
            return;
        }

        // Boss/mini-boss patrol
        if (state != State.DEAD && state != State.FLEEING && state != State.USING_FRUIT) {
            BossPatrol.PatrolState patrol = bossPatrol.tick(client, player, config);

            if (patrol == BossPatrol.PatrolState.GOING_TO_BOSS ||
                patrol == BossPatrol.PatrolState.GOING_TO_MINIBOSS) {

                // Farm soldats en route !
                Entity nearby = targetSelector.selectTarget(client, player, config);
                if (nearby != null) {
                    double dist = player.distanceTo(nearby);
                    if (dist <= config.combat.attackRange) {
                        MovementHelper.lookAtEntity(player, nearby,
                                config.movement.lookSpeed, config.movement.lookWobble);
                        combatHandler.tryAttack(client, config);
                        combatHandler.tryHaki(client, config);
                        return;
                    } else if (dist <= Constants.MOB_AVOIDANCE_RADIUS) {
                        // Mob très proche de la route → petit détour
                        MovementHelper.lookAtEntity(player, nearby,
                                config.movement.lookSpeed, config.movement.lookWobble);
                        MovementHelper.setMovement(client, true, false, false, false);
                        return;
                    }
                }

                // Pas de mob → continuer la route
                Vec3d patrolTarget = bossPatrol.getPatrolTarget();
                if (patrolTarget != null) {
                    if (PathHelper.isAvailable()) {
                        if (repathCooldown <= 0) {
                            PathHelper.pathTo(patrolTarget.x, patrolTarget.y, patrolTarget.z);
                            repathCooldown = Constants.REPATH_COOLDOWN_TICKS;
                        }
                    } else {
                        MovementHelper.walkToward(client, player, patrolTarget,
                                config.movement.lookSpeed, config.movement.lookWobble);
                    }
                }
                return;
            }
        }

        // Timeout sécurité
        if (state != State.IDLE && state != State.DEAD && stateTicks > config.safety.stateTimeout) {
            targetSelector.invalidateTarget();
            PathHelper.cancel(); MovementHelper.stopMovement(client);
            setState(State.SEARCHING);
        }

        switch (state) {
            case SEARCHING -> tickSearching(client, player, config);
            case APPROACHING -> tickApproaching(client, player, config);
            case ATTACKING -> tickAttacking(client, player, config);
            case FLEEING -> tickFleeing(client, player, config);
            case USING_FRUIT -> { if (!combatHandler.isFruitActive()) setState(State.SEARCHING); }
            case DEAD -> { if (player.getHealth() > 0) { setState(State.SEARCHING); targetSelector.reset(); searchCooldown = 40; } }
            default -> {}
        }
    }

    private void tickSearching(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        if (searchCooldown > 0) return;
        searchCooldown = 5;

        Entity target = targetSelector.forceRetarget(client, player, config);
        if (target != null) {
            noTargetTicks = 0;
            combatHandler.resetInvincibleCounter();
            double dist = player.distanceTo(target);
            if (dist <= config.combat.attackRange) setState(State.ATTACKING);
            else {
                setState(State.APPROACHING);
                approachTicks = 0; stuckTicks = 0; lastPos = player.getPos();
                startPath(client, player, target, config);
            }
        } else {
            noTargetTicks++;
            tickIdleWander(client, player, config);
        }
    }

    private void tickIdleWander(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        if (noTargetTicks <= 60) {
            if (noTargetTicks % 20 == 0) {
                float turn = Constants.IDLE_WANDER_YAW_1 + random.nextFloat() * Constants.IDLE_WANDER_YAW_2;
                if (random.nextBoolean()) turn = -turn;
                player.setYaw(player.getYaw() + turn);
            }
        } else if (noTargetTicks <= 120) {
            if (noTargetTicks % 25 == 0) {
                float turn = Constants.IDLE_WANDER_YAW_3 + random.nextFloat() * Constants.IDLE_WANDER_YAW_4;
                if (random.nextBoolean()) turn = -turn;
                player.setYaw(player.getYaw() + turn);
            }
            MovementHelper.setMovement(client, true, false, false, false);
        } else {
            MovementHelper.stopMovement(client);
            noTargetTicks = 0;
        }
    }

    private void tickApproaching(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        Entity target = targetSelector.getCurrentTarget();
        if (target == null || target.isRemoved() || !target.isAlive()) {
            targetSelector.invalidateTarget(); 
            PathHelper.cancel();
            MovementHelper.stopMovement(client); 
            setState(State.SEARCHING); 
            return;
        }

        double dist = player.distanceTo(target);
        MovementHelper.lookAtEntity(player, target, config.movement.lookSpeed, config.movement.lookWobble);

        if (dist <= config.combat.attackRange) {
            PathHelper.cancel(); PathHelper.markArrived();
            MovementHelper.stopMovement(client); setState(State.ATTACKING); return;
        }
        if (dist > config.combat.searchRange * 1.5) {
            targetSelector.invalidateTarget(); PathHelper.cancel();
            MovementHelper.stopMovement(client); setState(State.SEARCHING); return;
        }

        approachTicks++;
        if (approachTicks > config.safety.approachTimeout) {
            targetSelector.invalidateTarget(); PathHelper.cancel();
            MovementHelper.stopMovement(client); setState(State.SEARCHING); return;
        }

        if (PathHelper.isAvailable()) {
            if (repathCooldown <= 0) {
                startPath(client, player, target, config);
                repathCooldown = 40; lastTargetPos = target.getPos();
            }
        } else {
            MovementHelper.setMovement(client, true, false, false, false);
            Vec3d cur = player.getPos();
            if (lastPos != null && MovementHelper.horizontalDistanceSq(lastPos, cur) < Constants.STUCK_THRESHOLD_DISTANCE) {
                stuckTicks++;
                if (stuckTicks > config.movement.stuckThreshold) handleStuck(client, player);
            } else { if (stuckTicks > 0 && stuckTicks < config.movement.stuckThreshold) stuckTicks = 0; }
            lastPos = cur;
        }
        combatHandler.tryHaki(client, config);
    }

    private void tickAttacking(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        Entity target = targetSelector.getCurrentTarget();
        if (target == null || target.isRemoved() || !target.isAlive()) {
            targetSelector.onTargetKilled(); combatHandler.resetInvincibleCounter();
            MovementHelper.stopMovement(client); setState(State.SEARCHING); return;
        }

        if (combatHandler.isMobInvincible()) {
            log("\u00A7eMob invincible, skip...");
            targetSelector.invalidateTarget(); combatHandler.resetInvincibleCounter();
            MovementHelper.stopMovement(client); setState(State.SEARCHING); return;
        }

        double dist = player.distanceTo(target);
        if (dist > config.combat.attackRange + 2.0) {
            setState(State.APPROACHING); approachTicks = 0; stuckTicks = 0;
            lastPos = player.getPos(); startPath(client, player, target, config); return;
        }

        Entity better = targetSelector.selectTarget(client, player, config);
        if (better != null && better != target && !better.isRemoved() && better.isAlive()) {
            double betterDist = player.distanceTo(better);
            if (betterDist <= config.combat.attackRange) {
                combatHandler.resetInvincibleCounter();
            }
        }

        combatHandler.ensureSwordEquipped(player, config);
        MovementHelper.lookAtEntity(player, target, config.movement.lookSpeed, config.movement.lookWobble);

        if (dist > config.combat.attackRange - 0.5)
            MovementHelper.setMovement(client, true, false, false, false);
        else if (dist < 1.2)
            MovementHelper.setMovement(client, false, true, false, false);
        else MovementHelper.stopMovement(client);

        combatHandler.tryAttack(client, config);
        combatHandler.tryHaki(client, config);
    }

    private void tickFleeing(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        PathHelper.cancel();
        MovementHelper.setMovement(client, false, true, false, false);
        if (data.hp >= config.safety.safeHp || data.hp == 0) {
            MovementHelper.stopMovement(client); setState(State.SEARCHING);
        }
    }

    private void setState(State s) { state = s; stateTicks = 0; }

    private void startPath(MinecraftClient c, ClientPlayerEntity p, Entity t, ModConfig cfg) {
        if (t == null || t.isRemoved()) {
            return;
        }
        if (PathHelper.isAvailable()) {
            Vec3d pos = t.getPos();
            if (pos != null) {
                PathHelper.pathNear(pos.x, pos.y, pos.z, (int) cfg.combat.attackRange);
            }
        }
    }

    private void handleStuck(MinecraftClient client, ClientPlayerEntity player) {
        int phase = (stuckTicks / 20) % 4;
        switch (phase) {
            case 0 -> { client.options.jumpKey.setPressed(true); MovementHelper.setMovement(client, true, false, false, false); }
            case 1 -> { client.options.jumpKey.setPressed(false); MovementHelper.setMovement(client, true, false, true, false); }
            case 2 -> MovementHelper.setMovement(client, true, false, false, true);
            case 3 -> { stuckTicks = 0; targetSelector.invalidateTarget(); PathHelper.cancel(); setState(State.SEARCHING); }
        }
    }

    public String statusLine() {
        StringBuilder sb = new StringBuilder(state.label);
        Entity t = targetSelector.getCurrentTarget();
        MinecraftClient client = MinecraftClient.getInstance();
        if (t != null && !t.isRemoved() && client != null && client.player != null) {
            try {
                double dist = client.player.distanceTo(t);
                sb.append(" \u00A77(").append(String.format("%.1f", dist)).append("m)");
            } catch (Exception e) {
                // Ignore distance calculation errors
            }
        }
        if (PathHelper.isAvailable() && PathHelper.isPathing()) {
            sb.append(" \u00A7b->");
        }
        if (combatHandler != null && combatHandler.isFruitActive()) {
            sb.append(" \u00A7dFRUIT");
        }

        if (bossPatrol != null) {
            BossPatrol.PatrolState ps = bossPatrol.getPatrolState();
            if (ps == BossPatrol.PatrolState.GOING_TO_BOSS) {
                sb.append(" \u00A76[BOSS]");
            } else if (ps == BossPatrol.PatrolState.GOING_TO_MINIBOSS) {
                sb.append(" \u00A7d[MINI]");
            } else if (ps == BossPatrol.PatrolState.FIGHTING_BOSS) {
                sb.append(" \u00A7c[FIGHT]");
            }

            String tn = bossPatrol.getCurrentTargetName();
            if (tn != null && !tn.isEmpty()) {
                sb.append(" \u00A77").append(tn);
            }
        }

        return sb.toString();
    }

    private void log(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null)
            c.player.sendMessage(Text.literal("\u00A78[\u00A76Farmer\u00A78] " + msg), false);
    }
}
