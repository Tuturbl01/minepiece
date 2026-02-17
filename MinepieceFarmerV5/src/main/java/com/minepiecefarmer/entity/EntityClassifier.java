package com.minepiecefarmer.entity;

import com.minepiecefarmer.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.InteractionEntity;

import java.util.*;

/**
 * Classifie les entités Minepiece en mob/NPC/décor.
 *
 * Architecture mob Minepiece (confirmée par sniffer):
 *   Chaque mob = groupe d'entités empilées au même XZ:
 *   - area_effect_cloud (base invisible)
 *   - interaction (HITBOX - c'est ça qu'on cible et frappe)
 *   - item_display × N (apparence visuelle)
 *   - text_display × 1 (barre de vie)
 *
 * Différenciation:
 *   MOB:   1 text_display associé, interaction height ~2.0
 *   NPC:   2+ text_display (nom + dialogue), interaction height ~1.3
 *   DÉCOR: 4+ interactions (meubles ItemsAdder), ou 0 text_display
 *   SHOP:  item_frame avec customName "ItemsAdder_furniture"
 *
 * On scanne par rayon autour de chaque interaction pour compter les text_display.
 */
public class EntityClassifier {

    public enum EntityType {
        MOB,
        NPC,
        DECOR,
        UNKNOWN
    }

    // Cache des classifications (entityId → type)
    private final Map<Integer, EntityType> classificationCache = new HashMap<>();
    private final Map<Integer, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_LIFETIME_MS = 5000; // 5s

    // Comptage text_display par zone
    private final Map<Long, Integer> textDisplayCountByZone = new HashMap<>();
    private final Map<Long, Integer> interactionCountByZone = new HashMap<>();

    /**
     * Scanne toutes les entités du monde et met à jour les comptages.
     * Optimisé pour éviter les itérations inutiles.
     */
    public void scan(MinecraftClient client, ModConfig config) {
        if (client.world == null || client.player == null) return;

        textDisplayCountByZone.clear();
        interactionCountByZone.clear();

        double groupRadius = config.entity.groupRadius;
        ClientPlayerEntity player = client.player;
        double scanRangeSquared = config.combat.searchRange * config.combat.searchRange * 1.5 * 1.5;

        // Only scan entities within reasonable range of player to reduce overhead
        for (Entity entity : client.world.getEntities()) {
            if (entity == null) continue;
            
            // Skip entities far from player to reduce processing
            try {
                double distSq = entity.squaredDistanceTo(player);
                if (distSq > scanRangeSquared) continue;
            } catch (Exception e) {
                // If distance check fails, skip this entity
                continue;
            }

            String typeName = getTypeName(entity);
            if (typeName == null) continue;

            // Quantifier le XZ (arrondi à 0.5 bloc pour grouper)
            long zoneKey = quantizePos(entity.getX(), entity.getZ(), groupRadius);

            if (typeName.contains("text_display")) {
                textDisplayCountByZone.merge(zoneKey, 1, Integer::sum);
            } else if (typeName.contains("interaction")) {
                interactionCountByZone.merge(zoneKey, 1, Integer::sum);
            }
        }
    }

    /**
     * Classifie une entité interaction spécifique.
     *
     * @return le type d'entité
     */
    public EntityType classify(Entity entity, ModConfig config) {
        if (entity == null) return EntityType.UNKNOWN;

        int id = entity.getId();

        // Check cache
        long now = System.currentTimeMillis();
        if (classificationCache.containsKey(id)) {
            Long timestamp = cacheTimestamps.get(id);
            if (timestamp != null && now - timestamp < CACHE_LIFETIME_MS) {
                return classificationCache.get(id);
            }
        }

        EntityType type = doClassify(entity, config);
        classificationCache.put(id, type);
        cacheTimestamps.put(id, now);
        return type;
    }

    private EntityType doClassify(Entity entity, ModConfig config) {
        String typeName = getTypeName(entity);

        // Doit être une interaction
        if (!typeName.contains("interaction")) return EntityType.DECOR;

        float w = entity.getWidth();
        float h = entity.getHeight();

        // Filtres de taille de base
        if (w <= 0.01f || h <= 0.01f) return EntityType.DECOR;
        if (h <= config.entity.minInteractionHeight) return EntityType.DECOR;
        if (w < config.entity.minMobWidth) return EntityType.DECOR;
        if (w >= config.entity.maxMobWidth) return EntityType.DECOR;

        // Compter les text_display dans la même zone
        double groupRadius = config.entity.groupRadius;
        long zoneKey = quantizePos(entity.getX(), entity.getZ(), groupRadius);

        int textDisplayCount = textDisplayCountByZone.getOrDefault(zoneKey, 0);
        int interactionCount = interactionCountByZone.getOrDefault(zoneKey, 0);

        // 4+ interactions au même endroit = meuble/décor ItemsAdder
        if (interactionCount >= 4) return EntityType.DECOR;

        // 2+ text_display = NPC (nom + dialogue)
        if (textDisplayCount > config.entity.mobMaxTextDisplays) return EntityType.NPC;

        // 1 text_display + 1 interaction = MOB
        if (textDisplayCount >= 1 && interactionCount <= 2) return EntityType.MOB;

        // 0 text_display = probablement décor, mais peut être un mob pas encore touché
        // On le marque UNKNOWN pour laisser le MobTracker trancher via mouvement/agro
        if (textDisplayCount == 0) return EntityType.UNKNOWN;

        return EntityType.MOB;
    }

    /**
     * Vérifie si une entité est potentiellement un mob ciblable
     * (interaction avec bonne taille, pas un décor évident).
     */
    public boolean isPotentialMob(Entity entity, ModConfig config) {
        EntityType type = classify(entity, config);
        return type == EntityType.MOB || type == EntityType.UNKNOWN;
    }

    /**
     * Nettoie le cache des entités disparues.
     */
    public void cleanup(MinecraftClient client) {
        if (client.world == null) return;

        long now = System.currentTimeMillis();
        classificationCache.entrySet().removeIf(entry -> {
            Long ts = cacheTimestamps.get(entry.getKey());
            return ts != null && now - ts > CACHE_LIFETIME_MS * 3;
        });
        cacheTimestamps.entrySet().removeIf(entry -> now - entry.getValue() > CACHE_LIFETIME_MS * 3);
    }

    /**
     * Force le reset complet (changement d'île, etc.)
     */
    public void reset() {
        classificationCache.clear();
        cacheTimestamps.clear();
        textDisplayCountByZone.clear();
        interactionCountByZone.clear();
    }

    // ═══════════ UTILS ═══════════

    private static String getTypeName(Entity entity) {
        if (entity == null) return "";
        try {
            return entity.getType().toString().toLowerCase();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Quantifie une position XZ en clé de zone pour regrouper les entités proches.
     */
    private static long quantizePos(double x, double z, double radius) {
        int qx = (int) Math.floor(x / radius);
        int qz = (int) Math.floor(z / radius);
        return ((long) qx << 32) | (qz & 0xFFFFFFFFL);
    }
}
