package dev.foreyy.dimsclient.search;

import dev.foreyy.dimsclient.DimsClient;
import dev.foreyy.dimsclient.DimsConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

/** Walks loaded chunks a few at a time and marks the structures whose blocks are actually there. */
public final class AutoSearch {
	private static final int PER_TICK = 3;
	private static final List<Mark> MARKS = new ArrayList<>();
	private static int cursor;

	private AutoSearch() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.searchEnabled || client.player == null || client.level == null) {
			MARKS.clear();
			return;
		}
		int radius = client.options.getEffectiveRenderDistance();
		BlockPos origin = client.player.blockPosition();
		int span = radius * 2 + 1;
		int total = span * span;
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		for (int n = 0; n < PER_TICK; n++) {
			int index = Math.floorMod(cursor, total);
			cursor++;
			int dx = index % span - radius;
			int dz = index / span - radius;
			LevelChunk chunk = client.level.getChunkSource().getChunk(cx + dx, cz + dz, false);
			if (chunk == null) {
				continue;
			}
			replace(cx + dx, cz + dz, kindOf(chunk));
		}
		MARKS.removeIf(mark -> Math.abs(mark.x - cx) > radius || Math.abs(mark.z - cz) > radius);
		paint();
	}

	private static void replace(int x, int z, Kind kind) {
		Iterator<Mark> it = MARKS.iterator();
		while (it.hasNext()) {
			Mark mark = it.next();
			if (mark.x == x && mark.z == z) {
				it.remove();
				break;
			}
		}
		if (kind != null) {
			MARKS.add(new Mark(x, z, kind));
		}
	}

	/** One pass, and it stops at the first block that names a structure. */
	private static Kind kindOf(LevelChunk chunk) {
		var sections = chunk.getSections();
		for (var section : sections) {
			if (section.hasOnlyAir()) {
				continue;
			}
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						String path = BuiltInRegistries.BLOCK.getKey(section.getBlockState(x, y, z).getBlock()).getPath();
						Kind kind = match(path);
						if (kind != null) {
							return kind;
						}
					}
				}
			}
		}
		return null;
	}

	private static Kind match(String path) {
		DimsConfig config = DimsClient.CONFIG;
		if (config.searchVillage && path.equals("bell")) {
			return Kind.VILLAGE;
		}
		if (config.searchTemple && path.equals("blue_terracotta")) {
			return Kind.TEMPLE;
		}
		if (config.searchOutpost && path.equals("dark_oak_log")) {
			return Kind.OUTPOST;
		}
		if (config.searchMonument && path.equals("prismarine_bricks")) {
			return Kind.MONUMENT;
		}
		if (config.searchShip && path.equals("oak_trapdoor")) {
			return Kind.SHIP;
		}
		if (config.searchPortal && path.equals("crying_obsidian")) {
			return Kind.PORTAL;
		}
		if (config.searchFortress && path.equals("nether_bricks")) {
			return Kind.FORTRESS;
		}
		if (config.searchBastion && path.equals("gilded_blackstone")) {
			return Kind.BASTION;
		}
		if (config.searchEnd && path.equals("purpur_pillar")) {
			return Kind.END_CITY;
		}
		if (config.searchTrail && path.equals("trial_spawner")) {
			return Kind.TRAIL;
		}
		return null;
	}

	private static void paint() {
		for (Mark mark : MARKS) {
			int x = mark.x << 4;
			int z = mark.z << 4;
			Gizmos.cuboid(new AABB(x, 64, z, x + 16, 192, z + 16), GizmoStyle.stroke((0xFF << 24) | mark.kind.color)).setAlwaysOnTop();
		}
	}

	private record Mark(int x, int z, Kind kind) {
	}

	private enum Kind {
		VILLAGE(0x3DDC6A),
		TEMPLE(0xE07848),
		OUTPOST(0x7A3E2E),
		MONUMENT(0x4EE6E0),
		SHIP(0xD8AF8A),
		PORTAL(0xB06CFF),
		FORTRESS(0xFF3B3B),
		BASTION(0x4A4A4A),
		END_CITY(0xFFD24A),
		TRAIL(0xE07848);

		final int color;

		Kind(int color) {
			this.color = color;
		}
	}
}
