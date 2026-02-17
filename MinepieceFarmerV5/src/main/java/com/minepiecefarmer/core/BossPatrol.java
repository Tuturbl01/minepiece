package com.minepiecefarmer.core;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.config.IslandConfig;
import com.minepiecefarmer.config.ModConfig;
import com.minepiecefarmer.movement.PathHelper;
import com.minepiecefarmer.util.Constants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * v5.2: Les boss/mini-boss sont visités dans l'ordre de priority (1, 2, 3...).
 * Le joueur définit l'ordre dans le menu Mobs.
 */
public class BossPatrol {

    public enum PatrolState {
        FARMING, GOING_TO_BOSS, FIGHTING_BOSS, GOING_TO_MINIBOSS, WAITING_MINIBOSS
    }

    // Boss
    private long lastBossCheckTime = 0;
    private final Map<String, Long> bossKillTimes = new HashMap<>();
    private String currentBossName = null;
    private boolean goingToBoss = false;
    private boolean fightingBoss = false;

    // Mini-boss
    private String currentMiniBossName = null;
    private final Map<String, Long> miniBossKillTimes = new HashMap<>();
    private boolean goingToMiniBoss = false;

    // Coords cible actuelle (pour le HUD et la navigation)
    private Vec3d currentPatrolTarget = null;

    private PatrolState patrolState = PatrolState.FARMING;
    private int patrolTicks = 0;

    // ═══ TIMERS pour l'affichage HUD ═══
    /** Map nom → temps restant en secondes (-1 = prêt) */
    private final Map<String, Long> timerDisplays = new LinkedHashMap<>();

    public PatrolState tick(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        patrolTicks++;
        IslandConfig island = config.getActiveIsland();

        // Mettre à jour les timers d'affichage
        updateTimerDisplays(island);

        // Boss patrol
        if (config.combat.bossPatrolEnabled && !fightingBoss) {
            PatrolState bs = tickBossPatrol(client, player, config, island);
            if (bs != PatrolState.FARMING) return bs;
        }

        // Mini-boss patrol
        if (config.combat.miniBossPatrolEnabled && !fightingBoss) {
            PatrolState ms = tickMiniBossPatrol(client, player, config, island);
            if (ms != PatrolState.FARMING) return ms;
        }

        patrolState = PatrolState.FARMING;
        currentPatrolTarget = null;
        return PatrolState.FARMING;
    }

    // ═══════════ BOSS PATROL (par priority) ═══════════

    private PatrolState tickBossPatrol(MinecraftClient client, ClientPlayerEntity player,
                                        ModConfig config, IslandConfig island) {
        long now = System.currentTimeMillis();
        long checkInterval = config.combat.bossCheckIntervalSeconds * 1000L;

        // Parcourir les boss dans l'ordre de priority
        for (IslandConfig.MobInfo mob : island.getMobsByType("boss")) {
            if (mob.coords == null) continue;

            long lastKill = bossKillTimes.getOrDefault(mob.name, 0L);
            long respawnMs = mob.respawnSeconds * 1000L;

            // Timer pas écoulé
            if (lastKill > 0 && now - lastKill < respawnMs) continue;

            // Check interval (sauf si déjà en route)
            if (!goingToBoss && now - lastBossCheckTime < checkInterval) continue;

            Vec3d bossPos = new Vec3d(mob.coords[0], mob.coords[1], mob.coords[2]);
            double dist = player.getPos().distanceTo(bossPos);

            if (dist > Constants.BOSS_ARRIVAL_DISTANCE) {
                if (!goingToBoss || !mob.name.equals(currentBossName)) {
                    goingToBoss = true;
                    currentBossName = mob.name;
                    lastBossCheckTime = now;
                    log("\u00A76-> " + mob.name + " (prio " + mob.priority + ")");
                }

                currentPatrolTarget = bossPos;
                if (PathHelper.isAvailable() && patrolTicks % 60 == 0) {
                    PathHelper.pathTo(mob.coords[0], mob.coords[1], mob.coords[2]);
                }

                patrolState = PatrolState.GOING_TO_BOSS;
                return PatrolState.GOING_TO_BOSS;
            } else {
                goingToBoss = false;
                lastBossCheckTime = now;

                if (isBossNearby(client, player, bossPos)) {
                    fightingBoss = true;
                    currentBossName = mob.name;
                    log("\u00A7c\u00A7l!! " + mob.name + " !! Combat !");
                    patrolState = PatrolState.FIGHTING_BOSS;
                    return PatrolState.FIGHTING_BOSS;
                } else {
                    log("\u00A77" + mob.name + " pas la. Suivant...");
                    // Pas là → marquer comme "vérifié" pour ne pas re-check immédiatement
                    lastBossCheckTime = now;
                    break; // Passer au mini-boss patrol
                }
            }
        }

        return PatrolState.FARMING;
    }

    // ═══════════ MINI-BOSS PATROL (par priority) ═══════════

