package com.minepiecefarmer.data;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.mixin.BossBarAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

import java.util.*;
import java.util.regex.*;

/**
 * Parse les boss bars Minepiece pour extraire les données joueur.
 *
 * Boss Bar #0 (données joueur) :
 *   - HP : Mr(\d+)heda  ← HP RÉELLE Minepiece (pas vanilla)
 *   - Berries : 实 ([\d.]+)(K|M)?
 *   - XP : [者把意] (\d+)
 *   - Mana : 日 (\d+)
 *   - Île : 译 (.+?)eb
 *   - Timer mob : 殊 (\d+)
 *   - Timer global : 汽 (.+)$
 *
 * Boss Bar #1 :
 *   - VoteParty : VoteParty (\d+)/(\d+)
 *   - Quête île : 献 (.+?)eb
 *   - Boss kills : 哭 (.+?) (\d+) ?/ ?(\d+)
 */
public class BossBarParser {

    private final PlayerData data;

    // ═══════════ PATTERNS BOSS BAR #0 ═══════════
    private static final Pattern PAT_HP = Pattern.compile("Mr(\\d+)heda");
    private static final Pattern PAT_BERRIES = Pattern.compile("\u5B9E ([\\d.]+)(K|M|G)?");
    private static final Pattern PAT_XP = Pattern.compile("[\u8005\u628A\u610F\u660E] (\\d+)");
    private static final Pattern PAT_MANA = Pattern.compile("\u65E5 (\\d+)");
    private static final Pattern PAT_ISLAND = Pattern.compile("\u8BD1 (.+?)eb");
    private static final Pattern PAT_LEVEL = Pattern.compile("Niveau (\\d+) \\(([\\d.]+)%\\)");
    private static final Pattern PAT_TIMER_GLOBAL = Pattern.compile("\u6C7D (.+)$");
    private static final Pattern PAT_MOB_TIMER = Pattern.compile("\u6B8A (\\d+)");

    // ═══════════ PATTERNS BOSS BAR #1 ═══════════
    private static final Pattern PAT_VOTEPARTY = Pattern.compile("VoteParty (\\d+)/(\\d+)");
    private static final Pattern PAT_QUEST = Pattern.compile("\u732E (.+?)eb");
    private static final Pattern PAT_BOSS_KILLS = Pattern.compile("\u54ED (.+?) (\\d+) ?/ ?(\\d+)");

    // Tracking pour détecter les changements
    private String lastBar0Text = "";
    private String lastBar1Text = "";

    public BossBarParser(PlayerData data) {
        this.data = data;
    }

    /**
     * Lit et parse les boss bars. Appeler périodiquement (toutes les 10 ticks).
     */
    public void tick(MinecraftClient client) {
        if (client.inGameHud == null) return;

        BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
        if (!(bossBarHud instanceof BossBarAccessor accessor)) return;

        Map<UUID, ClientBossBar> bars = accessor.getBossBars();
        if (bars == null || bars.isEmpty()) return;

        List<ClientBossBar> barList = new ArrayList<>(bars.values());

        if (!barList.isEmpty()) {
            String text = barList.get(0).getName().getString();
            if (!text.equals(lastBar0Text)) {
                parseBossBar0(text);
                lastBar0Text = text;
            }
        }
        if (barList.size() > 1) {
            String text = barList.get(1).getName().getString();
            if (!text.equals(lastBar1Text)) {
                parseBossBar1(text);
                lastBar1Text = text;
            }
        }
    }

    private void parseBossBar0(String text) {
        // HP (CRITIQUE: c'est la vraie HP Minepiece, pas vanilla)
        Matcher m = PAT_HP.matcher(text);
        if (m.find()) {
            try {
                data.hp = Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // Berries
        m = PAT_BERRIES.matcher(text);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String suffix = m.group(2);
                if (suffix != null) {
                    switch (suffix) {
                        case "K" -> val *= 1_000;
                        case "M" -> val *= 1_000_000;
                        case "G" -> val *= 1_000_000_000;
                    }
                }
                data.berriesNumeric = val;
                data.berries = m.group(1) + (m.group(2) != null ? m.group(2) : "");
            } catch (NumberFormatException ignored) {}
        }

        // XP (icône peut varier selon l'île: 者 把 意 明)
        m = PAT_XP.matcher(text);
        if (m.find()) {
            try { data.xp = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        // Mana
        m = PAT_MANA.matcher(text);
        if (m.find()) {
            try { data.mana = Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }

        // Île
        m = PAT_ISLAND.matcher(text);
        if (m.find()) {
            data.island = m.group(1).trim();
        }

        // Niveau
        m = PAT_LEVEL.matcher(text);
        if (m.find()) {
            try {
                data.level = Integer.parseInt(m.group(1));
                data.levelPercent = Double.parseDouble(m.group(2));
            } catch (NumberFormatException ignored) {}
        }

        // Timer mob respawn
        m = PAT_MOB_TIMER.matcher(text);
        if (m.find()) {
            try {
                data.mobRespawnSeconds = Integer.parseInt(m.group(1));
                data.mobRespawnTimer = m.group(1) + "s";
            } catch (NumberFormatException ignored) {}
        }

        // Timer global VIP
        m = PAT_TIMER_GLOBAL.matcher(text);
        if (m.find()) {
            data.globalTimer = m.group(1).trim();
        }
    }

    private void parseBossBar1(String text) {
        Matcher m = PAT_VOTEPARTY.matcher(text);
        if (m.find()) {
            try {
                data.votePartyCurrent = Integer.parseInt(m.group(1));
                data.votePartyMax = Integer.parseInt(m.group(2));
            } catch (NumberFormatException ignored) {}
        }

        m = PAT_QUEST.matcher(text);
        if (m.find()) {
            data.islandQuest = m.group(1).trim();
        }

        m = PAT_BOSS_KILLS.matcher(text);
        if (m.find()) {
            data.bossName = m.group(1).trim();
            try {
                data.bossKillCurrent = Integer.parseInt(m.group(2));
                data.bossKillMax = Integer.parseInt(m.group(3));
            } catch (NumberFormatException ignored) {}
        }
    }
}
