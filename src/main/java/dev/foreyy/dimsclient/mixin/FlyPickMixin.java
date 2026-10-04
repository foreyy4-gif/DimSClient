package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While flying the camera sits above the body, so the crosshair would aim from up there and
 * every swing would miss the block. Aiming from the body breaks blocks the way standing does.
 */
@Mixin(Minecraft.class)
public class FlyPickMixin {
	@Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"))
	private HitResult dims$aimFromBody(net.minecraft.client.player.LocalPlayer player, float partialTicks, Entity camera) {
		Entity from = camera;
		if (DimsClient.CONFIG.flyEnabled && DimsClient.CONFIG.flyOwnCrosshair && player.getAbilities().flying) {
			from = player;
		}
		return player.raycastHitResult(partialTicks, from);
	}
}
