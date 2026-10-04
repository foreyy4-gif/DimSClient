package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.chase.Chase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Movement input is read at the start of the player step, so the chase has to be in by then. */
@Mixin(LocalPlayer.class)
public abstract class ChaseMixin {
	@Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
	private void dims$chase(CallbackInfo callback) {
		Chase.tick(Minecraft.getInstance());
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void dims$look(CallbackInfo callback) {
		Chase.look(Minecraft.getInstance());
	}
}
