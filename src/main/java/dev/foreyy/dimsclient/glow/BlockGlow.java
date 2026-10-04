package dev.foreyy.dimsclient.glow;

import dev.foreyy.dimsclient.DimsClient;
import dev.foreyy.dimsclient.DimsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Outlines chests and ore veins through walls. The outline is rebuilt a few times a second, not every frame. */
public final class BlockGlow {
	private static final int SLICES = 8;
	private static final int REBUILD_EVERY = 10;
	private static final List<Cell> CELLS = new ArrayList<>();
	private static final List<Shape> SHAPES = new ArrayList<>();
	private static int slice;
	private static int untilRebuild = 1;

	private BlockGlow() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.glowEnabled || client.player == null || client.level == null) {
			CELLS.clear();
			SHAPES.clear();
			return;
		}
		int range = DimsClient.CONFIG.glowRange;
		scanSlice(client, slice, range);
		slice = (slice + 1) % SLICES;
		BlockPos origin = client.player.blockPosition();
		CELLS.removeIf(cell -> outOfRange(cell, origin, range));
		if (--untilRebuild <= 0) {
			untilRebuild = REBUILD_EVERY;
			rebuild();
		}
		paint();
	}

	private static boolean outOfRange(Cell cell, BlockPos origin, int range) {
		return Math.abs(cell.x - origin.getX()) > range || Math.abs(cell.y - origin.getY()) > range || Math.abs(cell.z - origin.getZ()) > range;
	}

	/** One band of chunk sections per tick, and only sections that actually hold an ore or a chest. */
	private static void scanSlice(Minecraft client, int band, int range) {
		BlockPos origin = client.player.blockPosition();
		int minY = client.level.getMinY();
		int span = range * 2 + 1;
		int y0 = origin.getY() - range + band * (span / SLICES);
		int y1 = band == SLICES - 1 ? origin.getY() + range : y0 + span / SLICES - 1;
		CELLS.removeIf(cell -> cell.y >= y0 && cell.y <= y1);
		DimsConfig config = DimsClient.CONFIG;
		int chunkRadius = (range >> 4) + 1;
		int originChunkX = origin.getX() >> 4;
		int originChunkZ = origin.getZ() >> 4;
		for (int chunkX = originChunkX - chunkRadius; chunkX <= originChunkX + chunkRadius; chunkX++) {
			for (int chunkZ = originChunkZ - chunkRadius; chunkZ <= originChunkZ + chunkRadius; chunkZ++) {
				LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, false);
				if (chunk == null) {
					continue;
				}
				LevelChunkSection[] sections = chunk.getSections();
				int sectionStart = Math.max(0, (y0 - minY) >> 4);
				int sectionEnd = Math.min(sections.length - 1, (y1 - minY) >> 4);
				for (int sectionIndex = sectionStart; sectionIndex <= sectionEnd; sectionIndex++) {
					LevelChunkSection section = sections[sectionIndex];
					if (section.hasOnlyAir() || !section.maybeHas(state -> colorOf(state, config) != 0)) {
						continue;
					}
					int baseY = minY + (sectionIndex << 4);
					for (int y = 0; y < 16; y++) {
						int worldY = baseY + y;
						if (worldY < y0 || worldY > y1) {
							continue;
						}
						for (int x = 0; x < 16; x++) {
							int worldX = (chunkX << 4) + x;
							if (Math.abs(worldX - origin.getX()) > range) {
								continue;
							}
							for (int z = 0; z < 16; z++) {
								int worldZ = (chunkZ << 4) + z;
								if (Math.abs(worldZ - origin.getZ()) > range) {
									continue;
								}
								int color = colorOf(section.getBlockState(x, y, z), config);
								if (color != 0) {
									CELLS.add(new Cell(worldX, worldY, worldZ, color));
								}
							}
						}
					}
				}
			}
		}
	}

	private static void rebuild() {
		SHAPES.clear();
		Map<Long, Integer> index = new HashMap<>();
		for (int i = 0; i < CELLS.size(); i++) {
			Cell cell = CELLS.get(i);
			index.put(key(cell.x, cell.y, cell.z), i);
		}
		boolean[] seen = new boolean[CELLS.size()];
		List<Cell> vein = new ArrayList<>();
		for (int i = 0; i < CELLS.size(); i++) {
			if (seen[i]) {
				continue;
			}
			vein.clear();
			int color = CELLS.get(i).color;
			int[] queue = new int[CELLS.size()];
			int head = 0;
			int tail = 0;
			queue[tail++] = i;
			seen[i] = true;
			while (head < tail) {
				Cell cell = CELLS.get(queue[head++]);
				vein.add(cell);
				for (Direction direction : Direction.values()) {
					Integer next = index.get(key(cell.x + direction.getStepX(), cell.y + direction.getStepY(), cell.z + direction.getStepZ()));
					if (next != null && !seen[next] && CELLS.get(next).color == color) {
						seen[next] = true;
						queue[tail++] = next;
					}
				}
			}
			SHAPES.add(shapeOf(vein, color));
		}
	}

	private static Shape shapeOf(List<Cell> vein, int color) {
		int[] fill = new int[vein.size() * 3];
		Set<Long> occupied = new HashSet<>();
		for (int i = 0; i < vein.size(); i++) {
			Cell cell = vein.get(i);
			fill[i * 3] = cell.x;
			fill[i * 3 + 1] = cell.y;
			fill[i * 3 + 2] = cell.z;
			occupied.add(key(cell.x, cell.y, cell.z));
		}
		Map<Edge, Integer> edges = new HashMap<>();
		for (Cell cell : vein) {
			for (Direction direction : Direction.values()) {
				if (occupied.contains(key(cell.x + direction.getStepX(), cell.y + direction.getStepY(), cell.z + direction.getStepZ()))) {
					continue;
				}
				outline(edges, cell, direction);
			}
		}
		int count = 0;
		for (int mask : edges.values()) {
			if (Integer.bitCount(mask) >= 2) {
				count++;
			}
		}
		int[] line = new int[count * 6];
		int cursor = 0;
		for (var entry : edges.entrySet()) {
			if (Integer.bitCount(entry.getValue()) < 2) {
				continue;
			}
			Edge edge = entry.getKey();
			line[cursor++] = edge.x0;
			line[cursor++] = edge.y0;
			line[cursor++] = edge.z0;
			line[cursor++] = edge.x1;
			line[cursor++] = edge.y1;
			line[cursor++] = edge.z1;
		}
		return new Shape(color, fill, line);
	}

	private static void paint() {
		DimsConfig config = DimsClient.CONFIG;
		boolean drawFill = config.glowStyle != 0 && config.glowFillAlpha > 0;
		boolean drawLine = config.glowStyle != 1 && config.glowLineAlpha > 0;
		for (Shape shape : SHAPES) {
			int fill = (config.glowFillAlpha << 24) | (shape.color & 0xFFFFFF);
			int stroke = (config.glowLineAlpha << 24) | (shape.color & 0xFFFFFF);
			if (drawFill) {
				for (int i = 0; i < shape.fill.length; i += 3) {
					int x = shape.fill[i];
					int y = shape.fill[i + 1];
					int z = shape.fill[i + 2];
					Gizmos.cuboid(new AABB(x, y, z, x + 1, y + 1, z + 1), GizmoStyle.fill(fill)).setAlwaysOnTop();
				}
			}
			if (drawLine) {
				for (int i = 0; i < shape.line.length; i += 6) {
					Gizmos.line(new Vec3(shape.line[i], shape.line[i + 1], shape.line[i + 2]),
						new Vec3(shape.line[i + 3], shape.line[i + 4], shape.line[i + 5]), stroke, 2.0F).setAlwaysOnTop();
				}
			}
		}
	}

	private static void outline(Map<Edge, Integer> edges, Cell cell, Direction direction) {
		int x = cell.x;
		int y = cell.y;
		int z = cell.z;
		switch (direction) {
			case DOWN, UP -> {
				int plane = direction == Direction.UP ? y + 1 : y;
				add(edges, x, plane, z, x + 1, plane, z, 1);
				add(edges, x, plane, z + 1, x + 1, plane, z + 1, 1);
				add(edges, x, plane, z, x, plane, z + 1, 1);
				add(edges, x + 1, plane, z, x + 1, plane, z + 1, 1);
			}
			case NORTH, SOUTH -> {
				int plane = direction == Direction.SOUTH ? z + 1 : z;
				add(edges, x, y, plane, x + 1, y, plane, 2);
				add(edges, x, y + 1, plane, x + 1, y + 1, plane, 2);
				add(edges, x, y, plane, x, y + 1, plane, 2);
				add(edges, x + 1, y, plane, x + 1, y + 1, plane, 2);
			}
			case WEST, EAST -> {
				int plane = direction == Direction.EAST ? x + 1 : x;
				add(edges, plane, y, z, plane, y, z + 1, 0);
				add(edges, plane, y + 1, z, plane, y + 1, z + 1, 0);
				add(edges, plane, y, z, plane, y + 1, z, 0);
				add(edges, plane, y, z + 1, plane, y + 1, z + 1, 0);
			}
		}
	}

	private static void add(Map<Edge, Integer> edges, int x0, int y0, int z0, int x1, int y1, int z1, int axis) {
		edges.merge(new Edge(x0, y0, z0, x1, y1, z1), 1 << axis, (left, right) -> left | right);
	}

	private static long key(int x, int y, int z) {
		return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
	}

	private static int colorOf(BlockState state, DimsConfig config) {
		String path = state.getBlock().builtInRegistryHolder().key().identifier().getPath();
		if (isChest(path)) {
			return config.glowChests ? config.glowChestColor : 0;
		}
		if (!path.endsWith("_ore") && !path.equals("ancient_debris")) {
			return 0;
		}
		if (path.contains("coal")) {
			return config.glowCoal ? config.glowCoalColor : 0;
		}
		if (path.contains("iron")) {
			return config.glowIron ? config.glowIronColor : 0;
		}
		if (path.contains("copper")) {
			return config.glowCopper ? config.glowCopperColor : 0;
		}
		if (path.contains("gold")) {
			return config.glowGold ? config.glowGoldColor : 0;
		}
		if (path.contains("redstone")) {
			return config.glowRedstone ? config.glowRedstoneColor : 0;
		}
		if (path.contains("lapis")) {
			return config.glowLapis ? config.glowLapisColor : 0;
		}
		if (path.contains("diamond")) {
			return config.glowDiamond ? config.glowDiamondColor : 0;
		}
		if (path.contains("emerald")) {
			return config.glowEmerald ? config.glowEmeraldColor : 0;
		}
		if (path.contains("quartz")) {
			return config.glowQuartz ? config.glowQuartzColor : 0;
		}
		if (path.equals("ancient_debris")) {
			return config.glowDebris ? config.glowDebrisColor : 0;
		}
		return config.glowOther ? config.glowOtherColor : 0;
	}

	private static boolean isChest(String path) {
		return path.equals("chest") || path.equals("trapped_chest") || path.equals("ender_chest")
			|| path.equals("barrel") || path.endsWith("shulker_box");
	}

	private record Cell(int x, int y, int z, int color) {
	}

	private record Shape(int color, int[] fill, int[] line) {
	}

	private record Edge(int x0, int y0, int z0, int x1, int y1, int z1) {
	}
}
