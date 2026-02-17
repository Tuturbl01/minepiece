package com.minepiecefarmer.util;

import com.minepiecefarmer.config.ModConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates and sanitizes configuration values to prevent invalid settings.
 * Logs warnings for any corrected values.
 */
public class ConfigValidator {

    /**
     * Validates and corrects all configuration values.
     * Returns a list of validation warnings/errors that were corrected.
     */
    public static List<String> validate(ModConfig config, Logger logger) {
        List<String> issues = new ArrayList<>();

        // Validate combat settings
        validateCombat(config.combat, issues);

        // Validate fruit settings
        validateFruit(config.fruit, issues);

        // Validate haki settings
        validateHaki(config.haki, issues);

        // Validate safety settings
        validateSafety(config.safety, issues);

        // Validate movement settings
        validateMovement(config.movement, issues);

        // Validate entity settings
        validateEntity(config.entity, issues);

        // Validate HUD settings
        validateHud(config.hud, issues);

        // Log all issues
        for (String issue : issues) {
            logger.warn("Config validation: {}", issue);
        }

        return issues;
    }

    private static void validateCombat(ModConfig.CombatConfig combat, List<String> issues) {
        // Validate sword slot (1-9)
        if (combat.swordSlot < Constants.MIN_HOTBAR_SLOT || combat.swordSlot > Constants.MAX_HOTBAR_SLOT) {
            issues.add(String.format("Invalid sword_slot %d, correcting to %d", 
                combat.swordSlot, Constants.MIN_HOTBAR_SLOT));
            combat.swordSlot = Constants.MIN_HOTBAR_SLOT;
        }

        // Validate attack range
        if (combat.attackRange < Constants.MIN_ATTACK_RANGE) {
            issues.add(String.format("Attack range %.2f too small, correcting to %.2f", 
                combat.attackRange, Constants.MIN_ATTACK_RANGE));
            combat.attackRange = Constants.MIN_ATTACK_RANGE;
        }
        if (combat.attackRange > Constants.MAX_ATTACK_RANGE) {
            issues.add(String.format("Attack range %.2f too large, correcting to %.2f", 
                combat.attackRange, Constants.MAX_ATTACK_RANGE));
            combat.attackRange = Constants.MAX_ATTACK_RANGE;
        }

        // Validate search range (must be >= attack range)
        if (combat.searchRange < combat.attackRange) {
            issues.add(String.format("Search range %.2f < attack range %.2f, correcting to %.2f", 
                combat.searchRange, combat.attackRange, combat.attackRange * 2));
            combat.searchRange = combat.attackRange * 2;
        }

        // Validate cooldowns (must be non-negative)
        if (combat.attackCooldownMin < 0) {
            issues.add(String.format("Attack cooldown min %d negative, correcting to 0", 
                combat.attackCooldownMin));
            combat.attackCooldownMin = 0;
        }
        if (combat.attackCooldownRandom < 0) {
            issues.add(String.format("Attack cooldown random %d negative, correcting to 0", 
                combat.attackCooldownRandom));
            combat.attackCooldownRandom = 0;
        }

        // Validate boss check interval (minimum 10 seconds)
        if (combat.bossCheckIntervalSeconds < 10) {
            issues.add(String.format("Boss check interval %d too short, correcting to 10", 
                combat.bossCheckIntervalSeconds));
            combat.bossCheckIntervalSeconds = 10;
        }

        // Validate invincible threshold (minimum 1)
        if (combat.invincibleThreshold < 1) {
            issues.add(String.format("Invincible threshold %d invalid, correcting to %d", 
                combat.invincibleThreshold, Constants.INVINCIBLE_HIT_THRESHOLD));
            combat.invincibleThreshold = Constants.INVINCIBLE_HIT_THRESHOLD;
        }

        // Validate target switch distance (must be positive)
        if (combat.targetSwitchDistance <= 0) {
            issues.add(String.format("Target switch distance %.2f invalid, correcting to 5.0", 
                combat.targetSwitchDistance));
            combat.targetSwitchDistance = 5.0;
        }

        // Validate retarget interval (must be positive)
        if (combat.retargetInterval <= 0) {
            issues.add(String.format("Retarget interval %d invalid, correcting to 20", 
                combat.retargetInterval));
            combat.retargetInterval = 20;
        }
    }

