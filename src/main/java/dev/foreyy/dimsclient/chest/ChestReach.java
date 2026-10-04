package dev.foreyy.dimsclient.chest;

import dev.foreyy.dimsclient.DimsClient;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Opens the chest under the crosshair through a wall. In free cam the body walks to the camera
 * one block a tick, opens the chest from there and walks back, which is as fast as the server allows.
 */
public final class ChestReach {
	private static final double STEP = 0.9;

	private enum Phase {
		IDLE, GO, BACK
	}

	private static Phase phase = Phase.IDLE;
	private static BlockPos chest;
	private static final Queue<Vec3> STEPS = new ArrayDeque<>();
	private static double anchorX;
	private static double anchorY;
	private static double anchorZ;

	private ChestReach() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.chestEnabled || client.player == null || client.level == null || client.gameMode == null) {
			phase = Phase.IDLE;
			return;
		}
		while (client.options.keyUse.consumeClick()) {
			if (phase == Phase.IDLE) {
				begin(client);
			}
		}
		if (phase != Phase.IDLE) {
			// The body follows the route. Letting it drift makes the server pull it back.
			client.player.setPos(anchorX, anchorY, anchorZ);
			client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
		}
		if (phase == Phase.IDLE || !STEPS.isEmpty()) {
			return;
		}
		if (phase == Phase.GO) {
			open(client);
			// Back home, so the body is not left standing at the camera.
			route(client.player, home);
			phase = Phase.BACK;
		} else {
			phase = Phase.IDLE;
			chest = null;
		}
	}

	private static Vec3 home;

	private static void begin(Minecraft client) {
		chest = aimed(client);
		if (chest == null) {
			return;
		}
		LocalPlayer player = client.player;
		home = player.position();
		Vec3 target = dev.foreyy.dimsclient.freecam.FreeCam.flying()
			? dev.foreyy.dimsclient.freecam.FreeCam.position().subtract(0.0, player.getEyeHeight(), 0.0)
			: null;
		// Close enough already, or not in free cam: just open it.
		if (target == null || target.distanceToSqr(home) < 16.0) {
			open(client);
			chest = null;
			return;
		}
		route(player, target);
		phase = Phase.GO;
	}

	/** Splits the trip into steps the server still accepts. */
	private static void route(LocalPlayer player, Vec3 target) {
		STEPS.clear();
		anchorX = player.getX();
		anchorY = player.getY();
		anchorZ = player.getZ();
		double leftX = target.x - anchorX;
		double leftY = target.y - anchorY;
		double leftZ = target.z - anchorZ;
		while (Math.abs(leftX) > 1.0E-4 || Math.abs(leftY) > 1.0E-4 || Math.abs(leftZ) > 1.0E-4) {
			double sx = Math.copySign(Math.min(STEP, Math.abs(leftX)), leftX);
			double sy = Math.copySign(Math.min(STEP, Math.abs(leftY)), leftY);
			double sz = Math.copySign(Math.min(STEP, Math.abs(leftZ)), leftZ);
			STEPS.add(new Vec3(sx, sy, sz));
			leftX -= sx;
			leftY -= sy;
			leftZ -= sz;
		}
	}

	public static boolean moving() {
		return phase != Phase.IDLE;
	}

	/** One step toward the camera or back home. Replaces the normal move packet while it lasts. */
	public static ServerboundMovePlayerPacket packet(LocalPlayer self) {
		Vec3 step = STEPS.poll();
		if (step != null) {
			anchorX += step.x;
			anchorY += step.y;
			anchorZ += step.z;
		}
		return new ServerboundMovePlayerPacket.PosRot(anchorX, anchorY, anchorZ, self.getYRot(), self.getXRot(), false, false);
	}

	private static void open(Minecraft client) {
		if (chest == null) {
			return;
		}
		Vec3 eye = client.player.getEyePosition();
		Vec3 at = Vec3.atCenterOf(chest);
		Vec3 point = at.add(eye.subtract(at).normalize().scale(0.49));
		Direction face = Direction.getApproximateNearest(eye.x - at.x, eye.y - at.y, eye.z - at.z);
		client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(point, face, chest, false));
		client.player.swing(InteractionHand.MAIN_HAND);
	}

	private static BlockPos aimed(Minecraft client) {
		Vec3 eye = dev.foreyy.dimsclient.freecam.FreeCam.flying()
			? dev.foreyy.dimsclient.freecam.FreeCam.position()
			: client.player.getEyePosition();
		Vec3 look = dev.foreyy.dimsclient.freecam.FreeCam.flying()
			? dev.foreyy.dimsclient.freecam.FreeCam.look()
			: client.player.getLookAngle();
		double reach = dev.foreyy.dimsclient.freecam.FreeCam.flying() ? 256.0 : client.player.blockInteractionRange() + 1.0;
		BlockPos found = null;
		double best = reach * reach;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int span = (int) Math.ceil(reach);
		for (int dx = -span; dx <= span; dx++) {
			for (int dy = -span; dy <= span; dy++) {
				for (int dz = -span; dz <= span; dz++) {
					pos.set(eye.x + dx, eye.y + dy, eye.z + dz);
					if (!isChest(client.level.getBlockState(pos))) {
						continue;
					}
					double distance = Vec3.atCenterOf(pos).distanceToSqr(eye);
					if (distance < best && facing(eye, look, Vec3.atCenterOf(pos))) {
						best = distance;
						found = pos.immutable();
					}
				}
			}
		}
		return found;
	}

	private static boolean isChest(BlockState state) {
		return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof EnderChestBlock;
	}

	/** Roughly under the crosshair: within about twenty degrees of the look. */
	private static boolean facing(Vec3 eye, Vec3 look, Vec3 point) {
		Vec3 toward = point.subtract(eye).normalize();
		return look.dot(toward) > 0.94;
	}
}
