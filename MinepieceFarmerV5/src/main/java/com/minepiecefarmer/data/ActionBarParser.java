package com.minepiecefarmer.data;

import com.minepiecefarmer.MinepieceFarmer;

import java.util.regex.*;

/**
 * Parse l'action bar pour extraire les récompenses de kill.
 *
 * Format confirmé par sniffer:
 *   +10 明  = +10 XP
 *   +141 实  = +141 Berries
 *   +1 军   = +1 Kill count
 *
 * L'action bar envoie plusieurs messages en rafale au même timestamp.
 * Le dernier message contient le total cumulé.
 *
 * Icônes unicode:
 *   明 = XP (font=fonts:icons)
 *   实 = Berries
 *   军 = Kills
 */
public class ActionBarParser {

    private final PlayerData data;

    // Pattern pour extraire les valeurs: +NUM ICONE
    private static final Pattern PAT_XP = Pattern.compile("\\+(\\d+).*?\u660E");
    private static final Pattern PAT_BERRIES = Pattern.compile("\\+(\\d+).*?\u5B9E");
    private static final Pattern PAT_KILLS = Pattern.compile("\\+(\\d+).*?\u519B");

    // Anti-doublon : le serveur envoie parfois le même message 2x
    private long lastParseTime = 0;
    private String lastParsedText = "";

    public ActionBarParser(PlayerData data) {
        this.data = data;
    }

    /**
     * Parse un message de l'action bar.
     * Appelé par le mixin quand un overlay=true message est reçu.
     *
     * @param text le texte brut de l'action bar (getString())
     */
    public void parse(String text) {
        if (text == null || text.isEmpty()) return;

        // Ignorer les doublons rapides (< 50ms, même texte)
        long now = System.currentTimeMillis();
        if (text.equals(lastParsedText) && now - lastParseTime < 50) return;

        // Vérifier qu'il contient au moins une icône de récompense
        if (!text.contains("\u660E") && !text.contains("\u5B9E") && !text.contains("\u519B")) return;

        int xp = 0, berries = 0, kills = 0;

        Matcher m = PAT_XP.matcher(text);
        if (m.find()) {
            try { xp = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        m = PAT_BERRIES.matcher(text);
        if (m.find()) {
            try { berries = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        m = PAT_KILLS.matcher(text);
        if (m.find()) {
            try { kills = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        if (xp > 0 || berries > 0 || kills > 0) {
            data.registerKillReward(xp, berries, kills);
        }

        lastParseTime = now;
        lastParsedText = text;
    }

    /**
     * Parse un message chat (pour haki, etc.)
     */
    public void onChatMessage(String message) {
        if (message == null) return;

        if (message.contains("activé le haki")) {
            data.hakiActive = true;
            data.hakiReady = false;
        }
        if (message.contains("haki est prêt") || message.contains("haki disponible")) {
            data.hakiReady = true;
            data.hakiActive = false;
        }
        if (message.contains("haki désactiv")) {
            data.hakiActive = false;
        }
    }
}
