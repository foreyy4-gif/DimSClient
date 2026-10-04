package dev.foreyy.dimsclient.elytra;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Circles a player on elytra at firework speed and hits them, without spending a rocket. */
public final class ElytraTarget {
	private static final double REACH = 3.2;
	private static final double RING = 2.6;
	private static final float AIM = 22.0F;
	private static final double POWER = 1.5;

	private ElytraTarget() {
	}

	public static void tick(Minecraft client) {
		LocalPlayer self = client.player;
		if (self == null || client.level == null || client.gameMode == null || !self.isFallFlying()) {
			return;
		}
		Player target = find(client);
		if (target == null) {
			return;
		}
		Vec3 want = orbit(self, target);
		look(self, want);
		boost(self);
		if (self.distanceTo(target) <= REACH && self.getAttackStrengthScale(0.0F) >= 0.9F) {
			client.gameMode.attack(self, target);
			self.swing(InteractionHand.MAIN_HAND);
		}
	}

	/** A point on a ring around the target, one step ahead of where we are now. */
	private static Vec3 orbit(LocalPlayer self, Player target) {
		double dx = self.getX() - target.getX();
		double dz = self.getZ() - target.getZ();
		double flat = Math.sqrt(dx * dx + dz * dz);
		if (flat < 0.001) {
			dx = 1.0;
			dz = 0.0;
			flat = 1.0;
		}
		double spin = 0.55 / flat;
		double nx = dx * Math.cos(spin) - dz * Math.sin(spin);
		double nz = dx * Math.sin(spin) + dz * Math.cos(spin);
		double length = Math.sqrt(nx * nx + nz * nz);
		nx = nx / length * RING;
		nz = nz / length * RING;
		double height = target.getY() + target.getBbHeight() * 0.6 + Math.sin(self.tickCount * 0.15) * 0.6;
		return new Vec3(target.getX() + nx, height, target.getZ() + nz);
	}

	private static void look(LocalPlayer self, Vec3 point) {
		Vec3 eye = self.getEyePosition();
		double dx = point.x - eye.x;
		double dy = point.y - eye.y;
		double dz = point.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
		float nextYaw = step(self.getYRot(), yaw);
		float nextPitch = step(self.getXRot(), pitch);
		self.setYRot(nextYaw);
		self.setXRot(nextPitch);
		self.setYHeadRot(nextYaw);
	}

	/** Same shove a firework rocket gives while you are gliding. */
	private static void boost(LocalPlayer self) {
		Vec3 look = self.getLookAngle();
		Vec3 movement = self.getDeltaMovement();
		self.setDeltaMovement(
			look.x * 0.1 + (look.x * POWER - movement.x) * 0.5,
			look.y * 0.1 + (look.y * POWER - movement.y) * 0.5,
			look.z * 0.1 + (look.z * POWER - movement.z) * 0.5);
	}

	private static float step(float from, float to) {
		float delta = Mth.wrapDegrees(to - from);
		return from + Mth.clamp(delta, -AIM, AIM);
	}

	private static Player find(Minecraft client) {
		String name = DimsClient.CONFIG.chaseName;
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (Player player : client.level.players()) {
			if (player == client.player || !player.isAlive()) {
				continue;
			}
			if (!name.isEmpty() && !player.getPlainTextName().equalsIgnoreCase(name)) {
				continue;
			}
			double distance = player.distanceToSqr(client.player);
			if (name.isEmpty() && distance > 96.0 * 96.0) {
				continue;
			}
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		return nearest;
	}
}
