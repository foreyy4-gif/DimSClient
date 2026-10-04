package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops a health update that is not a heal, and the red flash that comes with a hit. */
@Mixin(ClientPacketListener.class)
public class GodMixin {
	@Inject(method = "handleSetHealth", at = @At("HEAD"), cancellable = true)
	private void dims$noDamage(ClientboundSetHealthPacket packet, CallbackInfo callback) {
		if (!DimsClient.CONFIG.godEnabled) {
			return;
		}
		var player = Minecraft.getInstance().player;
		if (player != null && packet.getHealth() <= player.getHealth()) {
			callback.cancel();
		}
	}

	@Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
	private void dims$noHurt(ClientboundDamageEventPacket packet, CallbackInfo callback) {
		if (!DimsClient.CONFIG.godEnabled) {
			return;
		}
		var player = Minecraft.getInstance().player;
		var level = Minecraft.getInstance().level;
		if (player != null && level != null && level.getEntity(packet.entityId()) == player) {
			callback.cancel();
		}
	}
}
