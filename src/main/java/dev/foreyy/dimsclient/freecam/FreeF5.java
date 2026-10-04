package dev.foreyy.dimsclient.freecam;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Spins the third-person camera without turning the player. */
public final class FreeF5 {
	private static float yaw;

	private FreeF5() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.freeF5Enabled || client.options.getCameraType().isFirstPerson()) {
			yaw = 0.0F;
		}
	}

	public static boolean active(Minecraft client) {
		return DimsClient.CONFIG.freeF5Enabled && !client.options.getCameraType().isFirstPerson();
	}

	public static void turn(double xo, double yo) {
		yaw += (float) xo * 0.15F;
		yaw = Mth.wrapDegrees(yaw);
	}

	public static float yaw() {
		return yaw;
	}
}
