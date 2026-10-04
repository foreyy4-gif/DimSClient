package dev.foreyy.dimsclient.stun;

import dev.foreyy.dimsclient.DimsClient;

import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Breaks a shielding player's guard with an axe. 26.2 only registers the hit when the look ray
 * enters the target's hitbox inside the weapon's own reach window, so the aim is solved against
 * that window and shipped in the same tick as the swing.
 */
public final class StunSlam {
	private static final float HITBOX_MARGIN = 0.3F;
	/** Matches the buffer ServerGamePacketListenerImpl.handleAttack allows past the weapon reach. */
	private static final double SERVER_BUFFER = 3.0;

	private static int returnSlot = -1;
	private static int swingCooldown;
	private StunSlam() {
	}

	public static void reset() {
		returnSlot = -1;
		swingCooldown = 0;
	}

	public static void tick(Minecraft client) {
		LocalPlayer self = client.player;
		if (self == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
			returnSlot = -1;
			swingCooldown = 0;
			return;
		}

		if (swingCooldown > 0) {
			swingCooldown--;
		}

		if (returnSlot != -1) {
			selectSlot(client, returnSlot);
			returnSlot = -1;
			return;
		}

		int axeSlot = findAxeSlot(self);
		if (axeSlot == -1) {
			return;
		}

		ItemStack axe = self.getInventory().getItem(axeSlot);
		AttackRange reach = axe.get(DataComponents.ATTACK_RANGE);
		float minReach = reach != null ? reach.minReach() : 0.0F;
		float maxReach = reach != null ? reach.maxReach() : 3.0F;
		Vec3 eye = self.getEyePosition();
		Vec3 look = self.getLookAngle();
		Player target = nearestBlockingPlayer(self, eye, look);
		if (target == null) {
			return;
		}

		AABB real = target.getBoundingBox();
		AABB hitbox = real.inflate(HITBOX_MARGIN + (DimsClient.CONFIG.hitboxEnabled ? DimsClient.CONFIG.hitboxExpand : 0.0F));
		double along = rayDistanceToBox(eye, look, hitbox);
		if (Double.isNaN(along)) {
			return;
		}

		// Server measures eye-to-box, which is shorter than where the ray first touches it.
		double boxDistance = Math.sqrt(hitbox.distanceToSqr(eye));
		if (boxDistance < minReach - SERVER_BUFFER || boxDistance > maxReach + SERVER_BUFFER) {
			return;
		}

		// Eye-to-player distance: the setting is how far the axe reaches.
		double playerDistance = Math.sqrt(target.distanceToSqr(eye));
		if (playerDistance > DimsClient.CONFIG.stunRange) {
			return;
		}

		if (swingCooldown > 0) {
			return;
		}

		// 26.2 only counts the hit when the look ray enters the hitbox between the weapon's min and
		// max reach. Aiming at the closest point on the box lands short of that window, so the server drops it.
		// The grown box only decides the crosshair is close enough. The point itself has to sit
		// inside the real box, or the server drops the hit.
		Vec3 point = aimPoint(eye, look, real, minReach, maxReach, rayDistanceToBox(eye, look, real));
		if (point == null) {
			return;
		}
		float[] aim = aimAt(eye, point);
		self.connection.send(new ServerboundMovePlayerPacket.Rot(aim[0], aim[1], self.onGround(), self.horizontalCollision));
		self.setYRot(aim[0]);
		self.setXRot(aim[1]);

		int previous = self.getInventory().getSelectedSlot();
		if (previous != axeSlot) {
			// Only the local slot changes here. gameMode.attack notices the mismatch and sends the
			// carried-item packet itself; sending it ahead of that desyncs the slot and the server
			// then rejects ordinary swings.
			self.getInventory().setSelectedSlot(axeSlot);
			returnSlot = previous;
		}

		client.gameMode.attack(self, target);
		self.swing(InteractionHand.MAIN_HAND);
		swingCooldown = DimsClient.CONFIG.stunIntervalTicks;
	}