    private static void validateFruit(ModConfig.FruitConfig fruit, List<String> issues) {
        // Validate fruit slot (1-9)
        if (fruit.slot < Constants.MIN_HOTBAR_SLOT || fruit.slot > Constants.MAX_HOTBAR_SLOT) {
            issues.add(String.format("Invalid fruit slot %d, correcting to %d", 
                fruit.slot, Constants.MAX_HOTBAR_SLOT));
            fruit.slot = Constants.MAX_HOTBAR_SLOT;
        }

        // Validate HP threshold (must be positive)
        if (fruit.hpThreshold <= 0) {
            issues.add(String.format("Fruit HP threshold %d invalid, correcting to 1000", 
                fruit.hpThreshold));
            fruit.hpThreshold = 1000;
        }

        // Validate cooldown (must be non-negative)
        if (fruit.cooldownTicks < 0) {
            issues.add(String.format("Fruit cooldown %d negative, correcting to 0", 
                fruit.cooldownTicks));
            fruit.cooldownTicks = 0;
        }

        // Validate delays (must be non-negative)
        if (fruit.switchDelay < 0) {
            issues.add("Fruit switch delay negative, correcting to 0");
            fruit.switchDelay = 0;
        }
        if (fruit.useDuration < 0) {
            issues.add("Fruit use duration negative, correcting to 1");
            fruit.useDuration = 1;
        }
        if (fruit.returnDelay < 0) {
            issues.add("Fruit return delay negative, correcting to 0");
            fruit.returnDelay = 0;
        }
    }

    private static void validateHaki(ModConfig.HakiConfig haki, List<String> issues) {
        // Validate interval (must be positive)
        if (haki.intervalTicks <= 0) {
            issues.add(String.format("Haki interval %d invalid, correcting to 100", 
                haki.intervalTicks));
            haki.intervalTicks = 100;
        }

        // Validate click duration (must be positive)
        if (haki.clickDuration <= 0) {
            issues.add(String.format("Haki click duration %d invalid, correcting to 3", 
                haki.clickDuration));
            haki.clickDuration = 3;
        }
    }

    private static void validateSafety(ModConfig.SafetyConfig safety, List<String> issues) {
        // Validate HP values (must be positive and flee < safe)
        if (safety.fleeHp <= 0) {
            issues.add(String.format("Flee HP %d invalid, correcting to 100", safety.fleeHp));
            safety.fleeHp = 100;
        }
        if (safety.safeHp <= 0) {
            issues.add(String.format("Safe HP %d invalid, correcting to 200", safety.safeHp));
            safety.safeHp = 200;
        }
        if (safety.fleeHp >= safety.safeHp) {
            issues.add(String.format("Flee HP %d >= Safe HP %d, correcting flee to %d", 
                safety.fleeHp, safety.safeHp, safety.safeHp / 2));
            safety.fleeHp = safety.safeHp / 2;
        }

        // Validate timeouts (must be positive)
        if (safety.stateTimeout <= 0) {
            issues.add(String.format("State timeout %d invalid, correcting to 1200", 
                safety.stateTimeout));
            safety.stateTimeout = 1200;
        }
        if (safety.approachTimeout <= 0) {
            issues.add(String.format("Approach timeout %d invalid, correcting to 200", 
                safety.approachTimeout));
            safety.approachTimeout = 200;
        }

        // Validate jitter interval (must be positive)
        if (safety.jitterInterval <= 0) {
            issues.add(String.format("Jitter interval %d invalid, correcting to 100", 
                safety.jitterInterval));
            safety.jitterInterval = 100;
        }
    }

