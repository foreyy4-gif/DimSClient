package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.dash.SpearDash;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The glide rewrite drops a dash. For a few ticks the lunge is what the glide starts from. */
@Mixin(LivingEntity.class)
public class ElytraDashMixin {
	@Inject(method = "updateFallFlyingMovement", at = @At("HEAD"), cancellable = true)
	private void dims$keepDash(Vec3 movement, CallbackInfoReturnable<Vec3> callback) {
		if (SpearDash.gliding()) {
			callback.setReturnValue(movement);
		}
	}
}
