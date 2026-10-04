package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops the darkness effect from pulling fog or brightness down. The effect stays. */
@Mixin(DarknessFogEnvironment.class)
public class NoDarknessMixin {
	@Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
	private void dims$noDarkness(FogData data, net.minecraft.client.Camera camera, net.minecraft.client.multiplayer.ClientLevel level,
			float renderDistance, net.minecraft.client.DeltaTracker delta, CallbackInfo ci) {
		if (!DimsClient.CONFIG.noDarknessEnabled) {
			return;
		}
		data.environmentalStart = renderDistance * 0.8F;
		data.environmentalEnd = renderDistance;
		data.skyEnd = renderDistance;
		data.cloudEnd = renderDistance;
		ci.cancel();
	}

	@Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
	private void dims$keepLight(net.minecraft.world.entity.LivingEntity entity, float darkness, float partialTick,
			CallbackInfoReturnable<Float> ci) {
		if (DimsClient.CONFIG.noDarknessEnabled) {
			ci.setReturnValue(darkness);
		}
	}

	@Mixin(net.minecraft.client.renderer.LightmapRenderStateExtractor.class)
	private static class Brightness {
		@Inject(method = "extract", at = @At("TAIL"))
		private void dims$noPulse(net.minecraft.client.renderer.state.LightmapRenderState state, float partialTicks, CallbackInfo ci) {
			if (!DimsClient.CONFIG.noDarknessEnabled) {
				return;
			}
			net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
			if (client.player == null || !client.player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)) {
				return;
			}
			state.darknessEffectScale = 0.0F;
			state.brightness = client.options.gamma().get().floatValue();
		}
	}
}