    private static void validateMovement(ModConfig.MovementConfig movement, List<String> issues) {
        // Validate look speed (0.01 to 1.0)
        if (movement.lookSpeed < 0.01f || movement.lookSpeed > 1.0f) {
            issues.add(String.format("Look speed %.2f out of range, correcting to 0.15", 
                movement.lookSpeed));
            movement.lookSpeed = 0.15f;
        }

        // Validate look wobble (0.0 to 1.0)
        if (movement.lookWobble < 0.0f || movement.lookWobble > 1.0f) {
            issues.add(String.format("Look wobble %.2f out of range, correcting to 0.2", 
                movement.lookWobble));
            movement.lookWobble = 0.2f;
        }

        // Validate fast look speed (0.01 to 1.0)
        if (movement.lookSpeedFast < 0.01f || movement.lookSpeedFast > 1.0f) {
            issues.add(String.format("Look speed fast %.2f out of range, correcting to 0.35", 
                movement.lookSpeedFast));
            movement.lookSpeedFast = 0.35f;
        }

        // Validate stuck threshold (must be positive)
        if (movement.stuckThreshold <= 0) {
            issues.add(String.format("Stuck threshold %d invalid, correcting to 60", 
                movement.stuckThreshold));
            movement.stuckThreshold = 60;
        }
    }

    private static void validateEntity(ModConfig.EntityConfig entity, List<String> issues) {
        // Validate heights and widths (must be positive)
        if (entity.minInteractionHeight <= 0) {
            issues.add("Min interaction height invalid, correcting to 0.5");
            entity.minInteractionHeight = 0.5f;
        }
        if (entity.minMobWidth <= 0) {
            issues.add("Min mob width invalid, correcting to 0.3");
            entity.minMobWidth = 0.3f;
        }
        if (entity.maxMobWidth <= entity.minMobWidth) {
            issues.add("Max mob width <= min, correcting to 2.0");
            entity.maxMobWidth = 2.0f;
        }

        // Validate group radius (must be positive)
        if (entity.groupRadius <= 0) {
            issues.add("Group radius invalid, correcting to 0.5");
            entity.groupRadius = 0.5;
        }

        // Validate scan interval (must be positive, recommended 5-20)
        if (entity.scanInterval <= 0) {
            issues.add(String.format("Scan interval %d invalid, correcting to %d", 
                entity.scanInterval, Constants.ENTITY_SCAN_INTERVAL));
            entity.scanInterval = Constants.ENTITY_SCAN_INTERVAL;
        }
        if (entity.scanInterval > 100) {
            issues.add(String.format("Scan interval %d very high, may affect responsiveness", 
                entity.scanInterval));
        }

        // Validate mob max text displays (must be non-negative)
        if (entity.mobMaxTextDisplays < 0) {
            issues.add("Mob max text displays negative, correcting to 1");
            entity.mobMaxTextDisplays = 1;
        }
    }

    private static void validateHud(ModConfig.HudConfig hud, List<String> issues) {
        // Validate overlay position (must be non-negative)
        if (hud.overlayX < 0) {
            issues.add("HUD overlay X negative, correcting to 5");
            hud.overlayX = 5;
        }
        if (hud.overlayY < 0) {
            issues.add("HUD overlay Y negative, correcting to 5");
            hud.overlayY = 5;
        }

        // Validate timers margin (must be non-negative)
        if (hud.timersRightMargin < 0) {
            issues.add("Timers right margin negative, correcting to 5");
            hud.timersRightMargin = 5;
        }

        // Warn if overlay position is too large (might be off-screen)
        if (hud.overlayX > 1920) {
            issues.add(String.format("HUD overlay X %d very large, may be off-screen", 
                hud.overlayX));
        }
        if (hud.overlayY > 1080) {
            issues.add(String.format("HUD overlay Y %d very large, may be off-screen", 
                hud.overlayY));
        }
    }
}