    private PatrolState tickMiniBossPatrol(MinecraftClient client, ClientPlayerEntity player,
                                            ModConfig config, IslandConfig island) {
        long now = System.currentTimeMillis();

        // Parcourir les mini-boss dans l'ordre de priority
        for (IslandConfig.MobInfo mob : island.getMobsByType("mini_boss")) {
            if (mob.coords == null) continue;

            long lastKill = miniBossKillTimes.getOrDefault(mob.name, 0L);
            long respawnMs = mob.respawnSeconds * 1000L;
            if (respawnMs <= 0) respawnMs = 300_000;

            // Timer pas écoulé
            if (lastKill > 0 && now - lastKill < respawnMs) continue;

            // Ce mini-boss est prêt ! Aller le chercher
            Vec3d targetPos = new Vec3d(mob.coords[0], mob.coords[1], mob.coords[2]);
            double dist = player.getPos().distanceTo(targetPos);

            if (dist > Constants.BOSS_ARRIVAL_DISTANCE) {
                if (!goingToMiniBoss || !mob.name.equals(currentMiniBossName)) {
                    goingToMiniBoss = true;
                    currentMiniBossName = mob.name;
                    log("\u00A7d-> Mini-boss " + mob.name + " (prio " + mob.priority + ")");
                }

                currentPatrolTarget = targetPos;
                patrolState = PatrolState.GOING_TO_MINIBOSS;
                return PatrolState.GOING_TO_MINIBOSS;
            } else {
                // On est au spawn
                goingToMiniBoss = false;
                return PatrolState.FARMING; // Farm autour du spawn
            }
        }

        return PatrolState.FARMING;
    }

    // ═══════════ NAVIGATION TARGET ═══════════

    public Vec3d getPatrolTarget() {
        return currentPatrolTarget;
    }

    // ═══════════ BOSS DETECTION ═══════════

    private boolean isBossNearby(MinecraftClient client, ClientPlayerEntity player, Vec3d pos) {
        if (client.world == null) return false;
        for (Entity e : client.world.getEntities()) {
            if (e == player) continue;
            if (!e.getType().toString().toLowerCase().contains("interaction")) continue;
            if (e.getWidth() < 0.8f || e.getHeight() < 1.0f) continue;
            if (e.getPos().distanceTo(pos) < Constants.BOSS_DETECTION_RADIUS) return true;
        }
        return false;
    }

    // ═══════════ TIMER DISPLAYS (pour HUD) ═══════════

    private void updateTimerDisplays(IslandConfig island) {
        timerDisplays.clear();
        long now = System.currentTimeMillis();

        for (IslandConfig.MobInfo mob : island.getMobsByPriority()) {
            if (!"boss".equals(mob.type) && !"mini_boss".equals(mob.type)) continue;
            if (mob.coords == null) continue;

            Map<String, Long> killMap = "boss".equals(mob.type) ? bossKillTimes : miniBossKillTimes;
            long lastKill = killMap.getOrDefault(mob.name, 0L);

            if (lastKill == 0) {
                timerDisplays.put(mob.name, -1L); // Jamais tué = prêt
            } else {
                long respawnMs = mob.respawnSeconds * 1000L;
                long remaining = respawnMs - (now - lastKill);
                timerDisplays.put(mob.name, Math.max(0, remaining / 1000));
            }
        }
    }

    /** Retourne les timers pour affichage HUD: nom → secondes restantes (-1 = prêt) */
    public Map<String, Long> getTimerDisplays() {
        return timerDisplays;
    }

    // ═══════════ NOTIFICATIONS ═══════════

    public void onBossKilled() {
        if (currentBossName != null) {
            bossKillTimes.put(currentBossName, System.currentTimeMillis());
            log("\u00A7a" + currentBossName + " tue ! Timer lance.");
        }
        fightingBoss = false;
        goingToBoss = false;
    }

    public void onBossFightAborted() {
        fightingBoss = false;
        goingToBoss = false;
    }

    public void onMiniBossKilled() {
        if (currentMiniBossName != null) {
            miniBossKillTimes.put(currentMiniBossName, System.currentTimeMillis());
            log("\u00A7a" + currentMiniBossName + " tue ! Timer lance.");
        }
        goingToMiniBoss = false;
    }

    public PatrolState getPatrolState() { return patrolState; }
    public boolean isFightingBoss() { return fightingBoss; }
    public boolean isGoingToBoss() { return goingToBoss; }
    public boolean isGoingToMiniBoss() { return goingToMiniBoss; }
    public String getCurrentTargetName() {
        if (goingToBoss && currentBossName != null) return currentBossName;
        if (goingToMiniBoss && currentMiniBossName != null) return currentMiniBossName;
        return null;
    }

    public void reset() {
        lastBossCheckTime = 0;
        bossKillTimes.clear();
        currentBossName = null;
        goingToBoss = false;
        fightingBoss = false;
        currentMiniBossName = null;
        miniBossKillTimes.clear();
        goingToMiniBoss = false;
        currentPatrolTarget = null;
        patrolState = PatrolState.FARMING;
        patrolTicks = 0;
        timerDisplays.clear();
    }

    private void log(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null)
            c.player.sendMessage(Text.literal("\u00A78[\u00A76Patrol\u00A78] " + msg), false);
    }
}
