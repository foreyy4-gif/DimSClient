package dev.foreyy.dimsclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Everything the click GUI can change, in one file so the two modules and the shell stay in step.
 */
public final class DimsConfig {
	public static final float STUN_MIN_RANGE = 2.0F;
	public static final float STUN_MAX_RANGE = 2.3F;
	public static final int STUN_MIN_INTERVAL = 2;
	public static final int STUN_MAX_INTERVAL = 40;
	public static final List<Float> STUN_RANGES = List.of(2.0F, 2.1F, 2.2F, 2.3F);
	public static final List<Integer> STUN_INTERVALS = List.of(2, 4, 6, 8, 10, 15, 20, 30, 40);
	public static final List<Float> HITBOX_EXPANDS = List.of(0.0F, 0.3F, 0.5F, 0.7F, 1.0F, 1.5F, 2.0F);
	public static final List<Integer> CART_LEADS = List.of(5, 6, 7, 8, 9, 10);
	public static final List<Double> CART_RANGES = List.of(5.0, 6.0);
	public static final List<Integer> CART_AIM_SPEEDS = List.of(1, 2, 3, 4, 5, 7, 10, 15, 20);
	public static final List<Integer> DASH_COOLDOWNS = List.of(4, 6, 8, 10, 15, 20, 30);
	public static final List<Integer> GLOW_RANGES = List.of(16, 32, 48, 64, 96, 128, 160, 200);
	public static final List<Double> SPEED_MULTIPLIERS = List.of(1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, 10.0);
	public static final List<Integer> CHASE_RANGES = List.of(6, 8, 12, 16, 24, 32, 48, 64);
	public static final List<Integer> AIR_HEIGHTS = List.of(5, 10, 20, 30, 50, 75, 100, 150, 200);
	public static final List<Integer> GLOW_ALPHAS = List.of(0, 40, 85, 128, 180, 220, 255);
	private static final int[] GLOW_COLORS = {
		0x3DDC6A, 0x4A4A4A, 0xD8AF8A, 0xE07848, 0xFFD24A, 0xFF3B3B, 0x3D5BFF, 0x4EE6E0, 0x2BD96A, 0xF4F0EA, 0x7A3E2E,
		0xFFFFFF, 0xC8C8C8, 0xFF8AD4, 0xB06CFF, 0x7CFF4E, 0xFF9A1F, 0x1FA8FF, 0x111111
	};

	public static int nextGlowColor(int current) {
		for (int i = 0; i < GLOW_COLORS.length; i++) {
			if (GLOW_COLORS[i] == (current & 0xFFFFFF)) {
				return GLOW_COLORS[(i + 1) % GLOW_COLORS.length];
			}
		}
		return GLOW_COLORS[0];
	}


	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dimsclient.json");

	public boolean stunEnabled;
	public float stunRange = 2.3F;
	public int stunIntervalTicks = 10;
	public boolean hitboxEnabled = true;
	/** Draw the grown box in the F3 + B view. */
	public boolean hitboxVisible = true;
	/** Extra size around living things for the crosshair, every weapon. */
	public float hitboxExpand = 0.3F;

	public boolean chestEnabled;

	public boolean cartEnabled = true;
	public int cartLeadTicks = 5;
	public double cartRange = 6.0;
	public boolean cartKeepSlot = true;
	public boolean cartSmoothAim;
	public int cartAimSpeed = 5;
	public boolean cartLegit = true;

	public boolean xbowEnabled;
	public boolean xbowFire;

	public boolean dashEnabled;
	public int dashCooldownTicks = 10;

	public boolean glowEnabled;
	public int glowRange = 32;
	/** 0 outline only, 1 fill only, 2 both. */
	public int glowStyle = 2;
	public int glowFillAlpha = 85;
	public int glowLineAlpha = 255;
	public boolean glowChests = true;
	public int glowChestColor = 0x3DDC6A;
	public boolean glowCoal = true;
	public int glowCoalColor = 0x4A4A4A;
	public boolean glowIron = true;
	public int glowIronColor = 0xD8AF8A;
	public boolean glowCopper = true;
	public int glowCopperColor = 0xE07848;
	public boolean glowGold = true;
	public int glowGoldColor = 0xFFD24A;
	public boolean glowRedstone = true;
	public int glowRedstoneColor = 0xFF3B3B;
	public boolean glowLapis = true;
	public int glowLapisColor = 0x3D5BFF;
	public boolean glowDiamond = true;
	public int glowDiamondColor = 0x4EE6E0;
	public boolean glowEmerald = true;
	public int glowEmeraldColor = 0x2BD96A;
	public boolean glowQuartz = true;
	public int glowQuartzColor = 0xF4F0EA;
	public boolean glowDebris = true;
	public int glowDebrisColor = 0x7A3E2E;
	public boolean glowOther = true;
	public int glowOtherColor = 0xB0B0B0;

