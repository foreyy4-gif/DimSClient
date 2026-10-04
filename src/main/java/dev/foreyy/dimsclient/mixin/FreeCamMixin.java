package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.freecam.FreeCam;
import net.minecraft.client.MouseHandler;
import dev.foreyy.dimsclient.chase.Chase;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the free camera flies, looking around turns it instead of the player. */
@Mixin(MouseHandler.class)
public class FreeCamMixin {
	@Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void dims$turnCamera(net.minecraft.client.player.LocalPlayer player, double xo, double yo) {
		if (FreeCam.flying()) {
			FreeCam.turn(xo, yo);
			return;
		}
		if (dev.foreyy.dimsclient.freecam.FreeF5.active(net.minecraft.client.Minecraft.getInstance())) {
			dev.foreyy.dimsclient.freecam.FreeF5.turn(xo, yo);
			return;
		}
		player.turn(xo, yo);
	}

	/** The mouse turns the player after the tick, so the aim has to be put back on last. */
	@Inject(method = "turnPlayer", at = @At("TAIL"))
	private void dims$aim(CallbackInfo callback) {
		Chase.look(Minecraft.getInstance());
	}
}
