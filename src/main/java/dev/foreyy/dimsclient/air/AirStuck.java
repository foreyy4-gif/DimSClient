package dev.foreyy.dimsclient.air;

import dev.foreyy.dimsclient.DimsClient;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Freezes the player where they are. The server is told the same spot, one short step
 * lower every tick, so its fall distance grows and a mace smash connects. Anything that
 * tries to move the player while they hang, a dash included, is kept and handed back in
 * one go when they drop. Up climbs by the configured height, a few blocks a tick, which
 * is as far as the server accepts.
 */
public final class AirStuck {
	private static final double STEP = 0.05;
	/**
	 * One block at a time. The server rejects a move that touches any block the player was not
	 * already inside, so a taller step stops dead on a thick wall while a single block never
	 * reaches the far side of one.
	 */
	private static final double CLIMB = 1.0;
	/** Pressed against a ceiling the move check already allows, so the climb passes through it. */
	private static final double CEILING = 1.8 - 1.0E-4;
	private static boolean held;
	private static double anchorX;
	private static double anchorY;
	private static double anchorZ;
	private static double reportY;
	private static Vec3 stored = Vec3.ZERO;
	private static final Queue<Double> CLIMBS = new ArrayDeque<>();

	private AirStuck() {
	}

	public static void tick(LocalPlayer self) {
		while (DimsClient.AIR_HOLD.consumeClick()) {
			held = !held;
			if (held) {
				begin(self);
			}
		}
		while (DimsClient.AIR_UP.consumeClick()) {
			if (!held) {
				begin(self);
				held = true;
			}
			double floor = Math.floor(anchorY);
			if (anchorY - floor < CEILING) {
				CLIMBS.add(floor + CEILING - anchorY);
			}
			double left = DimsClient.CONFIG.airHeight;
			while (left > 0.0) {
				double step = Math.min(CLIMB, left);
				CLIMBS.add(step);
				left -= step;
			}
		}
		boolean drop = false;
		while (DimsClient.AIR_DROP.consumeClick()) {
			drop = held;
			held = false;
		}
		if (!held || !DimsClient.CONFIG.airEnabled) {
			held = false;
			CLIMBS.clear();
			if (drop) {
				release(self);
			}
			return;
		}
		stored = stored.add(self.getDeltaMovement());
		self.setDeltaMovement(Vec3.ZERO);
		self.fallDistance = 0.0;
		self.setPos(anchorX, anchorY, anchorZ);
	}

	private static void begin(LocalPlayer self) {
		anchorX = self.getX();
		anchorY = self.getY();
		anchorZ = self.getZ();
		reportY = anchorY;
		stored = Vec3.ZERO;
		CLIMBS.clear();
	}

	/** One step down, or the next piece of a climb. Called once a tick. */
	public static ServerboundMovePlayerPacket packet(LocalPlayer self) {
		Double climb = CLIMBS.poll();
		if (climb != null) {
			anchorY += climb;
			reportY = anchorY;
		} else {
			reportY -= STEP;
		}
		return new ServerboundMovePlayerPacket.PosRot(
			anchorX, reportY, anchorZ, self.getYRot(), self.getXRot(), false, false);
	}

	/** Everything stored while hanging, applied the moment the drop key is pressed. */
	private static void release(LocalPlayer self) {
		self.setPos(anchorX, anchorY, anchorZ);
		self.setDeltaMovement(stored);
		self.hurtMarked = true;
		stored = Vec3.ZERO;
	}

	public static boolean holding() {
		return held && DimsClient.CONFIG.airEnabled;
	}

	public static double reportedY() {
		return reportY;
	}
}

