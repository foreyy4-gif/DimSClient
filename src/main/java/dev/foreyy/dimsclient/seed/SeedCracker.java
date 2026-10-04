package dev.foreyy.dimsclient.seed;

import dev.foreyy.dimsclient.DimsClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Collects loaded structure chunks and recovers the vanilla world seed from where they stand.
 * The search runs a slice per tick. A layout no vanilla seed places is reported as custom.
 */
public final class SeedCracker {
	private static final int BANDS = 6;
	private static final int SLICE = 1 << 12;
	private static final long X_MUL = 341873128712L;
	private static final long Z_MUL = 132897987541L;
	private static final long MASK = (1L << 48) - 1L;
	private static final long MULT = 25214903917L;
	private static final long ADD = 11L;

	private static int band;
	private static final List<Hit> HITS = new ArrayList<>();
	private static long seed = Long.MIN_VALUE;
	private static boolean custom;
	private static long cursor;
	private static long partial;
	private static boolean scanning;

	private SeedCracker() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.seedEnabled || client.player == null || client.level == null) {
			return;
		}
		scan(client, band);
		band = (band + 1) % BANDS;
		if (band == 0) {
			begin();
		}
		if (scanning) {
			step();
		}
	}

	public static String text() {
		if (!DimsClient.CONFIG.seedEnabled) {
			return "";
		}
		if (custom) {
			return DimsClient.CONFIG.t("генерация кастомная", "custom generation");
		}
		if (seed == Long.MIN_VALUE) {
			return DimsClient.CONFIG.t("мало структур", "not enough structures");
		}
		return Long.toString(seed);
	}

	private static void scan(Minecraft client, int slice) {
		BlockPos origin = client.player.blockPosition();
		int radius = 8;
		int cx = origin.getX() >> 4;
		int cz = origin.getZ() >> 4;
		for (int x = cx - radius + slice; x <= cx + radius; x += BANDS) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				LevelChunk chunk = client.level.getChunkSource().getChunk(x, z, false);
				if (chunk == null) {
					continue;
				}
				Kind kind = kindOf(chunk);
				if (kind != null) {
					remember(new Hit(x, z, kind));
				}
			}
		}
	}

	/** Blocks that belong to one structure and almost nothing else, so a stray block is not a hit. */
	private static Kind kindOf(LevelChunk chunk) {
		boolean blue = false;
		boolean cut = false;
		boolean dark = false;
		boolean prismarine = false;
		boolean chest = false;
		int min = chunk.getMinY();
		var sections = chunk.getSections();
		for (int i = 0; i < sections.length; i++) {
			var section = sections[i];
			if (section.hasOnlyAir()) {
				continue;
			}
			int base = min + (i << 4);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						String path = BuiltInRegistries.BLOCK.getKey(section.getBlockState(x, y, z).getBlock()).getPath();
						if (path.equals("blue_terracotta")) {
							blue = true;
						} else if (path.startsWith("cut_sandstone")) {
							cut = true;
						} else if (path.equals("dark_oak_log")) {
							dark = true;
						} else if (path.contains("prismarine")) {
							prismarine = true;
						} else if (path.equals("chest") && base + y < 48) {
							chest = true;
						}
					}
				}
			}
		}
		if (blue) {
			return Kind.TEMPLE;
		}
		if (prismarine) {
			return Kind.MONUMENT;
		}
		if (cut && dark) {
			return Kind.OUTPOST;
		}
		if (chest && dark) {
			return Kind.SHIP;
		}
		return null;
	}

	private static void remember(Hit hit) {
		for (Hit known : HITS) {
			if (known.x == hit.x && known.z == hit.z) {
				return;
			}
		}
		HITS.add(hit);
		if (HITS.size() > 32) {
			HITS.removeFirst();
		}
	}

	private static void begin() {
		int usable = 0;
		for (Hit hit : HITS) {
			if (offset(hit) != null) {
				usable++;
			}
		}
		if (usable < 4) {
			seed = Long.MIN_VALUE;
			custom = false;
			scanning = false;
			return;
		}
		if (!scanning && seed == Long.MIN_VALUE) {
			partial = sharedPartial();
			cursor = 0L;
			scanning = partial != Long.MIN_VALUE;
			custom = partial == Long.MIN_VALUE;
		}
	}

	/** One slice of the unknown high bits per tick. The low 17 bits are already fixed by the structures. */
	private static void step() {
		long end = Math.min(cursor + SLICE, 1L << 31);
		for (long high = cursor; high < end; high++) {
			long candidate = ((high << 17) | partial) & MASK;
			if (confirms(candidate)) {
				seed = candidate;
				custom = false;
				scanning = false;
				return;
			}
		}
		cursor = end;
		if (cursor >= (1L << 31)) {
			custom = true;
			seed = Long.MIN_VALUE;
			scanning = false;
		}
	}

	/** Every vanilla structure agrees on these low bits. Disagreement means the layout was changed. */
	private static long sharedPartial() {
		long shared = Long.MIN_VALUE;
		for (Hit hit : HITS) {
			int[] spread = offset(hit);
			if (spread == null) {
				continue;
			}
			long bits = ((((spread[0] + 1L) << 31) / hit.kind.limit) << 17) & 0x1FFFFL;
			long got = (bits - regionTerm(hit)) & 0x1FFFFL;
			if (shared == Long.MIN_VALUE) {
				shared = got;
			} else if (shared != got) {
				return Long.MIN_VALUE;
			}
		}
		return shared;
	}

	private static boolean confirms(long worldSeed) {
		int checked = 0;
		for (Hit hit : HITS) {
			int[] spread = offset(hit);
			if (spread == null) {
				continue;
			}
			if (!places(hit, spread, worldSeed)) {
				return false;
			}
			checked++;
		}
		return checked >= 4;
	}

	private static boolean places(Hit hit, int[] spread, long worldSeed) {
		long mixed = (regionTerm(hit) + worldSeed) & MASK;
		long state = (mixed ^ MULT) & MASK;
		int x = nextInt(state, hit.kind.limit);
		state = (state * MULT + ADD) & MASK;
		int z = nextInt(state, hit.kind.limit);
		return x == spread[0] && z == spread[1];
	}

	private static int nextInt(long state, int bound) {
		return (int) (((state >>> 17) * bound) >>> 31);
	}

	private static long regionTerm(Hit hit) {
		int regionX = Math.floorDiv(hit.x, hit.kind.spacing);
		int regionZ = Math.floorDiv(hit.z, hit.kind.spacing);
		return (regionX * X_MUL + regionZ * Z_MUL + hit.kind.salt) & MASK;
	}

	/** Null when the chunk lies past the ring vanilla may place this structure in. */
	private static int[] offset(Hit hit) {
		int x = Math.floorMod(hit.x, hit.kind.spacing);
		int z = Math.floorMod(hit.z, hit.kind.spacing);
		if (x >= hit.kind.limit || z >= hit.kind.limit) {
			return null;
		}
		return new int[]{x, z};
	}

	private record Hit(int x, int z, Kind kind) {
	}

	private enum Kind {
		TEMPLE(32, 8, 14357617),
		OUTPOST(32, 8, 165745296),
		MONUMENT(32, 5, 10387313),
		SHIP(24, 4, 165745295);

		final int spacing;
		final int limit;
		final int salt;

		Kind(int spacing, int separation, int salt) {
			this.spacing = spacing;
			this.limit = spacing - separation;
			this.salt = salt;
		}
	}
}
