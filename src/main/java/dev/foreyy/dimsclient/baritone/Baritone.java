package dev.foreyy.dimsclient.baritone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IElytraProcess;
import dev.foreyy.dimsclient.DimsClient;
import dev.foreyy.dimsclient.teleport.Teleport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Drives the Baritone mod: walks to typed coordinates, flies an elytra there,
 * and walks to the block AutoFarm is set to mine. Baritone is a separate mod,
 * so every call is guarded and does nothing when it is not installed.
 */
public final class Baritone {
	private static boolean missing;
	/** The block Baritone was last sent after, so a change in AutoFarm retargets it. */
	private static String sentBlock = "";

	private Baritone() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.baritoneEnabled || client.player == null) {
			return;
		}
		if (!DimsClient.CONFIG.mineEnabled) {
			sentBlock = "";
			return;
		}
		// Empty means any ore, which is what AutoFarm mines when no block is picked.
		String id = DimsClient.CONFIG.mineBlock.isEmpty() ? "ore" : DimsClient.CONFIG.mineBlock;
		if (id.equals(sentBlock)) {
			return;
		}
		sentBlock = id;
		mine(client, id);
	}

	/**
	 * Mines the named block and walks over whatever it drops. Baritone only waits a quarter of a
	 * second for a drop by default, which is gone before the item appears, so the wait is stretched.
	 */
	private static void mine(Minecraft client, String id) {
		IBaritone baritone = baritone(client);
		if (baritone == null) {
			return;
		}
		Settings settings = BaritoneAPI.getSettings();
		settings.mineScanDroppedItems.value = true;
		settings.mineDropLoiterDurationMSThanksLouca.value = 1500L;
		baritone.getMineProcess().mineByName(0, id);
	}

	/** Walks to the coordinates typed in the teleport field. */
	public static void walk(Minecraft client) {
		double[] target = Teleport.parse(DimsClient.CONFIG.teleportCoords);
		if (target == null) {
			return;
		}
		ICustomGoalProcess process = goal(client);
		if (process != null) {
			process.setGoalAndPath(new GoalBlock((int) Math.floor(target[0]), (int) Math.floor(target[1]), (int) Math.floor(target[2])));
		}
	}

	/** Flies an elytra to the same coordinates. */
	public static void fly(Minecraft client) {
		double[] target = Teleport.parse(DimsClient.CONFIG.teleportCoords);
		if (target == null) {
			return;
		}
		IBaritone baritone = baritone(client);
		if (baritone == null) {
			return;
		}
		IElytraProcess elytra = baritone.getElytraProcess();
		if (elytra.isLoaded()) {
			elytra.pathTo(BlockPos.containing(target[0], target[1], target[2]));
		}
	}

	/** Drops whatever path is running. */
	public static void stop(Minecraft client) {
		ICustomGoalProcess process = goal(client);
		if (process != null) {
			process.setGoal(null);
		}
		sentBlock = "";
	}

	private static ICustomGoalProcess goal(Minecraft client) {
		IBaritone baritone = baritone(client);
		return baritone == null ? null : baritone.getCustomGoalProcess();
	}

	private static IBaritone baritone(Minecraft client) {
		if (missing || client.player == null) {
			return null;
		}
		try {
			return BaritoneAPI.getProvider().getPrimaryBaritone();
		} catch (Throwable ignored) {
			// The mod is not installed, so there is nothing to drive.
			missing = true;
			return null;
		}
	}
}
