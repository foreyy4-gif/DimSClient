package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.freecam.FreeF5;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** FreeF5 keeps the player's look and only turns the third-person camera. */
@Mixin(Camera.class)
public class FreeF5Mixin {
	@ModifyArgs(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
	private void dims$orbit(Args args) {
		Minecraft client = Minecraft.getInstance();
		if (!FreeF5.active(client)) {
			return;
		}
		args.set(0, (float) args.get(0) + FreeF5.yaw());
	}
}
