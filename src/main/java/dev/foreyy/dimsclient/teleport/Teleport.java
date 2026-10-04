package dev.foreyy.dimsclient.teleport;

import dev.foreyy.dimsclient.DimsClient;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Walks the player to typed coordinates, one block a tick. Anything faster gets
 * bounced back by the server, and a taller step stops dead inside a wall.
 */
public final class Teleport {
	private static final double CLIMB = 1.0;
	/** Flat steps can be long. A tall one gets cut down to headroom by the server. */
	private static final double RUN = 8.0;
	private static final double CEILING = 1.8 - 1.0E-4;

	private static boolean moving;
	/** Ticks left in the window a mace smash opens, where the server accepts any move. */
	private static int grace;
	private static double anchorX;
	private static double anchorY;
	private static double anchorZ;
	private static final Queue<Vec3> STEPS = new ArrayDeque<>();

	private Teleport() {
	}

	public static void tick(LocalPlayer self) {
		if (grace > 0) {
			grace--;
			if (pending != null && grace < 38) {
				var point = pending;
				pending = null;
				to(self, point);
			}
		}
		while (DimsClient.TELEPORT.consumeClick()) {
			go(self);
		}
		if (!moving || !DimsClient.CONFIG.teleportEnabled) {
			moving = false;
			STEPS.clear();
			return;
		}
		self.setDeltaMovement(Vec3.ZERO);
		self.fallDistance = 0.0;
		self.setPos(anchorX, anchorY, anchorZ);
	}

	/** Walks to a point the same way a typed trip does, one block a tick. */
	public static void to(LocalPlayer self, net.minecraft.world.phys.Vec3 target) {
		if (DimsClient.CONFIG.teleportBlink && grace > 0) {
			blink(self, target);
			return;
		}
		if (DimsClient.CONFIG.teleportBlink && smash(self)) {
			pending = target;
			return;
		}
		moving = true;
		anchorX = self.getX();
		anchorY = self.getY();
		anchorZ = self.getZ();
		STEPS.clear();
		fill(target.x, target.y, target.z);
	}

	/**
	 * One packet straight there. The server only allows a jump this big while gliding, so the glide is
	 * switched on for the packet and off again at once.
	 */
	/** One packet straight there. Only arrives while the smash window is open. */
	private static void blink(LocalPlayer self, net.minecraft.world.phys.Vec3 target) {
		self.connection.send(new ServerboundMovePlayerPacket.PosRot(target.x, target.y, target.z, self.getYRot(), self.getXRot(), false, false));
		self.setPos(target.x, target.y, target.z);
		self.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
	}

	/**
	 * A mace hit while falling opens the window on the server for forty ticks. The teleport has to wait
	 * out that swing, so the point is kept and the jump is taken once the window is open.
	 */
	private static net.minecraft.world.phys.Vec3 pending;

	private static boolean smash(LocalPlayer self) {
		if (self.fallDistance <= 1.5F || !self.getMainHandItem().is(net.minecraft.world.item.Items.MACE)) {
			return false;
		}
		var client = net.minecraft.client.Minecraft.getInstance();
		net.minecraft.world.entity.LivingEntity nearest = null;
		double best = 9.0;
		for (var entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof net.minecraft.world.entity.LivingEntity living) || living == self || !living.isAlive()) {
				continue;
			}
			double distance = living.distanceToSqr(self);
			if (distance < best) {
				best = distance;
				nearest = living;
			}
		}
		if (nearest == null) {
			return false;
		}
		net.minecraft.client.Minecraft.getInstance().gameMode.attack(self, nearest);
		grace = 40;
		return true;
	}

	/** Starts a new trip. An empty or broken field does nothing. */
	public static void go(LocalPlayer self) {
		double[] target = parse(DimsClient.CONFIG.teleportCoords);
		if (target == null) {
			return;
		}
		moving = true;
		anchorX = self.getX();
		anchorY = self.getY();
		anchorZ = self.getZ();
		STEPS.clear();
		fill(target[0] + 0.5, target[1], target[2] + 0.5);
	}

	private static void fill(double x, double y, double z) {
		double floor = Math.floor(anchorY);
		if (anchorY - floor < CEILING) {
			STEPS.add(new Vec3(0.0, floor + CEILING - anchorY, 0.0));
		}
		double leftX = x - anchorX;
		double leftY = y - (STEPS.isEmpty() ? anchorY : Math.floor(anchorY) + CEILING);
		double leftZ = z - anchorZ;
		while (leftY > 1.0E-4) {
			double sy = Math.min(CLIMB, leftY);
			STEPS.add(new Vec3(0.0, sy, 0.0));
			leftY -= sy;
		}
		while (Math.abs(leftX) > 1.0E-4 || Math.abs(leftZ) > 1.0E-4) {
			double sx = Math.copySign(Math.min(RUN, Math.abs(leftX)), leftX);
			double sz = Math.copySign(Math.min(RUN, Math.abs(leftZ)), leftZ);
			STEPS.add(new Vec3(sx, 0.0, sz));
			leftX -= sx;
			leftZ -= sz;
		}
		while (leftY < -1.0E-4) {
			double sy = Math.max(-RUN, leftY);
			STEPS.add(new Vec3(0.0, sy, 0.0));
			leftY -= sy;
		}
	}

	/** One block toward the target. Called once a tick, from the only packet the server gets. */
	public static ServerboundMovePlayerPacket packet(LocalPlayer self) {
		Vec3 step = STEPS.poll();
		if (step == null) {
			moving = false;
		} else {
			anchorX += step.x;
			anchorY += step.y;
			anchorZ += step.z;
		}
		return new ServerboundMovePlayerPacket.PosRot(
			anchorX, anchorY, anchorZ, self.getYRot(), self.getXRot(), false, false);
	}

	public static boolean moving() {
		return moving && DimsClient.CONFIG.teleportEnabled;
	}

	/** Three numbers, commas or spaces. Null when the field is not that. */
	public static double[] parse(String text) {
		String[] parts = text.trim().split("[,\\s]+");
		if (parts.length != 3) {
			return null;
		}
		try {
			return new double[] {Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2])};
		} catch (NumberFormatException bad) {
			return null;
		}
	}
}
