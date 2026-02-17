package com.minepiecefarmer.entity;

import com.minepiecefarmer.MinepieceFarmer;
import com.minepiecefarmer.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Sélectionne la cible optimale parmi les mobs détectés.
 *
 * FIX CRITIQUE v5: Le v4 prenait des cibles aléatoires car le tracking
 * par mouvement créait des biais. Maintenant:
 *
 * 1. On utilise EntityClassifier pour filtrer (pas de movement tracking)
 * 2. On trie TOUJOURS par distance au joueur (closest first)
 * 3. Re-évaluation périodique pour switch si un mob spawn plus proche
 * 4. Sticky target: on garde la cible actuelle sauf si une autre est
 *    significativement plus proche (évite le flip-flop)
 *
 * Modes de priorité:
 *   - "closest" (défaut): le plus proche
 *   - "lowest_hp": le plus bas en HP (si on peut lire les HP)
 *   - "highest_value": mini-boss en priorité
 */
public class TargetSelector {

    private final EntityClassifier classifier;

    /** Cible actuelle (sticky) */
    private Entity currentTarget = null;
    private int retargetTimer = 0;

    /** Cache des mobs valides */
    private final List<ScoredEntity> validTargets = new ArrayList<>();
    private int cachedMobCount = 0;

    public TargetSelector(EntityClassifier classifier) {
        this.classifier = classifier;
    }

    /**
     * Récupère la meilleure cible.
     * Appelé chaque tick par le bot.
     *
     * @return la meilleure cible, ou null si aucun mob
     */
    public Entity selectTarget(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        retargetTimer++;

        // Si on a une cible valide et qu'on n'a pas besoin de re-évaluer, la garder
        if (currentTarget != null && isValidTarget(currentTarget, player, config)) {
            if (retargetTimer < config.combat.retargetInterval) {
                return currentTarget;
            }
        }

        // Re-évaluation
        retargetTimer = 0;
        return reevaluateTarget(client, player, config);
    }

    /**
     * Force une re-évaluation immédiate (cible morte, etc.)
     */
    public Entity forceRetarget(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        retargetTimer = 0;
        currentTarget = null;
        return reevaluateTarget(client, player, config);
    }

    /**
     * Re-évalue toutes les cibles et choisit la meilleure.
     */
    private Entity reevaluateTarget(MinecraftClient client, ClientPlayerEntity player, ModConfig config) {
        if (client.world == null) return null;

        validTargets.clear();
        Vec3d playerPos = player.getPos();

        // Scanner toutes les entités
        for (Entity entity : client.world.getEntities()) {
            if (entity == player) continue;
            if (!isValidTarget(entity, player, config)) continue;

            double dist = playerPos.distanceTo(entity.getPos());
            if (dist > config.combat.searchRange) continue;

            validTargets.add(new ScoredEntity(entity, dist));
        }

        cachedMobCount = validTargets.size();

        if (validTargets.isEmpty()) {
            currentTarget = null;
            return null;
        }

        // Trier selon la priorité
        sortByPriority(config.combat.targetPriority);

        Entity bestTarget = validTargets.get(0).entity;

        // Sticky target: garder la cible actuelle sauf si la nouvelle est
        // significativement plus proche (évite le flip-flop entre 2 mobs)
        if (currentTarget != null && isValidTarget(currentTarget, player, config)) {
            double currentDist = playerPos.distanceTo(currentTarget.getPos());
            double bestDist = validTargets.get(0).distance;

            // Garder la cible actuelle si elle est encore à portée raisonnable
            // et que la nouvelle n'est pas > switchDistance blocs plus proche
            if (currentDist <= config.combat.searchRange &&
                currentDist - bestDist < config.combat.targetSwitchDistance) {
                return currentTarget;
            }
        }

        currentTarget = bestTarget;
        return currentTarget;
    }

    /**
     * Vérifie qu'une entité est une cible valide.
     */
    private boolean isValidTarget(Entity entity, ClientPlayerEntity player, ModConfig config) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return false;

        // Doit être une interaction
        String typeName = entity.getType().toString().toLowerCase();
        if (!typeName.contains("interaction")) return false;
        if (typeName.contains("armor") || typeName.contains("item_frame")) return false;

        float w = entity.getWidth();
        float h = entity.getHeight();

        // Filtres de taille
        if (w <= 0.01f || h <= 0.01f) return false;
        if (h <= config.entity.minInteractionHeight) return false;
        if (w < config.entity.minMobWidth) return false;
        if (w >= config.entity.maxMobWidth) return false;

        // Classification par EntityClassifier
        if (!classifier.isPotentialMob(entity, config)) return false;

        // Filtre par catégorie (soldat vs mini-boss basé sur la largeur)
        if (w < 0.8f) {
            if (!config.combat.farmSoldiers) return false;
        } else {
            if (!config.combat.farmMiniBoss) return false;
        }

        return true;
    }

    /**
     * Trie les cibles selon la priorité configurée.
     */
    private void sortByPriority(String priority) {
        switch (priority) {
            case "closest" -> validTargets.sort(Comparator.comparingDouble(e -> e.distance));
            case "lowest_hp" -> {
                // Pour l'instant on n'a pas les HP des mobs, fallback sur closest
                validTargets.sort(Comparator.comparingDouble(e -> e.distance));
            }
            case "highest_value" -> {
                // Mini-boss (largeur >= 0.8) en priorité, puis par distance
                validTargets.sort((a, b) -> {
                    boolean aIsBoss = a.entity.getWidth() >= 0.8f;
                    boolean bIsBoss = b.entity.getWidth() >= 0.8f;
                    if (aIsBoss && !bIsBoss) return -1;
                    if (!aIsBoss && bIsBoss) return 1;
                    return Double.compare(a.distance, b.distance);
                });
            }
            default -> validTargets.sort(Comparator.comparingDouble(e -> e.distance));
        }
    }

    /**
     * Notifie que la cible actuelle est morte.
     */
    public void onTargetKilled() {
        currentTarget = null;
        retargetTimer = config_retargetInterval(); // force re-eval immédiat
    }

    /**
     * Notifie que la cible est invalide (removed, too far, etc.)
     */
    public void invalidateTarget() {
        currentTarget = null;
    }

    /**
     * Nombre de mobs valides dans le dernier scan.
     */
    public int getMobCount() {
        return cachedMobCount;
    }

    /**
     * La cible actuelle.
     */
    public Entity getCurrentTarget() {
        return currentTarget;
    }

    /**
     * Reset complet (changement d'île, stop bot, etc.)
     */
    public void reset() {
        currentTarget = null;
        retargetTimer = 0;
        validTargets.clear();
        cachedMobCount = 0;
    }

    private int config_retargetInterval() {
        return 999; // will force reevaluation next tick
    }

    // ═══════════ INNER CLASS ═══════════

    private static class ScoredEntity {
        final Entity entity;
        final double distance;

        ScoredEntity(Entity entity, double distance) {
            this.entity = entity;
            this.distance = distance;
        }
    }
}
