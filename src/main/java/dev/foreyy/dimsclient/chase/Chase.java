package dev.foreyy.dimsclient.chase;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Runs after a player and only swings while falling, which is what makes the hit a crit. */
public final class Chase {
	/** As far as the server still counts a hit, plus its own buffer. */
	private static final double REACH = 6.0;
	/** How far out the circle sits, and how far along it one tick may move. */
	private static final double ORBIT = 2.4;
	private static final double STEP = 0.85;
	/** Lift while circling, so the hit stays a crit instead of landing on the ground. */
	private static final double HOP = 0.42;
	/** How far the crosshair may turn in one frame. Fast enough to stay on a moving target, still eased. */
	private static final float AIM = 30.0F;

	private Chase() {
	}

	public static void tick(Minecraft client) {
		// The key press lands before the menu opens, so it must be read even while the menu is up.
		// Otherwise it sits there and flips the module back on the moment the menu closes.
		while (DimsClient.CHASE.consumeClick()) {
			DimsClient.CONFIG.chaseEnabled = !DimsClient.CONFIG.chaseEnabled;
			DimsClient.CONFIG.save();
		}
		LocalPlayer self = client.player;
		if (self == null || client.level == null || client.gameMode == null) {
			return;
		}
		if (!DimsClient.CONFIG.chaseEnabled) {
			return;
		}
		LivingEntity target = find(client);
		if (target == null) {
			return;
		}
		boolean aimed = aim(self, target);
		double flat = flatDistance(self, target);
		boolean close = flat <= DimsClient.CONFIG.chaseRange;
		// Legit follows on foot and never circles. Only the unleashed mode takes the circle.
		if (close && !DimsClient.CONFIG.chaseLegit) {
			orbit(self, target);
		} else {
			chase(self, target);
		}
		// A crit needs a fall and no sprint. Only swing on the way down.
		boolean falling = !self.onGround() && self.getDeltaMovement().y < 0.0 && self.fallDistance > 0.0F;
		if (close && falling && aimed && self.getAttackStrengthScale(0.0F) >= 0.9F && client.gui.screen() == null) {
			self.setSprinting(false);
			client.gameMode.attack(self, target);
			self.swing(InteractionHand.MAIN_HAND);
		}
	}

	/** Runs straight at the target. Jump stays the player's own key. */
	private static void chase(LocalPlayer self, LivingEntity target) {
		double dx = target.getX() - self.getX();
		double dz = target.getZ() - self.getZ();
		float yaw = self.getYRot() * ((float) Math.PI / 180.0F);
		double length = Math.max(Math.sqrt(dx * dx + dz * dz), 0.001);
		float forward = (float) ((-dx * Mth.sin(yaw) + dz * Mth.cos(yaw)) / length);
		float strafe = (float) ((dx * Mth.cos(yaw) + dz * Mth.sin(yaw)) / length);
		boolean go = forward > 0.15F;
		boolean back = forward < -0.15F;
		boolean left = strafe < -0.2F;
		boolean right = strafe > 0.2F;
		self.input.keyPresses = new Input(go, back, left, right, self.input.keyPresses.jump(), false, go);
		self.setSprinting(go);
	}

	/**
	 * Places the player on the next point of a circle around the target.
	 * Walls still stop a step that does not fit. While on the ground it hops,
	 * so the swing on the way down is a crit.
	 */
	private static void orbit(LocalPlayer self, LivingEntity target) {
		double dx = self.getX() - target.getX();
		double dz = self.getZ() - target.getZ();
		double angle = Math.atan2(dz, dx);
		double spin = STEP / ORBIT;
		double y = self.getY();
		double x = self.getX();
		double z = self.getZ();
		for (int attempt = 1; attempt <= 8; attempt++) {
			double next = angle + spin * attempt;
			double tx = target.getX() + Math.cos(next) * ORBIT;
			double tz = target.getZ() + Math.sin(next) * ORBIT;
			if (fits(self, tx, y, tz)) {
				x = tx;
				z = tz;
				break;
			}
		}
		double vy = self.getDeltaMovement().y;
		if (self.onGround()) {
			vy = HOP;
		}
		self.setPos(x, y, z);
		self.setDeltaMovement(0.0, vy, 0.0);
		self.hurtMarked = true;
		self.input.keyPresses = Input.EMPTY;
		self.setSprinting(false);
	}

