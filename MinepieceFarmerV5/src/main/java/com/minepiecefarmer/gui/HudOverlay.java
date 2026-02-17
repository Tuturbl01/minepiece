package com.minepiecefarmer.gui;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.config.ModConfig;
import com.minepiecefarmer.core.BossPatrol;
import com.minepiecefarmer.core.FarmerBot;
import com.minepiecefarmer.data.PlayerData;
import com.minepiecefarmer.movement.PathHelper;
import com.minepiecefarmer.util.Constants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

public class HudOverlay {

    private static final int BG = 0x90000000;
    private static final int ACCENT = 0xFF996600;

    public static void render(DrawContext ctx) {
        try {
            renderLeftPanel(ctx);
            renderRightTimers(ctx);
        } catch (Exception ignored) {}
    }

    // ═══════════ LEFT PANEL — Stats ═══════════

    private static void renderLeftPanel(DrawContext ctx) {
        ModConfig config = MinepieceFarmer.config;
        if (config == null || !config.hud.showOverlay) return;

        FarmerBot bot = MinepieceFarmer.bot;
        if (bot == null) return;
        PlayerData data = MinepieceFarmer.data;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer tr = client.textRenderer;
        int x = config.hud.overlayX, y = config.hud.overlayY;
        int lh = Constants.HUD_LINE_HEIGHT, w = 175;

        int lines = bot.isRunning() ? 10 : 3;
        int h = lines * lh + 8;

        ctx.fill(x, y, x + w, y + h, Constants.HUD_BACKGROUND_COLOR);
        ctx.fill(x, y, x + w, y + 2, Constants.HUD_ACCENT_COLOR);

        int cy = y + 4;
        dt(ctx, tr, "\u00A76\u00A7lFarmer \u00A77v5.2", x + 4, cy); cy += lh;
        dt(ctx, tr, bot.statusLine(), x + 4, cy); cy += lh;

        if (!bot.isRunning()) {
            dt(ctx, tr, "\u00A78F9 pour lancer", x + 4, cy);
            return;
        }

        int mc = bot.getMobCount();
        dt(ctx, tr, "\u00A77Mobs: " + (mc > 0 ? "\u00A7a" : "\u00A7c") + mc, x + 4, cy); cy += lh;

        if (data.hp > 0) {
            String hc = data.hp < config.safety.fleeHp ? "\u00A74" :
                         data.hp < config.fruit.hpThreshold ? "\u00A7c" : "\u00A7a";
            dt(ctx, tr, "\u00A77HP: " + hc + data.hp, x + 4, cy); cy += lh;
        }

        String hs = data.hakiActive ? "\u00A7aH" : "\u00A77H";
        String fs = config.fruit.enabled ? "\u00A7dF" : "\u00A77F";
        dt(ctx, tr, hs + " " + fs + (bot.getCombatHandler().isFruitActive() ? " \u00A7d!" : ""), x + 4, cy); cy += lh;

        dt(ctx, tr, String.format("\u00A77%s \u00A78| \u00A7c%d kills", data.sessionDuration(), data.sessionKills), x + 4, cy); cy += lh;
        dt(ctx, tr, String.format("\u00A7e%.1f k/m \u00A78| \u00A7e%d atk", data.killsPerMinute(), data.totalAttacks), x + 4, cy); cy += lh;
        dt(ctx, tr, String.format("\u00A76+%s B \u00A78(%.0f/m)", fmt(data.sessionBerriesGained), data.berriesPerMinute()), x + 4, cy); cy += lh;
        dt(ctx, tr, String.format("\u00A7a+%s XP", fmt(data.sessionXpGained)), x + 4, cy); cy += lh;

        if (!data.island.isEmpty()) {
            String is = data.island.length() > 18 ? data.island.substring(0, 18) + ".." : data.island;
            dt(ctx, tr, "\u00A7b" + is, x + 4, cy);
        }
    }

    // ═══════════ RIGHT PANEL — Timers boss/mini-boss ═══════════

    private static void renderRightTimers(DrawContext ctx) {
        ModConfig config = MinepieceFarmer.config;
        if (config == null || !config.hud.showTimers) return;

        FarmerBot bot = MinepieceFarmer.bot;
        if (bot == null || !bot.isRunning()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        TextRenderer tr = client.textRenderer;
        Map<String, Long> timers = bot.getBossPatrol().getTimerDisplays();
        if (timers.isEmpty()) return;

        int screenW = client.getWindow().getScaledWidth();
        int margin = config.hud.timersRightMargin;
        int lh = 12;
        int panelW = 140;
        int x = screenW - panelW - margin;
        int y = 5;

        int lines = timers.size() + 1;
        int h = lines * lh + 8;

        ctx.fill(x, y, x + panelW, y + h, Constants.HUD_BACKGROUND_COLOR);
        ctx.fill(x, y, x + panelW, y + 2, 0xFFCC3333);

        int cy = y + 4;
        dt(ctx, tr, "\u00A7c\u00A7lTimers", x + 4, cy); cy += lh;

        for (Map.Entry<String, Long> entry : timers.entrySet()) {
            String name = entry.getKey();
            long secs = entry.getValue();

            String timeStr;
            String color;
            if (secs < 0) {
                timeStr = "PRET";
                color = "\u00A7a";
            } else if (secs == 0) {
                timeStr = "PRET";
                color = "\u00A7a";
            } else {
                long min = secs / 60;
                long sec = secs % 60;
                timeStr = String.format("%d:%02d", min, sec);
                color = secs < 30 ? "\u00A7e" : "\u00A77";
            }

            // Tronquer le nom si trop long
            if (name.length() > Constants.HUD_TEXT_MAX_LENGTH) {
                name = name.substring(0, Constants.HUD_TEXT_MAX_LENGTH) + Constants.HUD_TEXT_TRUNCATION;
            }

            dt(ctx, tr, "\u00A77" + name + " " + color + timeStr, x + 4, cy);
            cy += lh;
        }
    }

    private static void dt(DrawContext ctx, TextRenderer tr, String text, int x, int y) {
        ctx.drawText(tr, text, x, y, 0xFFFFFF, true);
    }

    private static String fmt(long n) {
        if (n >= Constants.NUMBER_FORMAT_MILLION) {
            return String.format("%.2fM", n / (double) Constants.NUMBER_FORMAT_MILLION);
        }
        if (n >= Constants.NUMBER_FORMAT_THOUSAND) {
            return String.format("%.1fK", n / (double) Constants.NUMBER_FORMAT_THOUSAND);
        }
        return String.valueOf(n);
    }
}
