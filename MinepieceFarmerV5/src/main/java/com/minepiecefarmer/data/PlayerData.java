package com.minepiecefarmer.data;

/**
 * Données live du joueur, parsées depuis les boss bars, action bar et sons.
 * 
 * HP réelle = depuis boss bar (Mr(\d+)heda), PAS le vanilla (20 HP max).
 * Le serveur Minepiece utilise des barres de vie custom avec des échelons.
 */
public class PlayerData {

    // ═══════════ BOSS BAR #0 ═══════════
    /** HP Minepiece (ex: 2700, pas le vanilla 20) */
    public volatile int hp = 0;
    public volatile int maxHp = 0;
    public volatile String berries = "0";
    public volatile double berriesNumeric = 0;
    public volatile int xp = 0;
    public volatile int mana = 0;
    public volatile String island = "";
    public volatile int level = 0;
    public volatile double levelPercent = 0;
    public volatile String globalTimer = "";

    // ═══════════ BOSS BAR #1 ═══════════
    public volatile int votePartyCurrent = 0;
    public volatile int votePartyMax = 0;
    public volatile String bossName = "";
    public volatile int bossKillCurrent = 0;
    public volatile int bossKillMax = 0;
    public volatile String islandQuest = "";

    // ═══════════ BOSS BAR - RESPAWN TIMER ═══════════
    /** Timer de respawn mob lu depuis la boss bar (eb殊 format) */
    public volatile String mobRespawnTimer = "";
    public volatile int mobRespawnSeconds = -1;

    // ═══════════ HAKI ═══════════
    public volatile boolean hakiActive = false;
    public volatile boolean hakiReady = false;

    // ═══════════ COMBAT STATS (session) ═══════════
    public volatile int sessionKills = 0;
    public volatile int sessionXpGained = 0;
    public volatile long sessionBerriesGained = 0;
    public volatile long sessionStartTime = 0;
    public volatile int totalAttacks = 0;

    // ═══════════ DERNIÈRE RÉCOMPENSE ═══════════
    public volatile int lastRewardXp = 0;
    public volatile int lastRewardBerries = 0;
    public volatile int lastRewardKills = 0;
    public volatile long lastKillTime = 0;

    // ═══════════ SON COMBAT ═══════════
    public volatile long lastHurtSoundTime = 0;
    public volatile long lastDeathSoundTime = 0;
    public volatile boolean mobJustDied = false;

    // ═══════════ FRUIT ═══════════
    /** true si le fruit est détecté dans l'inventaire */
    public volatile boolean fruitDetected = false;
    public volatile int fruitSlotActual = -1; // -1 = pas trouvé

    /**
     * Reset les stats de session.
     */
    public void resetSession() {
        sessionKills = 0;
        sessionXpGained = 0;
        sessionBerriesGained = 0;
        sessionStartTime = System.currentTimeMillis();
        totalAttacks = 0;
    }

    /**
     * Enregistre une récompense de kill (depuis l'action bar).
     */
    public void registerKillReward(int xp, int berries, int kills) {
        if (xp > 0) {
            lastRewardXp = xp;
            sessionXpGained += xp;
        }
        if (berries > 0) {
            lastRewardBerries = berries;
            sessionBerriesGained += berries;
        }
        if (kills > 0) {
            lastRewardKills = kills;
            sessionKills += kills;
        }
        lastKillTime = System.currentTimeMillis();
    }

    /**
     * Kills par minute depuis le début de la session.
     */
    public double killsPerMinute() {
        if (sessionStartTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (elapsed < 1000) return 0;
        return (sessionKills * 60000.0) / elapsed;
    }

    /**
     * Berries par minute.
     */
    public double berriesPerMinute() {
        if (sessionStartTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (elapsed < 1000) return 0;
        return (sessionBerriesGained * 60000.0) / elapsed;
    }

    /**
     * Durée de session formatée.
     */
    public String sessionDuration() {
        if (sessionStartTime == 0) return "0:00";
        long elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000;
        long hours = elapsed / 3600;
        long mins = (elapsed % 3600) / 60;
        long secs = elapsed % 60;
        if (hours > 0) {
            return String.format("%dh%02dm%02ds", hours, mins, secs);
        }
        return String.format("%dm%02ds", mins, secs);
    }
}
