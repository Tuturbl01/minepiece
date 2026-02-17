package com.minepiecefarmer.combat;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.config.ModConfig;
import com.minepiecefarmer.data.PlayerData;
import com.minepiecefarmer.util.Constants;
import com.minepiecefarmer.util.ReflectionCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

/**
 * v5.2 Fixes:
 *   1. BLOCS: Vérifie crosshairTarget == EntityHitResult AVANT d'attaquer.
 *      Plus jamais de blocs cassés.
 *   2. FRUIT: Triple protection — aucun clic gauche possible quand fruit en main.
 *      Variable fruitInHand pour tracker si le slot actif = fruit.
 */
public class CombatHandler {

    private final PlayerData data;
    private final Random random = new Random();

    private int attackCooldown = 0;

    private int hakiTimer = 0;
    private int hakiReleaseTimer = -1;

    private int fruitCooldown = 0;
    private int fruitStep = -1;
    private int fruitTickCounter = 0;
    /** TRUE dès qu'on switch au fruit, FALSE quand on revient à l'épée */
    private boolean fruitInHand = false;

    private int hitsWithoutHurtSound = 0;
    private long lastKnownHurtTime = 0;

    public CombatHandler(PlayerData data) {
        this.data = data;
    }

    // ══════════════════════════════════════════════
    //  TICK
    // ══════════════════════════════════════════════

    public void tick(MinecraftClient client, ModConfig config) {
        if (client.player == null) return;

        if (attackCooldown > 0) attackCooldown--;
        if (fruitCooldown > 0) fruitCooldown--;
        hakiTimer++;

        // *** PROTECTION FRUIT: bloquer TOUT clic gauche si fruit en main ***
        if (fruitInHand) {
            client.options.attackKey.setPressed(false);
        }

        if (hakiReleaseTimer > 0) {
            hakiReleaseTimer--;
        } else if (hakiReleaseTimer == 0) {
            client.options.useKey.setPressed(false);
            hakiReleaseTimer = -1;
        }

        if (fruitStep >= 0) {
            tickFruit(client, client.player, config);
        }

        if (data.lastHurtSoundTime > lastKnownHurtTime) {
            lastKnownHurtTime = data.lastHurtSoundTime;
            hitsWithoutHurtSound = 0;
        }
    }

    // ══════════════════════════════════════════════
    //  ATTAQUE — VÉRIFIE CROSSHAIR TARGET
    // ══════════════════════════════════════════════

    /**
     * Attaque UNIQUEMENT si:
     *   - Le crosshair pointe sur une ENTITÉ (pas un bloc/air)
     *   - Le fruit N'EST PAS en main
     *   - Le cooldown est OK
     */
    public boolean tryAttack(MinecraftClient client, ModConfig config) {
        if (!config.combat.autoAttack) return false;
        if (attackCooldown > 0) return false;
        if (isFruitActive() || fruitInHand) return false;

        // *** FIX BLOCS: vérifier que le crosshair pointe sur une entité ***
        HitResult target = client.crosshairTarget;
        if (target == null || target.getType() != HitResult.Type.ENTITY) {
            // On ne pointe pas sur une entité → NE PAS ATTAQUER
            return false;
        }

        // Vérifier l'épée en main
        if (client.player != null) {
            int currentSlot = getSelectedSlot(client.player);
            if (currentSlot == config.fruitSlotIndex()) {
                selectSlot(client.player, config.swordSlotIndex());
                attackCooldown = Constants.SLOT_SWITCH_COOLDOWN;
                return false;
            }
            ensureSwordEquipped(client.player, config);
        }

        performAttack(client);
        attackCooldown = config.combat.attackCooldownMin +
                random.nextInt(Math.max(1, config.combat.attackCooldownRandom));
        data.totalAttacks++;
        hitsWithoutHurtSound++;
        return true;
    }

    public boolean isMobInvincible() {
        return hitsWithoutHurtSound >= Constants.INVINCIBLE_HIT_THRESHOLD;
    }

    public void resetInvincibleCounter() {
        hitsWithoutHurtSound = 0;
    }

    private void performAttack(MinecraftClient client) {
        // Dernière vérification avant attaque
        if (fruitInHand || isFruitActive()) return;
        
        if (client == null) {
            MinepieceFarmer.LOGGER.warn("Cannot attack: client is null");
            return;
        }

        boolean success = ReflectionCache.invokeDoAttack(client);
        if (!success) {
            MinepieceFarmer.LOGGER.debug("Attack invocation failed");
        }
    }

    // ══════════════════════════════════════════════
    //  HAKI
    // ══════════════════════════════════════════════

