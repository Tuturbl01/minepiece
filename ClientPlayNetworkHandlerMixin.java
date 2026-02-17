package com.minepiecefarmer.mixin;

import com.minepiecefarmer.MinepieceFarmer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepte les packets réseau pour détecter:
 *   - Messages action bar (overlay=true) → récompenses de kills (明=XP, 实=Berries, 军=Kills)
 *   - Sons de combat → illusioner.hurt (hit), illusioner.death (kill)
 *   - Messages chat → haki status
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    /**
     * Intercepte les messages de jeu (chat + action bar).
     */
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        try {
            String text = packet.content().getString();
            boolean overlay = packet.overlay();

            if (overlay) {
                // Action bar → parse les récompenses
                MinepieceFarmer.onActionBarMessage(text);
            } else {
                // Chat → parse haki et autres
                MinepieceFarmer.onChatMessage(text);
            }
        } catch (Exception e) {
            MinepieceFarmer.LOGGER.debug("Error parsing game message: {}", e.getMessage());
        }
    }

    /**
     * Intercepte les sons joués par le serveur.
     * illusioner.hurt = mob touché
     * illusioner.death = mob tué
     */
    @Inject(method = "onPlaySound", at = @At("HEAD"))
    private void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        try {
            String soundId = packet.getSound().value().id().toString();

            if (soundId.contains("illusioner")) {
                if (soundId.contains("hurt")) {
                    MinepieceFarmer.onMobHurtSound(packet.getX(), packet.getY(), packet.getZ());
                } else if (soundId.contains("death")) {
                    MinepieceFarmer.onMobDeathSound(packet.getX(), packet.getY(), packet.getZ());
                }
            }
        } catch (Exception e) {
            MinepieceFarmer.LOGGER.debug("Error parsing sound: {}", e.getMessage());
        }
    }
}
