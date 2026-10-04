package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Invisible players stay drawn. The effect itself is untouched. */
@Mixin(Entity.class)
public class SeeInvisibleMixin {
	@Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
	private void dims$showPlayers(Player player, CallbackInfoReturnable<Boolean> ci) {
		if (DimsClient.CONFIG.seeInvisibleEnabled && (Object) this instanceof Player) {
			ci.setReturnValue(false);
		}
	}
}
