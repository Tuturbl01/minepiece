package com.minepiecefarmer.config;

import com.google.gson.annotations.SerializedName;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {

    @SerializedName("version")
    public String version = "5.2.0";

    @SerializedName("active_island")
    public String activeIsland = "whole_cake";

    // ═══════════ KEYBINDS ═══════════
    public KeybindConfig keybinds = new KeybindConfig();
    public static class KeybindConfig {
        public int guiKey = 67;  // F8
        public int toggleKey = 68; // F9
    }

    // ═══════════ COMBAT ═══════════
    public CombatConfig combat = new CombatConfig();
    public static class CombatConfig {
        @SerializedName("sword_slot") public int swordSlot = 1;
        @SerializedName("attack_range") public double attackRange = 3.5;
        @SerializedName("search_range") public double searchRange = 40.0;
        @SerializedName("attack_cooldown_min") public int attackCooldownMin = 8;
        @SerializedName("attack_cooldown_random") public int attackCooldownRandom = 4;
        @SerializedName("auto_attack") public boolean autoAttack = true;
        @SerializedName("farm_soldiers") public boolean farmSoldiers = true;
        @SerializedName("farm_mini_boss") public boolean farmMiniBoss = true;
        @SerializedName("target_priority") public String targetPriority = "closest";
        @SerializedName("target_switch_distance") public double targetSwitchDistance = 5.0;
        @SerializedName("retarget_interval") public int retargetInterval = 20;
        @SerializedName("boss_patrol_enabled") public boolean bossPatrolEnabled = true;
        @SerializedName("boss_check_interval_seconds") public int bossCheckIntervalSeconds = 210;
        @SerializedName("mini_boss_patrol_enabled") public boolean miniBossPatrolEnabled = true;
        @SerializedName("invincible_threshold") public int invincibleThreshold = 12;
    }

    // ═══════════ FRUIT ═══════════
    public FruitConfig fruit = new FruitConfig();
    public static class FruitConfig {
        public boolean enabled = true;
        public int slot = 9;
        @SerializedName("hp_threshold") public int hpThreshold = 2000;
        @SerializedName("cooldown_ticks") public int cooldownTicks = 200;
        @SerializedName("switch_delay") public int switchDelay = 5;
        @SerializedName("use_duration") public int useDuration = 15;
        @SerializedName("return_delay") public int returnDelay = 5;
    }

    // ═══════════ HAKI ═══════════
    public HakiConfig haki = new HakiConfig();
    public static class HakiConfig {
        public boolean enabled = true;
        @SerializedName("interval_ticks") public int intervalTicks = 600;
        @SerializedName("click_duration") public int clickDuration = 3;
    }

    // ═══════════ SAFETY ═══════════
    public SafetyConfig safety = new SafetyConfig();
    public static class SafetyConfig {
        @SerializedName("flee_hp") public int fleeHp = 200;
        @SerializedName("safe_hp") public int safeHp = 500;
        @SerializedName("state_timeout") public int stateTimeout = 1200;
        @SerializedName("approach_timeout") public int approachTimeout = 200;
        @SerializedName("anti_afk") public boolean antiAfk = true;
        @SerializedName("jitter_interval") public int jitterInterval = 100;
    }

    // ═══════════ MOVEMENT ═══════════
    public MovementConfig movement = new MovementConfig();
    public static class MovementConfig {
        @SerializedName("look_speed") public float lookSpeed = 0.15f;
        @SerializedName("look_wobble") public float lookWobble = 0.2f;
        @SerializedName("look_speed_fast") public float lookSpeedFast = 0.35f;
        @SerializedName("auto_sprint") public boolean autoSprint = false;
        @SerializedName("stuck_threshold") public int stuckThreshold = 60;
    }

    // ═══════════ ENTITY ═══════════
    public EntityConfig entity = new EntityConfig();
    public static class EntityConfig {
        @SerializedName("min_interaction_height") public float minInteractionHeight = 0.5f;
        @SerializedName("min_mob_width") public float minMobWidth = 0.3f;
        @SerializedName("max_mob_width") public float maxMobWidth = 2.0f;
        @SerializedName("group_radius") public double groupRadius = 0.5;
        @SerializedName("mob_max_text_displays") public int mobMaxTextDisplays = 1;
        @SerializedName("scan_interval") public int scanInterval = 10;
    }

    // ═══════════ HUD ═══════════
    public HudConfig hud = new HudConfig();
    public static class HudConfig {
        @SerializedName("show_overlay") public boolean showOverlay = true;
        @SerializedName("overlay_x") public int overlayX = 5;
        @SerializedName("overlay_y") public int overlayY = 5;
        @SerializedName("show_timers") public boolean showTimers = true;
        @SerializedName("timers_right_margin") public int timersRightMargin = 5;
    }

    // ═══════════ ISLANDS ═══════════
    public Map<String, IslandConfig> islands = new LinkedHashMap<>();

    {
        islands.put("whole_cake", IslandConfig.wholeCakeIsland());
    }

    // ═══════════ HELPERS ═══════════

    public IslandConfig getActiveIsland() {
        return islands.getOrDefault(activeIsland, IslandConfig.wholeCakeIsland());
    }

    public int swordSlotIndex() { return combat.swordSlot - 1; }
    public int fruitSlotIndex() { return fruit.slot - 1; }
}