	/** A step only counts when the whole body fits, feet included. */
	private static boolean fits(LocalPlayer self, double x, double y, double z) {
		var box = self.getBoundingBox().move(x - self.getX(), y - self.getY(), z - self.getZ());
		int x0 = Mth.floor(box.minX);
		int y0 = Mth.floor(box.minY);
		int z0 = Mth.floor(box.minZ);
		int x1 = Mth.floor(box.maxX);
		int y1 = Mth.floor(box.maxY);
		int z1 = Mth.floor(box.maxZ);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int bx = x0; bx <= x1; bx++) {
			for (int by = y0; by <= y1; by++) {
				for (int bz = z0; bz <= z1; bz++) {
					var state = self.level().getBlockState(pos.set(bx, by, bz));
					if (!state.getCollisionShape(self.level(), pos).isEmpty()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	public static void release(LocalPlayer self) {
		if (self == null) {
			return;
		}
		self.input.keyPresses = Input.EMPTY;
		self.setSprinting(false);
	}

	/** True while a target is being chased, so the speed module can push the approach. */
	public static boolean chasing(Minecraft client) {
		return DimsClient.CONFIG.chaseEnabled && client.gui.screen() == null && find(client) != null;
	}

	/** Turns onto the target after the mouse has been read, so the look is not overwritten. */
	public static void look(Minecraft client) {
		if (!DimsClient.CONFIG.chaseEnabled || client.player == null || client.level == null || client.gui.screen() != null) {
			return;
		}
		LivingEntity target = find(client);
		if (target != null) {
			aim(client.player, target);
		}
	}

	/** Snaps the crosshair onto the grown hitbox, which is what the swing actually reaches. */
	private static boolean aim(LocalPlayer self, LivingEntity target) {
		Vec3 eye = self.getEyePosition();
		AABB box = target.getBoundingBox();
		if (DimsClient.CONFIG.hitboxEnabled && DimsClient.CONFIG.hitboxExpand > 0.0F) {
			box = box.inflate(DimsClient.CONFIG.hitboxExpand);
		}
		Vec3 point = new Vec3(
			Mth.clamp(eye.x, box.minX, box.maxX),
			Mth.clamp(eye.y, box.minY, box.maxY),
			Mth.clamp(eye.z, box.minZ, box.maxZ));
		double dx = point.x - eye.x;
		double dy = point.y - eye.y;
		double dz = point.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		float pitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
		float nextYaw = stepAngle(self.getYRot(), yaw);
		float nextPitch = stepAngle(self.getXRot(), pitch);
		self.setYRot(nextYaw);
		self.setXRot(nextPitch);
		self.setYHeadRot(nextYaw);
		return Math.abs(Mth.wrapDegrees(yaw - nextYaw)) < 1.0F && Math.abs(pitch - nextPitch) < 1.0F;
	}

	/** Turns toward the target a little each tick instead of snapping onto it. */
	private static float stepAngle(float from, float to) {
		float delta = Mth.wrapDegrees(to - from);
		delta = Mth.clamp(delta, -AIM, AIM);
		return from + delta;
	}

	private static LivingEntity find(Minecraft client) {
		String name = DimsClient.CONFIG.chaseName;
		LivingEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living) || living == client.player || !living.isAlive() || !wanted(living, name)) {
				continue;
			}
			double distance = living.distanceToSqr(client.player);
			if (name.isEmpty() && distance > 64.0 * 64.0) {
				continue;
			}
			if (distance < best) {
				best = distance;
				nearest = living;
			}
		}
		return nearest;
	}

	/** Friends are never hit. Enemies are always hit. A name only picks which player, the toggles do the rest. */
	private static boolean wanted(LivingEntity entity, String name) {
		if (entity instanceof Player player) {
			String plain = player.getPlainTextName();
			if (listed(DimsClient.CONFIG.chaseFriends, plain)) {
				return false;
			}
			if (listed(DimsClient.CONFIG.chaseEnemies, plain)) {
				return name.isEmpty() || plain.equalsIgnoreCase(name);
			}
		}
		if (!name.isEmpty() && !(entity instanceof Player player && player.getPlainTextName().equalsIgnoreCase(name))) {
			return false;
		}
		if (entity instanceof Player player) {
			return armored(player) ? DimsClient.CONFIG.chaseArmored : DimsClient.CONFIG.chaseNaked;
		}
		if (entity instanceof WitherBoss || entity instanceof EnderDragon) {
			return DimsClient.CONFIG.chaseBosses;
		}
		return DimsClient.CONFIG.chaseMobs;
	}

	/** Comma or space separated names. Matching ignores case. */
	private static boolean listed(String list, String name) {
		for (String part : list.split("[,\\s]+")) {
			if (!part.isEmpty() && part.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	/** Any worn piece counts, a carved pumpkin included. */
	private static boolean armored(LivingEntity entity) {
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot.isArmor() && !entity.getItemBySlot(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static String lookedAtName(Minecraft client) {
		if (client.crosshairPickEntity instanceof Player player && player != client.player) {
			return player.getPlainTextName();
		}
		return "";
	}

	/** Eye to the nearest point of the target, which is what the server measures. */
	private static double flatDistance(LocalPlayer self, LivingEntity target) {
		return Math.sqrt(target.getBoundingBox().distanceToSqr(self.getEyePosition()));
	}
}
