package dev.foreyy.dimsclient.mine;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Digs a 1x2 tunnel along the look and breaks any ore that shows up beside it. */
public final class AutoMine {
	private static BlockPos current = BlockPos.ZERO;
	private static boolean breaking;

	private AutoMine() {
	}

	public static void tick(Minecraft client) {
		if (client.gameMode == null || client.player == null) {
			return;
		}
		// Baritone mines and picks up the drops itself, so the tunnel digger stays out of its way.
		if (DimsClient.CONFIG.baritoneEnabled) {
			stop(client.gameMode);
			return;
		}
		int slot = DimsClient.CONFIG.mineSlot;
		if (client.player.getInventory().getSelectedSlot() != slot) {
			client.player.getInventory().setSelectedSlot(slot);
		}
		BlockPos next = nextBlock(client);
		if (next == null) {
			stop(client.gameMode);
			walk(client.player, false);
			return;
		}
		if (!breaking || !next.equals(current)) {
			stop(client.gameMode);
			current = next;
			breaking = true;
			client.gameMode.startDestroyBlock(next, Direction.UP);
		} else {
			client.gameMode.continueDestroyBlock(current, Direction.UP);
		}
		walk(client.player, next.getY() <= client.player.getBlockY() + 1 && flat(client.player, next) < 1.6);
	}

	public static void stop(MultiPlayerGameMode mode) {
		if (breaking) {
			mode.stopDestroyBlock();
			breaking = false;
		}
	}

	private static BlockPos nextBlock(Minecraft client) {
		LocalPlayer self = client.player;
		Block ore = targetBlock();
		BlockPos found = nearestOre(client, ore);
		if (found != null) {
			return found;
		}
		Direction facing = self.getDirection();
		BlockPos ahead = self.blockPosition().relative(facing);
		if (solid(client, ahead)) {
			return ahead;
		}
		BlockPos head = ahead.above();
		if (solid(client, head)) {
			return head;
		}
		return null;
	}

	private static BlockPos nearestOre(Minecraft client, Block target) {
		int range = (int) Math.ceil(client.player.blockInteractionRange());
		BlockPos origin = client.player.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = -range; y <= range; y++) {
			for (int x = -range; x <= range; x++) {
				for (int z = -range; z <= range; z++) {
					cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
					if (!client.player.isWithinBlockInteractionRange(cursor, 0.5)) {
						continue;
					}
					BlockState state = client.level.getBlockState(cursor);
					if (!wanted(state, target)) {
						continue;
					}
					double distance = cursor.distToCenterSqr(client.player.getEyePosition());
					if (distance < bestDistance) {
						bestDistance = distance;
						best = cursor.immutable();
					}
				}
			}
		}
		return best;
	}

	private static boolean wanted(BlockState state, Block target) {
		if (state.isAir()) {
			return false;
		}
		if (target != null) {
			return state.is(target);
		}
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		return path.endsWith("_ore") || path.equals("ancient_debris");
	}

	private static boolean solid(Minecraft client, BlockPos pos) {
		BlockState state = client.level.getBlockState(pos);
		return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(client.level, pos) >= 0.0F;
	}

	private static void walk(LocalPlayer self, boolean into) {
		self.input.keyPresses = new Input(into, false, false, false, false, false, into);
		self.setSprinting(into);
	}

	private static double flat(LocalPlayer self, BlockPos pos) {
		double dx = pos.getX() + 0.5 - self.getX();
		double dz = pos.getZ() + 0.5 - self.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static Block targetBlock() {
		Identifier id = Identifier.tryParse(DimsClient.CONFIG.mineBlock);
		if (id == null) {
			return null;
		}
		return BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
	}

	private static String lookedAt = "";

	public static void rememberLookedAt(Minecraft client) {
		lookedAt = idOf(client);
	}

	public static void pickLookedAt(Minecraft client) {
		String id = lookedAt.isEmpty() ? idOf(client) : lookedAt;
		if (id.isEmpty()) {
			DimsClient.CONFIG.mineBlock = "";
			DimsClient.CONFIG.save();
			return;
		}
		DimsClient.CONFIG.mineBlock = id;
		DimsClient.CONFIG.save();
	}

	private static String idOf(Minecraft client) {
		if (client.level == null) {
			return "";
		}
		HitResult hit = client.hitResult;
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return "";
		}
		BlockState state = client.level.getBlockState(blockHit.getBlockPos());
		if (state.isAir()) {
			return "";
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}
}
