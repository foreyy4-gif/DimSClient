package dev.foreyy.dimsclient.wet;

import dev.foreyy.dimsclient.DimsClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Every exposed face around the player, not just the top. Floors, walls and
 * ceilings all count, so a room reads as wet rather than a field of puddles.
 * Client side only: the world stays dry.
 */
public final class Wet {
	private static final int REACH = 10;
	private static final int PER_TICK = 64;
	private static final double LIFT = 0.0125;
	private static final List<Face> FACES = new ArrayList<>();
	private static int cursor;

	private Wet() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.wetEnabled || client.player == null || client.level == null) {
			FACES.clear();
			return;
		}
		BlockPos origin = client.player.blockPosition();
		int span = REACH * 2 + 1;
		int volume = span * span * span;
		for (int n = 0; n < PER_TICK; n++) {
			int index = Math.floorMod(cursor, volume);
			cursor++;
			int x = origin.getX() - REACH + index % span;
			int z = origin.getZ() - REACH + (index / span) % span;
			int y = origin.getY() - REACH + index / (span * span);
			refresh(client, x, y, z);
		}
		int minX = origin.getX() - REACH;
		int maxX = origin.getX() + REACH;
		int minY = origin.getY() - REACH;
		int maxY = origin.getY() + REACH;
		int minZ = origin.getZ() - REACH;
		int maxZ = origin.getZ() + REACH;
		FACES.removeIf(face -> face.x < minX || face.x > maxX || face.y < minY || face.y > maxY || face.z < minZ || face.z > maxZ);
	}

	public static List<Face> faces() {
		return FACES;
	}

	/** One quad per exposed face of the block's collision shape. */
	private static void refresh(Minecraft client, int x, int y, int z) {
		drop(x, y, z);
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = client.level.getBlockState(pos);
		if (!state.isSolidRender()) {
			return;
		}
		VoxelShape shape = state.getCollisionShape(client.level, pos);
		if (shape.isEmpty()) {
			return;
		}
		for (Direction direction : Direction.values()) {
			if (state.skipRendering(client.level.getBlockState(pos.relative(direction)), direction)) {
				continue;
			}
			VoxelShape face = shape.getFaceShape(direction);
			if (face.isEmpty()) {
				continue;
			}
			for (var box : face.toAabbs()) {
				FACES.add(sheet(x, y, z, direction, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ));
			}
		}
	}

	private static Face sheet(int x, int y, int z, Direction direction, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double lift = LIFT;
		return switch (direction) {
			case UP -> new Face(x, y, z, direction, minX, maxY + lift, minZ, maxX, maxY + lift, maxZ);
			case DOWN -> new Face(x, y, z, direction, minX, minY - lift, maxZ, maxX, minY - lift, minZ);
			case NORTH -> new Face(x, y, z, direction, maxX, minY, minZ - lift, minX, maxY, minZ - lift);
			case SOUTH -> new Face(x, y, z, direction, minX, minY, maxZ + lift, maxX, maxY, maxZ + lift);
			case WEST -> new Face(x, y, z, direction, minX - lift, minY, minZ, minX - lift, maxY, maxZ);
			case EAST -> new Face(x, y, z, direction, maxX + lift, minY, maxZ, maxX + lift, maxY, minZ);
		};
	}

	private static void drop(int x, int y, int z) {
		for (int i = FACES.size() - 1; i >= 0; i--) {
			Face face = FACES.get(i);
			if (face.x == x && face.y == y && face.z == z) {
				FACES.remove(i);
			}
		}
	}

	/**
	 * A single wet quad. Corners are block-local, wound so the normal points out.
	 */
	public record Face(int x, int y, int z, Direction direction,
			double x0, double y0, double z0, double x1, double y1, double z1) {
	}
}
