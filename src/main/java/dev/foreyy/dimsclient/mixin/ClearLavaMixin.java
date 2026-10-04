package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the lava camera clear. The burn itself is untouched. */
@Mixin(LavaFogEnvironment.class)
public class ClearLavaMixin {
	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private void dims$clearLava(FogData data, net.minecraft.client.Camera camera, net.minecraft.client.multiplayer.ClientLevel level,
			float renderDistance, net.minecraft.client.DeltaTracker delta, CallbackInfo ci) {
		if (!DimsClient.CONFIG.clearLavaEnabled) {
			return;
		}
		data.environmentalStart = renderDistance * 0.8F;
		data.environmentalEnd = renderDistance;
		data.skyEnd = renderDistance;
		data.cloudEnd = renderDistance;
		ci.cancel();
	}
}