    public boolean tryHaki(MinecraftClient client, ModConfig config) {
        if (!config.haki.enabled) return false;
        if (hakiTimer < config.haki.intervalTicks) return false;
        if (isFruitActive() || fruitInHand) return false;

        ensureSwordEquipped(client.player, config);
        client.options.useKey.setPressed(true);
        hakiReleaseTimer = config.haki.clickDuration;
        hakiTimer = 0;
        return true;
    }

    // ══════════════════════════════════════════════
    //  FRUIT — TRIPLE PROTECTION
    // ══════════════════════════════════════════════

    public boolean checkFruit(ModConfig config) {
        if (!config.fruit.enabled) return false;
        if (fruitStep >= 0) return true;
        if (fruitCooldown > 0) return false;
        if (data.hp <= 0 || data.hp >= config.fruit.hpThreshold) return false;
        startFruit(config);
        return true;
    }

    private void startFruit(ModConfig config) {
        fruitStep = 0;
        fruitTickCounter = 0;
        fruitCooldown = config.fruit.cooldownTicks;
    }

    private void tickFruit(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        fruitTickCounter++;

        // TOUJOURS bloquer le clic gauche pendant toute la séquence
        client.options.attackKey.setPressed(false);

        switch (fruitStep) {
            case 0 -> { // SWITCH_TO_FRUIT
                selectSlot(player, config.fruitSlotIndex());
                fruitInHand = true; // MARQUÉ: fruit en main
                fruitStep = 1;
                fruitTickCounter = 0;
            }
            case 1 -> { // WAIT_SWITCH
                if (fruitTickCounter >= config.fruit.switchDelay) {
                    fruitStep = 2;
                    fruitTickCounter = 0;
                }
            }
            case 2 -> { // USE_FRUIT (clic droit)
                client.options.useKey.setPressed(true);
                fruitStep = 3;
                fruitTickCounter = 0;
            }
            case 3 -> { // HOLD_USE
                if (fruitTickCounter >= config.fruit.useDuration) {
                    fruitStep = 4;
                    fruitTickCounter = 0;
                }
            }
            case 4 -> { // RELEASE
                client.options.useKey.setPressed(false);
                fruitStep = 5;
                fruitTickCounter = 0;
            }
            case 5 -> { // WAIT_RETURN
                if (fruitTickCounter >= config.fruit.returnDelay) {
                    fruitStep = 6;
                    fruitTickCounter = 0;
                }
            }
            case 6 -> { // SWITCH_TO_SWORD
                selectSlot(player, config.swordSlotIndex());
                fruitInHand = false; // ÉPÉE DE RETOUR
                fruitStep = -1;
                fruitTickCounter = 0;
            }
        }
    }

    public boolean isFruitActive() { return fruitStep >= 0; }

    public void cancelFruit(MinecraftClient client, ModConfig config) {
        if (fruitStep >= 0) {
            client.options.useKey.setPressed(false);
            client.options.attackKey.setPressed(false);
            if (client.player != null) selectSlot(client.player, config.swordSlotIndex());
            fruitStep = -1;
            fruitTickCounter = 0;
            fruitInHand = false;
        }
    }

    // ══════════════════════════════════════════════
    //  SLOT SWITCHING
    // ══════════════════════════════════════════════

    public void ensureSwordEquipped(ClientPlayerEntity player, ModConfig config) {
        if (player == null) return;
        if (getSelectedSlot(player) != config.swordSlotIndex()) {
            selectSlot(player, config.swordSlotIndex());
            fruitInHand = false;
        }
    }

    public void selectSlot(ClientPlayerEntity player, int slot) {
        if (player == null) {
            MinepieceFarmer.LOGGER.warn("Cannot select slot: player is null");
            return;
        }
        if (slot < 0 || slot > 8) {
            MinepieceFarmer.LOGGER.warn("Invalid slot number: {}", slot);
            return;
        }
        
        boolean success = ReflectionCache.setSelectedSlot(player.getInventory(), slot);
        if (!success) {
            MinepieceFarmer.LOGGER.debug("Slot selection failed for slot {}", slot);
        }
    }

    public int getSelectedSlot(ClientPlayerEntity player) {
        if (player == null) {
            MinepieceFarmer.LOGGER.warn("Cannot get slot: player is null");
            return -1;
        }
        return ReflectionCache.getSelectedSlot(player.getInventory());
    }

    public void reset(MinecraftClient client, ModConfig config) {
        cancelFruit(client, config);
        attackCooldown = 0;
        hakiTimer = 0;
        hakiReleaseTimer = -1;
        fruitCooldown = 0;
        fruitInHand = false;
        hitsWithoutHurtSound = 0;
        lastKnownHurtTime = 0;
    }
}
