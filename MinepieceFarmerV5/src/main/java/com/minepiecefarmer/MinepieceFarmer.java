package com.minepiecefarmer;

import com.minepiecefarmer.config.ConfigManager;
import com.minepiecefarmer.config.ModConfig;
import com.minepiecefarmer.core.FarmerBot;
import com.minepiecefarmer.data.ActionBarParser;
import com.minepiecefarmer.data.BossBarParser;
import com.minepiecefarmer.data.PlayerData;
import com.minepiecefarmer.gui.FarmerScreen;
import com.minepiecefarmer.movement.PathHelper;
import com.minepiecefarmer.util.Constants;
import com.minepiecefarmer.util.ReflectionCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MinepieceFarmer v5.2 — Point d'entrée principal.
 *
 * <p>Architecture modulaire refactorisée pour qualité et performance:</p>
 * <ul>
 *   <li><b>config/</b>   → Configuration JSON validée avec auto-correction</li>
 *   <li><b>core/</b>     → FarmerBot (machine à états) et BossPatrol</li>
 *   <li><b>combat/</b>   → CombatHandler (attaque, haki, fruit) avec reflection robuste</li>
 *   <li><b>data/</b>     → Parsing boss bars, action bars, stats de session</li>
 *   <li><b>entity/</b>   → Classification et sélection de cibles optimisées</li>
 *   <li><b>movement/</b> → Intégration Baritone et contrôle de mouvement</li>
 *   <li><b>gui/</b>      → Interface utilisateur et HUD overlay</li>
 *   <li><b>mixin/</b>    → Hooks réseau et rendering</li>
 *   <li><b>util/</b>     → Constants, validation config, cache reflection</li>
 * </ul>
 *
 * <p><b>Améliorations qualité v5.2:</b></p>
 * <ul>
 *   <li>Constantes centralisées (40+ magic numbers éliminés)</li>
 *   <li>Validation de configuration automatique</li>
 *   <li>Cache de reflection avec fallback robuste</li>
 *   <li>Null checks systématiques</li>
 *   <li>Optimisations de performance (entity scan, HUD rendering)</li>
 *   <li>Logging structuré et informatif</li>
 * </ul>
 *
 * @version 5.2.0
 * @since 5.0.0
 */
public class MinepieceFarmer implements ClientModInitializer {

    public static final String MOD_ID = "minepiecefarmer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ═══════════ GLOBAL INSTANCES ═══════════
    public static ModConfig config;
    public static PlayerData data;
    public static FarmerBot bot;
    public static BossBarParser bossBarParser;
    public static ActionBarParser actionBarParser;

    // ═══════════ KEYBINDS ═══════════
    private static KeyBinding guiKey;
    private static KeyBinding toggleKey;
    private boolean guiKeyWas = false;
    private boolean toggleKeyWas = false;

    // ═══════════ TICK ═══════════
    private int tickCount = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("╔══════════════════════════════════╗");
        LOGGER.info("║  MinepieceFarmer v5.2 — Chargé!  ║");
        LOGGER.info("╚══════════════════════════════════╝");

        // ── CONFIG ──
        config = ConfigManager.load();

        // ── REFLECTION CACHE ──
        ReflectionCache.initialize();

        // ── DATA ──
        data = new PlayerData();
        bossBarParser = new BossBarParser(data);
        actionBarParser = new ActionBarParser(data);

        // ── BOT ──
        bot = new FarmerBot(data);

        // ── KEYBINDS ──
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minepiecefarmer.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.minepiecefarmer"
        ));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.minepiecefarmer.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                "category.minepiecefarmer"
        ));

        // ── BARITONE ──
        PathHelper.init();

        // ── TICK HANDLER ──
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        LOGGER.info("Touches: F8 = GUI | F9 = Toggle Farmer");
    }

    // ═══════════ TICK PRINCIPAL ═══════════

    private void onTick(MinecraftClient client) {
        if (client.player == null) return;

        tickCount++;

        // ── KEYBINDS ──
        handleKeybinds(client);

        // ── BOSS BAR PARSE (toutes les 10 ticks) ──
        if (tickCount % Constants.BOSS_BAR_PARSE_INTERVAL == 0) {
            bossBarParser.tick(client);
        }

        // ── BOT TICK ──
        bot.tick(client, config);
    }

    private void handleKeybinds(MinecraftClient client) {
        // F8 = GUI
        boolean guiNow = guiKey.isPressed();
        if (guiNow && !guiKeyWas) {
            client.setScreen(new FarmerScreen());
        }
        guiKeyWas = guiNow;

        // F9 = Toggle
        boolean toggleNow = toggleKey.isPressed();
        if (toggleNow && !toggleKeyWas) {
            bot.toggle(config);

            if (bot.isRunning()) {
                client.player.sendMessage(Text.literal(""), false);
                client.player.sendMessage(Text.literal("§6§l╔════════════════════════════╗"), false);
                client.player.sendMessage(Text.literal("§6§l║  §a§l✔ Farmer v5 ACTIVÉ §6§l      ║"), false);
                client.player.sendMessage(Text.literal("§6§l║  §7F8 = Config  F9 = Stop   §6§l║"), false);
                client.player.sendMessage(Text.literal("§6§l╚════════════════════════════╝"), false);
            } else {
                client.player.sendMessage(Text.literal(""), false);
                client.player.sendMessage(Text.literal("§4§l╔════════════════════════════╗"), false);
                client.player.sendMessage(Text.literal("§4§l║  §c§l✘ Farmer ARRÊTÉ §4§l         ║"), false);
                client.player.sendMessage(Text.literal("§4§l║  §7F9 pour relancer        §4§l║"), false);
                client.player.sendMessage(Text.literal("§4§l╚════════════════════════════╝"), false);
            }
        }
        toggleKeyWas = toggleNow;
    }

    // ═══════════ CALLBACKS DEPUIS MIXINS ═══════════

    /**
     * Appelé par le mixin quand un message action bar est reçu (overlay=true).
     * Parse les récompenses: +XP 明, +Berries 实, +Kills 军
     */
    public static void onActionBarMessage(String text) {
        if (actionBarParser != null) {
            actionBarParser.parse(text);
        }
    }

    /**
     * Appelé par le mixin quand un message chat est reçu (overlay=false).
     * Parse le haki et autres messages serveur.
     */
    public static void onChatMessage(String text) {
        if (actionBarParser != null) {
            actionBarParser.onChatMessage(text);
        }
    }

    /**
     * Appelé par le mixin quand le son illusioner.hurt est joué.
     * Indique qu'un mob a été touché.
     */
    public static void onMobHurtSound(double x, double y, double z) {
        if (data != null) {
            data.lastHurtSoundTime = System.currentTimeMillis();
        }
    }

    /**
     * Appelé par le mixin quand le son illusioner.death est joué.
     * Indique qu'un mob est mort.
     */
    public static void onMobDeathSound(double x, double y, double z) {
        if (data != null) {
            data.lastDeathSoundTime = System.currentTimeMillis();
            data.mobJustDied = true;
        }
    }
}
