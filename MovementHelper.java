package com.minepiecefarmer.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * Gestion du déplacement et de l'orientation du joueur.
 * Inclut smooth look, mouvement directionnel, et détection d'eau.
 */
public class MovementHelper {

    private static final Random random = new Random();

    // ══════════════════════════════════════════════
    //  LOOK AT
    // ══════════════════════════════════════════════

    /**
     * Regarde une position de manière smooth avec wobble naturel.
     */
    public static void lookAtSmooth(ClientPlayerEntity player, Vec3d target, float speed, float wobble) {
        Vec3d eyes = player.getEyePos();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;

        double dist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, dist));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        float pitchDiff = targetPitch - player.getPitch();

        // Vitesse variable (plus naturel)
        float actualSpeed = speed + random.nextFloat() * 0.08f;

        // Accélération quand on est loin de la cible
        float absDiff = Math.abs(yawDiff);
        if (absDiff > 30) {
            actualSpeed *= 1.5f;
        } else if (absDiff > 60) {
            actualSpeed *= 2.0f;
        }

        // Micro-tremblements humains
        float wobbleX = (random.nextFloat() - 0.5f) * wobble;
        float wobbleY = (random.nextFloat() - 0.5f) * wobble * 0.6f;

        player.setYaw(player.getYaw() + yawDiff * actualSpeed + wobbleX);
        player.setPitch(MathHelper.clamp(
                player.getPitch() + pitchDiff * actualSpeed + wobbleY,
                -89.0f, 89.0f));
    }

    /**
     * Regarde une entité (centre du corps).
     */
    public static void lookAtEntity(ClientPlayerEntity player, Entity target, float speed, float wobble) {
        Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.5, 0);
        lookAtSmooth(player, targetPos, speed, wobble);
    }

    /**
     * Vérifie si le joueur regarde approximativement vers une entité.
     */
    public static boolean isLookingAt(ClientPlayerEntity player, Entity target, float tolerance) {
        Vec3d eyes = player.getEyePos();
        Vec3d targetCenter = target.getPos().add(0, target.getHeight() * 0.5, 0);

        double dx = targetCenter.x - eyes.x;
        double dz = targetCenter.z - eyes.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - player.getYaw()));

        return yawDiff < tolerance;
    }

    // ══════════════════════════════════════════════
    //  MOUVEMENT
    // ══════════════════════════════════════════════

    public static void setMovement(MinecraftClient client, boolean forward, boolean back, boolean left, boolean right) {
        client.options.forwardKey.setPressed(forward);
        client.options.backKey.setPressed(back);
        client.options.leftKey.setPressed(left);
        client.options.rightKey.setPressed(right);
    }

    public static void stopMovement(MinecraftClient client) {
        setMovement(client, false, false, false, false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
    }

    /**
     * Avance vers une position en marchant (sans Baritone).
     * Gère le saut automatique si bloqué.
     */
    public static void walkToward(MinecraftClient client, ClientPlayerEntity player,
                                   Vec3d target, float lookSpeed, float wobble) {
        lookAtSmooth(player, target, lookSpeed, wobble);
        setMovement(client, true, false, false, false);
    }

    // ══════════════════════════════════════════════
    //  EAU
    // ══════════════════════════════════════════════

    public static boolean isWater(MinecraftClient client, BlockPos pos) {
        if (client.world == null) return false;
        BlockState state = client.world.getBlockState(pos);
        return state.getFluidState().isIn(FluidTags.WATER) || state.isOf(Blocks.WATER);
    }

    public static boolean isWaterAhead(MinecraftClient client, ClientPlayerEntity player) {
        if (client.world == null) return false;

        float yaw = player.getYaw();
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);

        Vec3d pos = player.getPos();

        for (int i = 1; i <= 2; i++) {
            double checkX = pos.x + dx * i;
            double checkZ = pos.z + dz * i;
            BlockPos feetPos = new BlockPos(
                    (int) Math.floor(checkX),
                    (int) Math.floor(pos.y),
                    (int) Math.floor(checkZ));

            if (isWater(client, feetPos) || isWater(client, feetPos.down())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInWater(ClientPlayerEntity player) {
        return player.isTouchingWater();
    }

    // ══════════════════════════════════════════════
    //  UTILS
    // ══════════════════════════════════════════════

    public static double horizontalDistance(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double horizontalDistanceSq(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
