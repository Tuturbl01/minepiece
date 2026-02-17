package com.minepiecefarmer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.util.ConfigValidator;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

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
     * Valide et corrige automatiquement les valeurs invalides.
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
                    
                    // Valider et corriger la configuration
                    List<String> issues = ConfigValidator.validate(config, MinepieceFarmer.LOGGER);
                    if (!issues.isEmpty()) {
                        MinepieceFarmer.LOGGER.warn("Configuration corrigée avec {} problème(s)", issues.size());
                        // Sauvegarder la config corrigée
                        save(config);
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
                    MinepieceFarmer.LOGGER.warn("Config corrompue sauvegardée en {}.bak", CONFIG_FILE);
                } catch (IOException ex) {
                    MinepieceFarmer.LOGGER.error("Impossible de sauvegarder la config corrompue: {}", ex.getMessage());
                }
            }
        }

        // Créer config par défaut
        ModConfig config = createDefaultConfig();
        save(config);
        MinepieceFarmer.LOGGER.info("Config par défaut créée: {}", path);
        return config;
    }
    
    /**
     * Crée une configuration par défaut avec validation.
     */
    private static ModConfig createDefaultConfig() {
        ModConfig config = new ModConfig();
        // La validation s'assure que les valeurs par défaut sont correctes
        ConfigValidator.validate(config, MinepieceFarmer.LOGGER);
        return config;
    }

    /**
     * Sauvegarde la config en JSON après validation.
     */
    public static void save(ModConfig config) {
        if (config == null) {
            MinepieceFarmer.LOGGER.error("Impossible de sauvegarder une config null");
            return;
        }
        
        // Valider avant sauvegarde (sans corriger, juste log)
        ConfigValidator.validate(config, MinepieceFarmer.LOGGER);
        
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            MinepieceFarmer.LOGGER.debug("Config sauvegardée: {}", path);
        } catch (Exception e) {
            MinepieceFarmer.LOGGER.error("Erreur sauvegarde config: {}", e.getMessage(), e);
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
