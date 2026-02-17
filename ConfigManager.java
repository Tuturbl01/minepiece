package com.minepiecefarmer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minepiecefarmer.MinepieceFarmer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Gère la sérialisation/désérialisation de la config en JSON.
 * Auto-crée le fichier avec les valeurs par défaut si absent.
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "minepiecefarmer.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    /**
     * Charge la config depuis le fichier JSON, ou crée un défaut.
     */
    public static ModConfig load() {
        Path path = getConfigPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    // S'assurer que les îles par défaut existent
                    if (config.islands == null || config.islands.isEmpty()) {
                        config.islands = new java.util.LinkedHashMap<>();
                        config.islands.put("whole_cake_island", IslandConfig.wholeCakeIsland());
                    }
                    MinepieceFarmer.LOGGER.info("Config chargée depuis {}", path);
                    return config;
                }
            } catch (Exception e) {
                MinepieceFarmer.LOGGER.error("Erreur lecture config: {}", e.getMessage());
                // Backup le fichier corrompu
                try {
                    Files.move(path, path.resolveSibling(CONFIG_FILE + ".bak"),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
        }

        // Créer config par défaut
        ModConfig config = ModConfig.createDefault();
        save(config);
        MinepieceFarmer.LOGGER.info("Config par défaut créée: {}", path);
        return config;
    }

    /**
     * Sauvegarde la config en JSON.
     */
    public static void save(ModConfig config) {
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            MinepieceFarmer.LOGGER.error("Erreur sauvegarde config: {}", e.getMessage());
        }
    }

    /**
     * Recharge la config depuis le disque.
     */
    public static ModConfig reload() {
        MinepieceFarmer.LOGGER.info("Rechargement config...");
        return load();
    }
}
