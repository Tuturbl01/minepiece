package com.minepiecefarmer.util;

/**
 * Centralized constants for MinepieceFarmer mod.
 * Extracted from various classes to reduce magic numbers and improve maintainability.
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    // ═══════════ COMBAT CONSTANTS ═══════════
    
    /**
     * Number of consecutive hits without hurt sound before mob is considered invincible.
     * Used to detect when a mob is in an invincible state and should be avoided.
     */
    public static final int INVINCIBLE_HIT_THRESHOLD = 12;
    
    /**
     * Cooldown ticks when switching weapon slots.
     * Prevents rapid slot switching from causing issues.
     */
    public static final int SLOT_SWITCH_COOLDOWN = 5;

    // ═══════════ MOVEMENT & PATHFINDING ═══════════
    
    /**
     * Distance threshold for detecting when player is stuck (not moving).
     * If player moves less than this distance between checks, considered stuck.
     */
    public static final double STUCK_THRESHOLD_DISTANCE = 0.01;
    
    /**
     * Cooldown ticks before attempting to repath when stuck or approaching target.
     */
    public static final int REPATH_COOLDOWN_TICKS = 60;
    
    /**
     * Detection radius for nearby mobs when in idle state.
     * Mobs within this range will trigger combat mode.
     */
    public static final double MOB_AVOIDANCE_RADIUS = 12.0;

    // ═══════════ BOSS PATROL CONSTANTS ═══════════
    
    /**
     * Distance threshold for determining if player has arrived at boss location.
     * Used for both boss and mini-boss waypoint arrival checks.
     */
    public static final double BOSS_ARRIVAL_DISTANCE = 8.0;
    
    /**
     * Radius for detecting if a boss is nearby the player.
     * Used to determine if boss spawned or still present.
     */
    public static final double BOSS_DETECTION_RADIUS = 10.0;

    // ═══════════ IDLE WANDER CONSTANTS ═══════════
    
    /**
     * Yaw angle variation for idle wandering behavior.
     */
    public static final float IDLE_WANDER_YAW_1 = 30f;
    public static final float IDLE_WANDER_YAW_2 = 20f;
    public static final float IDLE_WANDER_YAW_3 = 40f;
    public static final float IDLE_WANDER_YAW_4 = 30f;
    
    /**
     * Pitch angle variation for idle wandering behavior.
     */
    public static final float IDLE_WANDER_PITCH_1 = 5f;
    public static final float IDLE_WANDER_PITCH_2 = 10f;

    // ═══════════ HUD DISPLAY CONSTANTS ═══════════
    
    /**
     * Background color for HUD panels (ARGB format).
     * Semi-transparent black background.
     */
    public static final int HUD_BACKGROUND_COLOR = 0x90000000;
    
    /**
     * Accent color for HUD headers (RGB format).
     * Orange color for titles and highlights.
     */
    public static final int HUD_ACCENT_COLOR = 0xFF996600;
    
    /**
     * Line height for HUD text rendering in pixels.
     */
    public static final int HUD_LINE_HEIGHT = 11;
    
    /**
     * Default panel width for HUD elements in pixels.
     */
    public static final int HUD_PANEL_WIDTH = 140;
    
    /**
     * Maximum text length before truncation in HUD displays.
     */
    public static final int HUD_TEXT_MAX_LENGTH = 10;
    
    /**
     * Truncation suffix for shortened text.
     */
    public static final String HUD_TEXT_TRUNCATION = "…";

    // ═══════════ TIMER & COOLDOWN CONSTANTS ═══════════
    
    /**
     * How often to run entity classifier scans (in ticks).
     * Default: every 10 ticks = 0.5 seconds.
     */
    public static final int ENTITY_SCAN_INTERVAL = 10;
    
    /**
     * How often to run boss bar parsing (in ticks).
     * Default: every 10 ticks = 0.5 seconds.
     */
    public static final int BOSS_BAR_PARSE_INTERVAL = 10;

    // ═══════════ VALIDATION CONSTANTS ═══════════
    
    /**
     * Minimum valid hotbar slot number (1-based).
     */
    public static final int MIN_HOTBAR_SLOT = 1;
    
    /**
     * Maximum valid hotbar slot number (1-based).
     */
    public static final int MAX_HOTBAR_SLOT = 9;
    
    /**
     * Minimum valid attack range in blocks.
     */
    public static final double MIN_ATTACK_RANGE = 0.5;
    
    /**
     * Maximum valid attack range in blocks.
     */
    public static final double MAX_ATTACK_RANGE = 6.0;
    
    /**
     * Minimum valid respawn time in seconds.
     */
    public static final int MIN_RESPAWN_TIME = 0;
    
    /**
     * Maximum valid respawn time in seconds.
     */
    public static final int MAX_RESPAWN_TIME = 86400; // 24 hours

    // ═══════════ NUMERIC FORMAT CONSTANTS ═══════════
    
    /**
     * Threshold for displaying numbers in "M" (millions) format.
     */
    public static final long NUMBER_FORMAT_MILLION = 1_000_000;
    
    /**
     * Threshold for displaying numbers in "K" (thousands) format.
     */
    public static final long NUMBER_FORMAT_THOUSAND = 1_000;
}