	public boolean mineEnabled;
	public boolean baritoneEnabled;
	/** True flies an elytra to the coordinates, false walks. */
	public boolean baritoneElytra;
	/** Registry id of the block to break, empty until one is picked. */
	public String mineBlock = "";
	/** Hotbar slot, 0 to 8, holding the tool. */
	public int mineSlot;

	public boolean chaseEnabled;
	/** Legit only crits. Off is the unleashed mode that also circles the target. */
	public boolean chaseLegit;
	/** Empty follows the nearest player. */
	public String chaseName = "";
	public double chaseSpeed = 4.0;
	/** Walk multiplier for the legit chase. One is a normal walk. */
	public double chaseLegitSpeed = 1.5;
	/** How far from the target the circle runs. */
	public int chaseRange = 16;
	/** Who the chase swings at. Armor means any worn piece, bosses are the wither and the dragon. */
	public boolean chaseNaked = true;
	public boolean chaseArmored = true;
	public boolean chaseMobs;
	public boolean chaseBosses;

	public String chaseFriends = "";
	public String chaseEnemies = "";

	public boolean speedEnabled;
	public double speedMultiplier = 2.0;

	public boolean noFallEnabled;
	public boolean seedEnabled;
	public boolean godEnabled;

	public boolean searchEnabled;
	public boolean searchVillage = true;
	public boolean searchTemple = true;
	public boolean searchOutpost = true;
	public boolean searchMonument = true;
	public boolean searchShip = true;
	public boolean searchPortal = true;
	public boolean searchFortress = true;
	public boolean searchBastion = true;
	public boolean searchEnd = true;
	public boolean searchTrail = true;

	public boolean elytraEnabled;
	public boolean airEnabled;
	public int airHeight = 10;
	public boolean freeCamEnabled;
	public boolean freeCamTeleport;
	public boolean teleportEnabled;
	public boolean teleportBlink;
	public String teleportCoords = "";
	public boolean wetEnabled;
	public boolean clearLavaEnabled;
	public boolean noDarknessEnabled;
	public boolean freeF5Enabled;
	public boolean flyEnabled;
	public boolean flyOwnCrosshair = true;
	public boolean totemEnabled;
	public boolean seeInvisibleEnabled;
	public boolean locatorEnabled;

	public String theme = "ember";
	public int scale = 100;
	public int panelX = -1;
	public int panelY = -1;
	public boolean russian = true;
	public boolean watermark = true;
	public boolean moduleList = true;
	public boolean hudHidden;
	public int hudX = 6;
	public int hudY = 6;
	public boolean widgetFps = true;
	public boolean widgetPlayers;
	public boolean widgetTime;
	public boolean widgetCoords;
	public boolean widgetPanel = true;
	public int fpsX = 6;
	public int fpsY = 6;
	public int playersX = 6;
	public int playersY = 24;
	public int timeX = 6;
	public int timeY = 42;
	public int coordsX = 6;
	public int coordsY = 60;
	public int panelHudX = 6;
	public int panelHudY = 6;

