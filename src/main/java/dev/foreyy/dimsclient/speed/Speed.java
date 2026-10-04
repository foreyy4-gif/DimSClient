package dev.foreyy.dimsclient.speed;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Multiplies walking speed. The modifier is removed the moment the module turns off. */
public final class Speed {
	private static final Identifier MODIFIER = Identifier.fromNamespaceAndPath("dimsclient", "speed");

	private Speed() {
	}

	public static void tick(LocalPlayer player) {
		var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attribute == null) {
			return;
		}
		attribute.removeModifier(MODIFIER);
		double multiplier = DimsClient.CONFIG.speedEnabled ? DimsClient.CONFIG.speedMultiplier : 1.0;
		if (dev.foreyy.dimsclient.chase.Chase.chasing(net.minecraft.client.Minecraft.getInstance())) {
			double chase = DimsClient.CONFIG.chaseLegit ? DimsClient.CONFIG.chaseLegitSpeed : DimsClient.CONFIG.chaseSpeed;
			multiplier = Math.max(multiplier, chase);
		}
		if (multiplier <= 1.0) {
			return;
		}
		double extra = multiplier - 1.0;
		attribute.addTransientModifier(new AttributeModifier(MODIFIER, extra, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}
}
