package dev.foreyy.dimsclient.dash;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/** Lunge III: 0.458 per level, straight along the look, with no spear and no hit. */
public final class SpearDash {
	private static final int LUNGE_LEVEL = 3;
	private static final float LUNGE_PER_LEVEL = 0.458F;

	private static int cooldown;
	private static int glide;
	/** Ticks left in the lunge, so NoFall only covers the spear dash and not a normal jump. */
	private static int lunge;

	private SpearDash() {
	}

	public static void tick(Minecraft client) {
		if (cooldown > 0) {
			cooldown--;
		}
		if (glide > 0) {
			glide--;
		}
		if (lunge > 0) {
			lunge--;
		}
		while (DimsClient.DASH.consumeClick()) {
			if (cooldown == 0) {
				push(client);
			}
		}
	}

	private static void push(Minecraft client) {
		LocalPlayer self = client.player;
		if (self == null || self.isPassenger() || self.isInWater()) {
			return;
		}
		Vec3 impulse = self.getLookAngle().scale(LUNGE_PER_LEVEL * LUNGE_LEVEL);
		self.addDeltaMovement(impulse);
		self.hurtMarked = true;
		// A sprinting lunge is billed as a sprint, which is what drains hunger.
		self.setSprinting(false);
		self.applyPostImpulseGraceTime(10);
		self.playSound(SoundEvents.LUNGE_3.value(), 1.0F, 1.0F);
		cooldown = DimsClient.CONFIG.dashCooldownTicks;
		lunge = 30;
		if (self.isFallFlying()) {
			glide = 6;
		}
	}

	/** True for a few ticks after a dash while gliding, so the glide does not eat the lunge. */
	public static boolean gliding() {
		return glide > 0;
	}

	/** True while the last spear dash is still carrying the player. */
	public static boolean lunging() {
		return lunge > 0;
	}
}