	public void load() {
		if (!Files.exists(FILE)) {
			importLegacy();
			return;
		}
		try (var reader = Files.newBufferedReader(FILE)) {
			Values values = GSON.fromJson(reader, Values.class);
			if (values == null) {
				return;
			}
			stunEnabled = values.stunEnabled;
			stunRange = clampStunRange(values.stunRange);
			stunIntervalTicks = clampStunInterval(values.stunIntervalTicks);
			hitboxEnabled = values.hitboxEnabled;
			hitboxVisible = values.hitboxVisible;
			hitboxExpand = snapHitbox(values.hitboxExpand);
			chestEnabled = values.chestEnabled != null && values.chestEnabled;
			cartEnabled = values.cartEnabled;
			cartLeadTicks = clamp(values.cartLeadTicks, 5, 10);
			cartRange = values.cartRange < 5.5 ? 5.0 : 6.0;
			cartKeepSlot = values.cartKeepSlot;
			cartSmoothAim = values.cartSmoothAim;
			cartAimSpeed = clamp(values.cartAimSpeed, 1, 20);
			cartLegit = values.cartLegit;
			xbowEnabled = values.xbowEnabled != null && values.xbowEnabled;
			xbowFire = values.xbowFire != null && values.xbowFire;
			dashEnabled = values.dashEnabled;
			dashCooldownTicks = clamp(values.dashCooldownTicks == 0 ? 10 : values.dashCooldownTicks, 4, 30);
			glowEnabled = values.glowEnabled;
			glowRange = clamp(values.glowRange == 0 ? 32 : values.glowRange, 8, 64);
			glowStyle = clamp(values.glowStyle == null ? 2 : values.glowStyle, 0, 2);
			glowFillAlpha = snapAlpha(values.glowFillAlpha == null ? 85 : values.glowFillAlpha);
			glowLineAlpha = snapAlpha(values.glowLineAlpha == null ? 255 : values.glowLineAlpha);
			boolean oresOn = values.glowOres == null || values.glowOres;
			glowChests = values.glowChests == null || values.glowChests;
			glowChestColor = colorOr(values.glowChestColor, 0x3DDC6A);
			glowCoal = values.glowCoal == null ? oresOn : values.glowCoal;
			glowCoalColor = colorOr(values.glowCoalColor, 0x4A4A4A);
			glowIron = values.glowIron == null ? oresOn : values.glowIron;
			glowIronColor = colorOr(values.glowIronColor, 0xD8AF8A);
			glowCopper = values.glowCopper == null ? oresOn : values.glowCopper;
			glowCopperColor = colorOr(values.glowCopperColor, 0xE07848);
			glowGold = values.glowGold == null ? oresOn : values.glowGold;
			glowGoldColor = colorOr(values.glowGoldColor, 0xFFD24A);
			glowRedstone = values.glowRedstone == null ? oresOn : values.glowRedstone;
			glowRedstoneColor = colorOr(values.glowRedstoneColor, 0xFF3B3B);
			glowLapis = values.glowLapis == null ? oresOn : values.glowLapis;
			glowLapisColor = colorOr(values.glowLapisColor, 0x3D5BFF);
			glowDiamond = values.glowDiamond == null ? oresOn : values.glowDiamond;
			glowDiamondColor = colorOr(values.glowDiamondColor, 0x4EE6E0);
			glowEmerald = values.glowEmerald == null ? oresOn : values.glowEmerald;
			glowEmeraldColor = colorOr(values.glowEmeraldColor, 0x2BD96A);
			glowQuartz = values.glowQuartz == null ? oresOn : values.glowQuartz;
			glowQuartzColor = colorOr(values.glowQuartzColor, 0xF4F0EA);
			glowDebris = values.glowDebris == null ? oresOn : values.glowDebris;
			glowDebrisColor = colorOr(values.glowDebrisColor, 0x7A3E2E);
			glowOther = values.glowOther == null ? oresOn : values.glowOther;
			glowOtherColor = colorOr(values.glowOtherColor, 0xB0B0B0);
			mineEnabled = values.mineEnabled;
			baritoneEnabled = values.baritoneEnabled != null && values.baritoneEnabled;
			baritoneElytra = values.baritoneElytra != null && values.baritoneElytra;
			mineBlock = values.mineBlock == null ? "" : values.mineBlock;
			mineSlot = clamp(values.mineSlot, 0, 8);
			chaseEnabled = values.chaseEnabled;
			chaseLegit = values.chaseLegit != null && values.chaseLegit;
			chaseName = values.chaseName == null ? "" : values.chaseName;
			chaseSpeed = snapSpeed(values.chaseSpeed == null ? 4.0 : values.chaseSpeed);
			chaseLegitSpeed = snapSpeed(values.chaseLegitSpeed == null ? 1.5 : values.chaseLegitSpeed);
			chaseRange = snapRange(values.chaseRange == null ? 16 : values.chaseRange);
			chaseNaked = values.chaseNaked == null || values.chaseNaked;
			chaseArmored = values.chaseArmored == null || values.chaseArmored;
			chaseMobs = values.chaseMobs != null && values.chaseMobs;
			chaseBosses = values.chaseBosses != null && values.chaseBosses;
			chaseFriends = values.chaseFriends == null ? "" : values.chaseFriends;
			chaseEnemies = values.chaseEnemies == null ? "" : values.chaseEnemies;
			speedEnabled = values.speedEnabled;
			speedMultiplier = snapSpeed(values.speedMultiplier == null ? 2.0 : values.speedMultiplier);
			noFallEnabled = values.noFallEnabled;
			seedEnabled = values.seedEnabled;
			godEnabled = values.godEnabled;
			searchEnabled = values.searchEnabled != null && values.searchEnabled;
			searchVillage = values.searchVillage == null || values.searchVillage;
			searchTemple = values.searchTemple == null || values.searchTemple;
			searchOutpost = values.searchOutpost == null || values.searchOutpost;
			searchMonument = values.searchMonument == null || values.searchMonument;
			searchShip = values.searchShip == null || values.searchShip;
			searchPortal = values.searchPortal == null || values.searchPortal;
			searchFortress = values.searchFortress == null || values.searchFortress;
			searchBastion = values.searchBastion == null || values.searchBastion;
			searchEnd = values.searchEnd == null || values.searchEnd;
			searchTrail = values.searchTrail == null || values.searchTrail;
			elytraEnabled = values.elytraEnabled != null && values.elytraEnabled;
			airEnabled = values.airEnabled != null && values.airEnabled;
			airHeight = snapAir(values.airHeight == null ? 10 : values.airHeight);
			freeCamEnabled = values.freeCamEnabled != null && values.freeCamEnabled;
			freeCamTeleport = values.freeCamTeleport != null && values.freeCamTeleport;
			teleportEnabled = values.teleportEnabled != null && values.teleportEnabled;
			teleportBlink = values.teleportBlink != null && values.teleportBlink;
			teleportCoords = values.teleportCoords == null ? "" : values.teleportCoords;
			wetEnabled = values.wetEnabled != null && values.wetEnabled;
			clearLavaEnabled = values.clearLavaEnabled != null && values.clearLavaEnabled;
			noDarknessEnabled = values.noDarknessEnabled != null && values.noDarknessEnabled;
			freeF5Enabled = values.freeF5Enabled != null && values.freeF5Enabled;
			flyEnabled = values.flyEnabled != null && values.flyEnabled;
			flyOwnCrosshair = values.flyOwnCrosshair == null || values.flyOwnCrosshair;
			totemEnabled = values.totemEnabled != null && values.totemEnabled;
			seeInvisibleEnabled = values.seeInvisibleEnabled != null && values.seeInvisibleEnabled;
			locatorEnabled = values.locatorEnabled != null && values.locatorEnabled;
			theme = Theme.byId(values.theme).id;
			scale = snapScale(values.scale);
			panelX = values.panelX;
			panelY = values.panelY;
			russian = values.russian;
			watermark = values.watermark;
			moduleList = values.moduleList;
			hudHidden = values.hudHidden;
			hudX = values.hudX;
			hudY = values.hudY;
			widgetFps = values.widgetFps;
			widgetPlayers = values.widgetPlayers;
			widgetTime = values.widgetTime;
			widgetCoords = values.widgetCoords;
			widgetPanel = values.widgetPanel == null || values.widgetPanel;
			fpsX = values.fpsX;
			fpsY = values.fpsY;
			playersX = values.playersX;
			playersY = values.playersY;
			timeX = values.timeX;
			timeY = values.timeY;
			coordsX = values.coordsX;
			coordsY = values.coordsY;
			if (values.widgetPanel == null && (values.hudX != 0 || values.hudY != 0)) {
				panelHudX = values.hudX;
				panelHudY = values.hudY;
			} else {
				panelHudX = values.panelHudX;
				panelHudY = values.panelHudY;
			}
		} catch (Exception ignored) {
			importLegacy();
		}
	}

