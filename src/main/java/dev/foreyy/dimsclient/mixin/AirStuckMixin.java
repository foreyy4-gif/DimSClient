package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.air.AirStuck;
import dev.foreyy.dimsclient.teleport.Teleport;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While held, the only move the server sees is one short step down, every tick.
 * Vanilla skips the packet when nothing moved, so the fall would stall until a
 * look change flushed the whole drop at once.
 */
@Mixin(LocalPlayer.class)
public class AirStuckMixin {
	@Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
	private void dims$fallEveryTick(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (self.isPassenger()) {
			return;
		}
		if (AirStuck.holding()) {
			self.connection.send(AirStuck.packet(self));
			ci.cancel();
			return;
		}
		if (Teleport.moving()) {
			self.connection.send(Teleport.packet(self));
			ci.cancel();
			return;
		}
		if (dev.foreyy.dimsclient.chest.ChestReach.moving()) {
			self.connection.send(dev.foreyy.dimsclient.chest.ChestReach.packet(self));
			ci.cancel();
		}
	}

	/** Covers the passenger path, which sends its own rotation packet. */
	@ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"), index = 0)
	private Packet<?> dims$fall(Packet<?> packet) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (AirStuck.holding()) {
			return AirStuck.packet(self);
		}
		if (Teleport.moving()) {
			return Teleport.packet(self);
		}
		if (dev.foreyy.dimsclient.chest.ChestReach.moving()) {
			return dev.foreyy.dimsclient.chest.ChestReach.packet(self);
		}
		return packet;
	}
}

