package com.minepiecefarmer.movement;

import com.minepiecefarmer.MinepieceFarmer;
import net.minecraft.client.MinecraftClient;

/**
 * Interface avec Baritone via commandes chat (#goto, #stop).
 * Baritone intercepte ces commandes AVANT envoi au serveur.
 */
public class PathHelper {

    private static boolean initialized = false;
    private static boolean baritoneAvailable = false;
    private static boolean currentlyPathing = false;
    private static long lastGotoTime = 0;
    private static final long GOTO_COOLDOWN_MS = 2000;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Class.forName("baritone.api.IBaritoneProvider");
            baritoneAvailable = true;
            MinepieceFarmer.LOGGER.info("Baritone détecté !");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("baritone.BaritoneProvider");
                baritoneAvailable = true;
                MinepieceFarmer.LOGGER.info("Baritone détecté (alt) !");
            } catch (ClassNotFoundException e2) {
                MinepieceFarmer.LOGGER.info("Baritone non installé — fallback walk.");
                baritoneAvailable = false;
            }
        }
    }

    public static boolean isAvailable() {
        if (!initialized) init();
        return baritoneAvailable;
    }

    public static boolean pathTo(double x, double y, double z) {
        if (!isAvailable()) return false;
        long now = System.currentTimeMillis();
        if (now - lastGotoTime < GOTO_COOLDOWN_MS) return true;

        String cmd = String.format("#goto %d %d %d",
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        sendCommand(cmd);
        currentlyPathing = true;
        lastGotoTime = now;
        return true;
    }

    public static boolean pathNear(double x, double y, double z, int range) {
        return pathTo(x, y, z);
    }

    public static void cancel() {
        if (!isAvailable()) return;
        sendCommand("#stop");
        currentlyPathing = false;
    }

    public static boolean isPathing() {
        if (!isAvailable()) return false;
        if (currentlyPathing && System.currentTimeMillis() - lastGotoTime > 15000) {
            currentlyPathing = false;
        }
        return currentlyPathing;
    }

    public static void markArrived() {
        currentlyPathing = false;
    }

    private static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatMessage(command);
        }
    }
}