	/** Hotbar slot of an axe, preferring the one already held. */
	private static int findAxeSlot(LocalPlayer self) {
		int selected = self.getInventory().getSelectedSlot();
		if (isAxe(self.getInventory().getItem(selected))) {
			return selected;
		}
		for (int slot = 0; slot < 9; slot++) {
			if (isAxe(self.getInventory().getItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	private static boolean isAxe(ItemStack stack) {
		return stack.is(ItemTags.AXES);
	}

	/** Closest other player who is blocking and whose grown hitbox the crosshair ray enters. */
	private static Player nearestBlockingPlayer(LocalPlayer self, Vec3 eye, Vec3 look) {
		float grow = HITBOX_MARGIN + (DimsClient.CONFIG.hitboxEnabled ? DimsClient.CONFIG.hitboxExpand : 0.0F);
		return self.level()
			.players()
			.stream()
			.filter(player -> player != self && player.isAlive() && player.isBlocking())
			.filter(player -> !Double.isNaN(rayDistanceToBox(eye, look, player.getBoundingBox().inflate(grow))))
			.min(Comparator.comparingDouble(player -> rayDistanceToBox(eye, look, player.getBoundingBox().inflate(grow))))
			.orElse(null);
	}

	/**
	 * A point the look ray crosses inside the weapon's reach window. Prefers where the current view
	 * already enters the box, and otherwise the spot on the box face nearest the crosshair.
	 */
	private static Vec3 aimPoint(Vec3 eye, Vec3 look, AABB box, float minReach, float maxReach, double entry) {
		double exit = rayExit(eye, look, box);
		if (!Double.isNaN(exit)) {
			double lo = Math.max(entry, minReach);
			double hi = Math.min(exit, maxReach);
			if (lo <= hi) {
				return eye.add(look.scale((lo + hi) / 2.0));
			}
		}
		double mid = (minReach + maxReach) / 2.0;
		if (Double.isNaN(entry)) {
			entry = mid;
		}
		Vec3 face = closestPoint(box, eye.add(look.scale(Mth.clamp(mid, entry, entry + box.getSize()))));
		Vec3 delta = face.subtract(eye);
		double distance = delta.length();
		if (distance < 1.0E-4 || distance < minReach || distance > maxReach) {
			return null;
		}
		return face;
	}

	private static double rayExit(Vec3 origin, Vec3 direction, AABB box) {
		double tMin = 0.0;
		double tMax = Double.MAX_VALUE;
		double[] x = slab(tMin, tMax, origin.x, direction.x, box.minX, box.maxX);
		if (x[0] > x[1]) {
			return Double.NaN;
		}
		double[] y = slab(x[0], x[1], origin.y, direction.y, box.minY, box.maxY);
		if (y[0] > y[1]) {
			return Double.NaN;
		}
		double[] z = slab(y[0], y[1], origin.z, direction.z, box.minZ, box.maxZ);
		return z[0] > z[1] ? Double.NaN : z[1];
	}

	private static float[] aimAt(Vec3 eye, Vec3 point) {
		Vec3 delta = point.subtract(eye);
		double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
		float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
		float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * (180.0 / Math.PI)));
		return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F)};
	}

	/** Distance along the ray to the first intersection with the box, or NaN when it never hits. */
	private static double rayDistanceToBox(Vec3 origin, Vec3 direction, AABB box) {
		double tMin = 0.0;
		double tMax = Double.MAX_VALUE;
		double[] x = slab(tMin, tMax, origin.x, direction.x, box.minX, box.maxX);
		if (x[0] > x[1]) {
			return Double.NaN;
		}
		double[] y = slab(x[0], x[1], origin.y, direction.y, box.minY, box.maxY);
		if (y[0] > y[1]) {
			return Double.NaN;
		}
		double[] z = slab(y[0], y[1], origin.z, direction.z, box.minZ, box.maxZ);
		if (z[0] > z[1] || z[1] < 0.0) {
			return Double.NaN;
		}
		return Math.max(z[0], 0.0);
	}

	private static double[] slab(double tMin, double tMax, double origin, double direction, double min, double max) {
		if (Math.abs(direction) < 1.0E-8) {
			return origin < min || origin > max ? new double[]{1.0, 0.0} : new double[]{tMin, tMax};
		}
		double a = (min - origin) / direction;
		double b = (max - origin) / direction;
		if (a > b) {
			double swap = a;
			a = b;
			b = swap;
		}
		return new double[]{Math.max(tMin, a), Math.min(tMax, b)};
	}

	private static Vec3 closestPoint(AABB box, Vec3 point) {
		return new Vec3(Mth.clamp(point.x, box.minX, box.maxX), Mth.clamp(point.y, box.minY, box.maxY), Mth.clamp(point.z, box.minZ, box.maxZ));
	}

	private static void selectSlot(Minecraft client, int slot) {
		client.player.getInventory().setSelectedSlot(slot);
	}
}