	/** First launch picks up whatever the standalone mods already saved. */
	private void importLegacy() {
		Path dir = FILE.getParent();
		readLegacy(dir.resolve("autostunslam.json"), json -> {
			stunEnabled = json.enabled;
			stunRange = clampStunRange(json.range);
			stunIntervalTicks = clampStunInterval(json.intervalTicks);
		});
		readLegacy(dir.resolve("autocart.json"), json -> {
			cartEnabled = json.enabled;
			cartLeadTicks = clamp(json.placementLeadTicks, 5, 10);
			cartRange = json.interactionRange < 5.5 ? 5.0 : 6.0;
			cartKeepSlot = json.switchToItemSlot;
			cartSmoothAim = json.smoothAim;
			cartAimSpeed = clamp(json.smoothAimSpeed, 1, 20);
			cartLegit = json.legitMode;
		});
	}

	private void readLegacy(Path path, java.util.function.Consumer<Legacy> apply) {
		if (!Files.exists(path)) {
			return;
		}
		try (var reader = Files.newBufferedReader(path)) {
			Legacy legacy = GSON.fromJson(reader, Legacy.class);
			if (legacy != null) {
				apply.accept(legacy);
			}
		} catch (Exception ignored) {
		}
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (var writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(new Values(stunEnabled, stunRange, stunIntervalTicks, hitboxEnabled, hitboxVisible, hitboxExpand, chestEnabled, cartEnabled, cartLeadTicks, cartRange,
					cartKeepSlot, cartSmoothAim, cartAimSpeed, cartLegit, xbowEnabled, xbowFire, dashEnabled, dashCooldownTicks, glowEnabled, glowRange,
					glowStyle, glowFillAlpha, glowLineAlpha,
					glowChests, glowChestColor, glowCoal, glowCoalColor, glowIron, glowIronColor, glowCopper, glowCopperColor,
					glowGold, glowGoldColor, glowRedstone, glowRedstoneColor, glowLapis, glowLapisColor, glowDiamond, glowDiamondColor,
					glowEmerald, glowEmeraldColor, glowQuartz, glowQuartzColor, glowDebris, glowDebrisColor, glowOther, glowOtherColor,
					mineEnabled, mineBlock, mineSlot, baritoneEnabled, baritoneElytra, chaseEnabled, chaseLegit, chaseName, chaseSpeed, chaseLegitSpeed, chaseRange, chaseNaked, chaseArmored, chaseMobs, chaseBosses, chaseFriends, chaseEnemies, speedEnabled, speedMultiplier, noFallEnabled, seedEnabled, godEnabled, searchEnabled, searchVillage, searchTemple, searchOutpost, searchMonument, searchShip, searchPortal, searchFortress, searchBastion, searchEnd, searchTrail, elytraEnabled, airEnabled, airHeight, freeCamEnabled, freeCamTeleport, teleportEnabled, teleportBlink, teleportCoords, wetEnabled, clearLavaEnabled, noDarknessEnabled, freeF5Enabled, flyEnabled, flyOwnCrosshair, totemEnabled, seeInvisibleEnabled, locatorEnabled,
					glowCoal && glowIron && glowCopper && glowGold && glowRedstone && glowLapis && glowDiamond && glowEmerald && glowQuartz && glowDebris && glowOther,
					theme, scale, panelX, panelY, russian,
					watermark, moduleList, hudHidden, hudX, hudY, widgetFps, widgetPlayers, widgetTime, widgetCoords, widgetPanel,
					fpsX, fpsY, playersX, playersY, timeX, timeY, coordsX, coordsY, panelHudX, panelHudY), writer);
			}
		} catch (Exception ignored) {
		}
	}

	public String t(String ru, String en) {
		return russian ? ru : en;
	}

	private static int colorOr(Integer value, int fallback) {
		return value == null ? fallback : value & 0xFFFFFF;
	}

	public static float clampStunRange(float value) {
		float tenths = Math.round(value * 10.0F) / 10.0F;
		return Math.max(STUN_MIN_RANGE, Math.min(STUN_MAX_RANGE, tenths));
	}

	public static int clampStunInterval(int ticks) {
		return Math.max(STUN_MIN_INTERVAL, Math.min(STUN_MAX_INTERVAL, ticks));
	}

	/** Snaps to the closest offered hitbox size. */
	public static float snapHitbox(float value) {
		float best = HITBOX_EXPANDS.getFirst();
		for (float option : HITBOX_EXPANDS) {
			if (Math.abs(option - value) < Math.abs(best - value)) {
				best = option;
			}
		}
		return best;
	}

	public static int snapScale(int value) {
		int snapped = Math.round(value / 10.0F) * 10;
		return Math.max(80, Math.min(130, snapped));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int snapAir(int value) {
		int best = AIR_HEIGHTS.getFirst();
		for (int option : AIR_HEIGHTS) {
			if (Math.abs(option - value) < Math.abs(best - value)) {
				best = option;
			}
		}
		return best;
	}

	private static int snapRange(int value) {
		int best = CHASE_RANGES.getFirst();
		int bestDelta = Math.abs(value - best);
		for (int option : CHASE_RANGES) {
			int delta = Math.abs(value - option);
			if (delta < bestDelta) {
				best = option;
				bestDelta = delta;
			}
		}
		return best;
	}

	private static double snapSpeed(double value) {
		double best = SPEED_MULTIPLIERS.getFirst();
		for (double option : SPEED_MULTIPLIERS) {
			if (Math.abs(option - value) < Math.abs(best - value)) {
				best = option;
			}
		}
		return best;
	}

	private static int snapAlpha(int value) {
		int best = GLOW_ALPHAS.getFirst();
		for (int option : GLOW_ALPHAS) {
			if (Math.abs(option - value) < Math.abs(best - value)) {
				best = option;
			}
		}
		return best;
	}

	private static final class Legacy {
		boolean enabled = true;
		float range = 2.3F;
		int intervalTicks = 10;
		int placementLeadTicks = 5;
		double interactionRange = 6.0;
		boolean switchToItemSlot = true;
		boolean smoothAim;
		int smoothAimSpeed = 5;
		boolean legitMode = true;
	}

	private record Values(boolean stunEnabled, float stunRange, int stunIntervalTicks, boolean hitboxEnabled, boolean hitboxVisible, float hitboxExpand, Boolean chestEnabled, boolean cartEnabled,
			int cartLeadTicks, double cartRange, boolean cartKeepSlot, boolean cartSmoothAim, int cartAimSpeed,
			boolean cartLegit, Boolean xbowEnabled, Boolean xbowFire, boolean dashEnabled, int dashCooldownTicks, boolean glowEnabled, int glowRange,
			Integer glowStyle, Integer glowFillAlpha, Integer glowLineAlpha,
			Boolean glowChests, Integer glowChestColor, Boolean glowCoal, Integer glowCoalColor, Boolean glowIron, Integer glowIronColor,
			Boolean glowCopper, Integer glowCopperColor, Boolean glowGold, Integer glowGoldColor, Boolean glowRedstone, Integer glowRedstoneColor,
			Boolean glowLapis, Integer glowLapisColor, Boolean glowDiamond, Integer glowDiamondColor, Boolean glowEmerald, Integer glowEmeraldColor,
			Boolean glowQuartz, Integer glowQuartzColor, Boolean glowDebris, Integer glowDebrisColor, Boolean glowOther, Integer glowOtherColor, boolean mineEnabled, String mineBlock, int mineSlot, Boolean baritoneEnabled, Boolean baritoneElytra,
			boolean chaseEnabled, Boolean chaseLegit, String chaseName, Double chaseSpeed, Double chaseLegitSpeed, Integer chaseRange, Boolean chaseNaked, Boolean chaseArmored, Boolean chaseMobs, Boolean chaseBosses, String chaseFriends, String chaseEnemies, boolean speedEnabled, Double speedMultiplier, boolean noFallEnabled, boolean seedEnabled, boolean godEnabled, Boolean searchEnabled, Boolean searchVillage, Boolean searchTemple, Boolean searchOutpost, Boolean searchMonument, Boolean searchShip, Boolean searchPortal, Boolean searchFortress, Boolean searchBastion, Boolean searchEnd, Boolean searchTrail, Boolean elytraEnabled, Boolean airEnabled, Integer airHeight, Boolean freeCamEnabled, Boolean freeCamTeleport, Boolean teleportEnabled, Boolean teleportBlink, String teleportCoords, Boolean wetEnabled, Boolean clearLavaEnabled, Boolean noDarknessEnabled, Boolean freeF5Enabled, Boolean flyEnabled, Boolean flyOwnCrosshair, Boolean totemEnabled, Boolean seeInvisibleEnabled, Boolean locatorEnabled,
			Boolean glowOres, String theme, int scale, int panelX, int panelY, boolean russian, boolean watermark,
			boolean moduleList, boolean hudHidden, int hudX, int hudY, boolean widgetFps, boolean widgetPlayers,
			boolean widgetTime, boolean widgetCoords, Boolean widgetPanel, int fpsX, int fpsY, int playersX, int playersY,
			int timeX, int timeY, int coordsX, int coordsY, int panelHudX, int panelHudY) {
	}
}
