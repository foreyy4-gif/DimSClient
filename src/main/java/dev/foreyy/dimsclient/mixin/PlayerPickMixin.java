package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Other players count as under the crosshair a bit past their real box. */
@Mixin(Entity.class)
public class PlayerPickMixin {
	@Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
	private void dimsclient$growPlayerHitbox(CallbackInfoReturnable<Float> callback) {
		if (((Object) this) instanceof LivingEntity && DimsClient.CONFIG.hitboxEnabled && DimsClient.CONFIG.hitboxExpand > 0.0F) {
			callback.setReturnValue(DimsClient.CONFIG.hitboxExpand);
		}
	}
}
