package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The server only counts a fall when the move packet says the player is in the air. */
@Mixin(LocalPlayer.class)
public class NoFallMixin {
	@ModifyArg(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"), index = 0)
	private net.minecraft.network.protocol.Packet<?> dims$ground(net.minecraft.network.protocol.Packet<?> packet) {
		if (!DimsClient.CONFIG.noFallEnabled || !(packet instanceof ServerboundMovePlayerPacket move) || move.isOnGround()) {
			return packet;
		}
		// AirStuck's packet has to stay airborne, or the server lands them at the spoofed height.
		if (dev.foreyy.dimsclient.air.AirStuck.holding() || dev.foreyy.dimsclient.teleport.Teleport.moving()) {
			return packet;
		}
		// A gliding player the server thinks is on the ground gets their flight stopped at once.
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (self.isFallFlying()) {
			return packet;
		}
		boolean collision = move.horizontalCollision();
		double y = self.getY();
		if (move instanceof ServerboundMovePlayerPacket.PosRot pos) {
			return new ServerboundMovePlayerPacket.PosRot(pos.getX(0), y, pos.getZ(0), pos.getYRot(0), pos.getXRot(0), true, collision);
		}
		if (move instanceof ServerboundMovePlayerPacket.Pos pos) {
			return new ServerboundMovePlayerPacket.Pos(pos.getX(0), y, pos.getZ(0), true, collision);
		}
		if (move instanceof ServerboundMovePlayerPacket.Rot rot) {
			return new ServerboundMovePlayerPacket.Rot(rot.getYRot(0), rot.getXRot(0), true, collision);
		}
		return new ServerboundMovePlayerPacket.StatusOnly(true, collision);
	}
}
