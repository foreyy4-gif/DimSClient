package dev.foreyy.dimsclient.fly;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.player.LocalPlayer;

/** Creative flight. The server still decides whether hovering in the air is allowed. */
public final class VanillaFly {
	private VanillaFly() {
	}

	public static void tick(LocalPlayer player) {
		var abilities = player.getAbilities();
		if (!DimsClient.CONFIG.flyEnabled) {
			if (abilities.mayfly && !player.isSpectator() && !player.isCreative()) {
				abilities.mayfly = false;
				abilities.flying = false;
				player.onUpdateAbilities();
			}
			return;
		}
		if (!abilities.mayfly) {
			abilities.mayfly = true;
			player.onUpdateAbilities();
		}
		// Hovering while flagged as sprinting is counted as a sprint and costs hunger.
		if (abilities.flying) {
			player.setSprinting(false);
		}
	}
}
