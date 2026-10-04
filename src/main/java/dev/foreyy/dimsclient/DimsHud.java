package dev.foreyy.dimsclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

/** Small status readout so the modules are visible on camera without opening the menu. */
final class DimsHud {
	private static boolean dragging;
	private static int grabX;
	private static int grabY;
	private static int draggingCard = -1;
	private static int cardGrabX;
	private static int cardGrabY;

	private DimsHud() {
	}

	static void render(GuiGraphicsExtractor graphics) {
		DimsConfig config = DimsClient.CONFIG;
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return;
		}
		dev.foreyy.dimsclient.ui.PlayerLocator.render(graphics, client);
		if (!widgetsOn(config)) {
			return;
		}
		boolean editing = editing(client);
		if (config.hudHidden && !editing) {
			return;
		}
		if (client.gui.screen() != null && !editing) {
			return;
		}

		Theme theme = Theme.byId(config.theme);
		Font font = client.font;
		int screenWidth = client.getWindow().getGuiScaledWidth();
		int screenHeight = client.getWindow().getGuiScaledHeight();
		drawWidgets(graphics, font, theme, client, config, screenWidth, editing);
		dev.foreyy.dimsclient.ui.OffscreenMarks.render(graphics, client);
	}

	/** Hold the move key to drag the readout. Right click hides it until the key is held again. */
	private static void drag(Minecraft client, DimsConfig config, int screenWidth, int screenHeight, int blockWidth, int blockHeight) {
		double mx = client.mouseHandler.getScaledXPos(client.getWindow());
		double my = client.mouseHandler.getScaledYPos(client.getWindow());
		long window = client.getWindow().handle();
		boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		if (right && contains(config.hudX, config.hudY, blockWidth, blockHeight, mx, my)) {
			config.hudHidden = !config.hudHidden;
			config.save();
		}
		if (left && !dragging && contains(config.hudX, config.hudY, blockWidth, blockHeight, mx, my)) {
			dragging = true;
			grabX = (int) mx - config.hudX;
			grabY = (int) my - config.hudY;
		}
		if (!left) {
			if (dragging) {
				config.save();
			}
			dragging = false;
		}
		if (dragging) {
			config.hudX = Mth.clamp((int) mx - grabX, 0, Math.max(0, screenWidth - blockWidth));
			config.hudY = Mth.clamp((int) my - grabY, 0, Math.max(0, screenHeight - blockHeight));
		}
	}

	private static void drawWidgets(GuiGraphicsExtractor graphics, Font font, Theme theme, Minecraft client, DimsConfig config,
			int screenWidth, boolean editing) {
		int screenHeight = client.getWindow().getGuiScaledHeight();
		int shown = 0;
		int[] xs = new int[5];
		int[] ys = new int[5];
		int[] widths = new int[5];
		int[] heights = new int[5];
		String[] texts = new String[5];
		if (config.widgetPanel) {
			texts[shown] = null;
			xs[shown] = config.panelHudX;
			ys[shown] = config.panelHudY;
			widths[shown] = panelWidth(font, config);
			heights[shown] = panelHeight(config);
			shown++;
		}
		if (config.widgetFps) {
			texts[shown] = fpsText(client);
			xs[shown] = config.fpsX;
			ys[shown] = config.fpsY;
			widths[shown] = font.width(texts[shown]) + 12;
			heights[shown] = 14;
			shown++;
		}
		if (config.widgetTime) {
			texts[shown] = clock(client);
			xs[shown] = config.timeX;
			ys[shown] = config.timeY;
			widths[shown] = font.width(texts[shown]) + 12;
			heights[shown] = 14;
			shown++;
		}
		if (config.widgetCoords) {
			texts[shown] = String.format("%.0f  %.0f  %.0f", client.player.getX(), client.player.getY(), client.player.getZ());
			xs[shown] = config.coordsX;
			ys[shown] = config.coordsY;
			widths[shown] = font.width(texts[shown]) + 12;
			heights[shown] = 14;
			shown++;
		}
		if (config.widgetPlayers) {
			drawPlayerArrows(graphics, font, theme, client, screenWidth, screenHeight);
		}
		if (shown == 0) {
			draggingCard = -1;
			return;
		}
		if (editing) {
			dragCards(client, config, screenWidth, screenHeight, xs, ys, widths, heights, shown);
		} else if (draggingCard != -1) {
			draggingCard = -1;
			config.save();
		}
		for (int i = 0; i < shown; i++) {
			int x = Mth.clamp(xs[i], 0, Math.max(0, screenWidth - widths[i]));
			int y = Mth.clamp(ys[i], 0, Math.max(0, screenHeight - heights[i]));
			if (editing) {
				graphics.fill(x - 2, y - 2, x + widths[i] + 2, y + heights[i] + 2, theme.edge);
			}
			if (texts[i] == null) {
				drawPanel(graphics, font, theme, config, x, y);
			} else {
				graphics.fill(x, y, x + widths[i], y + 14, theme.glass() ? glass(theme.panel, 150) : theme.panel);
				graphics.fill(x, y, x + widths[i], y + 2, theme.accent);
				graphics.text(font, texts[i], x + 6, y + 3, theme.text, false);
			}
		}
	}

	private static void drawPanel(GuiGraphicsExtractor graphics, Font font, Theme theme, DimsConfig config, int x, int y) {
		if (config.seedEnabled) {
			String seed = dev.foreyy.dimsclient.seed.SeedCracker.text();
			if (!seed.isEmpty()) {
				y = line(graphics, font, theme, x, y, seed, true);
			}
		}
		if (config.watermark) {
			String mark = "DimS";
			int markWidth = font.width(mark) + 10;
			graphics.fill(x, y, x + markWidth, y + 14, theme.glass() ? glass(theme.panel, 150) : theme.panel);
			graphics.fill(x, y, x + 2, y + 14, theme.accent);
			graphics.text(font, mark, x + 6, y + 3, theme.text, false);
			y += 17;
		}
		if (config.moduleList) {
			y = line(graphics, font, theme, x, y, "AutoStunSlam", config.stunEnabled);
			y = line(graphics, font, theme, x, y, "AutoCart", config.cartEnabled);
			y = line(graphics, font, theme, x, y, "ChestReach", config.chestEnabled);
			y = line(graphics, font, theme, x, y, "AutoXBow", config.xbowEnabled);
			y = line(graphics, font, theme, x, y, "Hitboxes", config.hitboxEnabled);
			y = line(graphics, font, theme, x, y, "Dash", config.dashEnabled);
			y = line(graphics, font, theme, x, y, "XRay", config.glowEnabled);
			y = line(graphics, font, theme, x, y, "AutoFarm", config.mineEnabled);
			y = line(graphics, font, theme, x, y, "Baritone", config.baritoneEnabled);
			y = line(graphics, font, theme, x, y, "KillAura", config.chaseEnabled);
			y = line(graphics, font, theme, x, y, "Speed", config.speedEnabled);
			y = line(graphics, font, theme, x, y, "NoFall", config.noFallEnabled);
			y = line(graphics, font, theme, x, y, "SeedCracker", config.seedEnabled);
			y = line(graphics, font, theme, x, y, "GodMode", config.godEnabled);
			y = line(graphics, font, theme, x, y, "AutoSearch", config.searchEnabled);
			y = line(graphics, font, theme, x, y, "ElytraTarget", config.elytraEnabled);
			y = line(graphics, font, theme, x, y, "AirStuck", config.airEnabled);
			y = line(graphics, font, theme, x, y, "FreeCam", config.freeCamEnabled);
			y = line(graphics, font, theme, x, y, "Teleport", config.teleportEnabled);
			y = line(graphics, font, theme, x, y, "Wet", config.wetEnabled);
			y = line(graphics, font, theme, x, y, "ClearLava", config.clearLavaEnabled);
			y = line(graphics, font, theme, x, y, "NoDarkness", config.noDarknessEnabled);
			y = line(graphics, font, theme, x, y, "FreeF5", config.freeF5Enabled);
			y = line(graphics, font, theme, x, y, "Fly", config.flyEnabled);
			y = line(graphics, font, theme, x, y, "AutoTotem", config.totemEnabled);
			y = line(graphics, font, theme, x, y, "SeeInvisible", config.seeInvisibleEnabled);
			line(graphics, font, theme, x, y, "Locator", config.locatorEnabled);
		}
	}

	private static int panelWidth(Font font, DimsConfig config) {
		int width = config.watermark ? font.width("DimS") + 10 : 0;
		if (config.moduleList) {
			width = Math.max(width, font.width("AutoStunSlam") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoCart") + font.width("OFF") + 16);
			width = Math.max(width, font.width("ChestReach") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoXBow") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Hitboxes") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Dash") + font.width("OFF") + 16);
			width = Math.max(width, font.width("XRay") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoFarm") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Baritone") + font.width("OFF") + 16);
			width = Math.max(width, font.width("KillAura") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Speed") + font.width("OFF") + 16);
			width = Math.max(width, font.width("NoFall") + font.width("OFF") + 16);
			width = Math.max(width, font.width("SeedCracker") + font.width("OFF") + 16);
			width = Math.max(width, font.width("GodMode") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoSearch") + font.width("OFF") + 16);
			width = Math.max(width, font.width("ElytraTarget") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AirStuck") + font.width("OFF") + 16);
			width = Math.max(width, font.width("FreeCam") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Teleport") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Wet") + font.width("OFF") + 16);
			width = Math.max(width, font.width("ClearLava") + font.width("OFF") + 16);
			width = Math.max(width, font.width("NoDarkness") + font.width("OFF") + 16);
			width = Math.max(width, font.width("FreeF5") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Fly") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoTotem") + font.width("OFF") + 16);
			width = Math.max(width, font.width("SeeInvisible") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Locator") + font.width("OFF") + 16);
		}
		return Math.max(width, 20);
	}

	private static int panelHeight(DimsConfig config) {
		int height = 0;
		if (config.watermark) {
			height += 17;
		}
		if (config.moduleList) {
			height += 17 * 27;
		}
		return Math.max(14, height - (config.watermark && config.moduleList ? 2 : 0));
	}

	private static void dragCards(Minecraft client, DimsConfig config, int screenWidth, int screenHeight, int[] xs, int[] ys,
			int[] widths, int[] heights, int shown) {
		double mx = client.mouseHandler.getScaledXPos(client.getWindow());
		double my = client.mouseHandler.getScaledYPos(client.getWindow());
		boolean left = GLFW.glfwGetMouseButton(client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (left && draggingCard == -1) {
			for (int i = shown - 1; i >= 0; i--) {
				if (contains(xs[i], ys[i], widths[i], heights[i], mx, my)) {
					draggingCard = cardId(config, i);
					cardGrabX = (int) mx - xs[i];
					cardGrabY = (int) my - ys[i];
					break;
				}
			}
		}
		if (!left) {
			if (draggingCard != -1) {
				config.save();
			}
			draggingCard = -1;
		}
		if (draggingCard == -1) {
			return;
		}
		int index = indexOf(config, draggingCard, shown);
		if (index == -1) {
			return;
		}
		int x = Mth.clamp((int) mx - cardGrabX, 0, Math.max(0, screenWidth - widths[index]));
		int y = Mth.clamp((int) my - cardGrabY, 0, Math.max(0, screenHeight - heights[index]));
		place(config, draggingCard, x, y);
		xs[index] = x;
		ys[index] = y;
	}

	/** Stable id so a card keeps being dragged when another card turns on. */
	private static int cardId(DimsConfig config, int shownIndex) {
		int seen = 0;
		int id = 0;
		for (boolean on : new boolean[]{config.widgetPanel, config.widgetFps, config.widgetPlayers, config.widgetTime, config.widgetCoords}) {
			if (on && seen++ == shownIndex) {
				return id;
			}
			id++;
		}
		return 0;
	}

	private static int indexOf(DimsConfig config, int id, int shown) {
		int seen = 0;
		int current = 0;
		for (boolean on : new boolean[]{config.widgetPanel, config.widgetFps, config.widgetPlayers, config.widgetTime, config.widgetCoords}) {
			if (on) {
				if (current == id) {
					return seen;
				}
				seen++;
			}
			current++;
			if (seen == shown) {
				break;
			}
		}
		return -1;
	}

	private static void place(DimsConfig config, int id, int x, int y) {
		switch (id) {
			case 0 -> {
				config.panelHudX = x;
				config.panelHudY = y;
			}
			case 1 -> {
				config.fpsX = x;
				config.fpsY = y;
			}
			case 2 -> {
				config.playersX = x;
				config.playersY = y;
			}
			case 3 -> {
				config.timeX = x;
				config.timeY = y;
			}
			default -> {
				config.coordsX = x;
				config.coordsY = y;
			}
		}
	}

	private static boolean widgetsOn(DimsConfig config) {
		return config.widgetPanel || config.widgetFps || config.widgetPlayers || config.widgetTime || config.widgetCoords;
	}

	private static String fpsText(Minecraft client) {
		int ping = ping(client);
		return ping < 0 ? client.getFps() + " FPS" : client.getFps() + " FPS  " + ping + " ms";
	}

	private static int ping(Minecraft client) {
		if (client.player == null || client.player.connection == null) {
			return -1;
		}
		PlayerInfo info = client.player.connection.getPlayerInfo(client.player.getUUID());
		return info == null ? -1 : info.getLatency();
	}

	/** Arrows around the crosshair point at nearby players, with their coordinates beside the tip. */
	private static void drawPlayerArrows(GuiGraphicsExtractor graphics, Font font, Theme theme, Minecraft client, int screenWidth, int screenHeight) {
		float yaw = client.player.getYRot() * ((float) Math.PI / 180.0F);
		float lookX = -Mth.sin(yaw);
		float lookZ = Mth.cos(yaw);
		float sideX = Mth.cos(yaw);
		float sideZ = Mth.sin(yaw);
		int cx = screenWidth / 2;
		int cy = screenHeight / 2;
		int drawn = 0;
		for (Player player : client.level.players()) {
			if (player == client.player || !player.isAlive()) {
				continue;
			}
			double dx = player.getX() - client.player.getX();
			double dz = player.getZ() - client.player.getZ();
			double flat = Math.sqrt(dx * dx + dz * dz);
			if (flat < 0.5 || flat > 64.0) {
				continue;
			}
			float forward = (float) ((dx * lookX + dz * lookZ) / flat);
			float right = (float) ((dx * sideX + dz * sideZ) / flat);
			int radius = 28;
			int tipX = cx + Math.round(right * radius);
			int tipY = cy - Math.round(forward * radius);
			arrow(graphics, tipX, tipY, right, -forward, theme.accent);
			String label = String.format("%.0f %.0f %.0f", player.getX(), player.getY(), player.getZ());
			int textX = tipX + Math.round(right * 8) + (right >= 0 ? 2 : -2 - font.width(label));
			int textY = tipY - Math.round(forward * 8) - 4;
			graphics.text(font, label, textX, textY, theme.text, true);
			if (++drawn == 8) {
				break;
			}
		}
	}

	private static void arrow(GuiGraphicsExtractor graphics, int tipX, int tipY, float dirX, float dirY, int color) {
		float length = (float) Math.sqrt(dirX * dirX + dirY * dirY);
		if (length < 0.001F) {
			return;
		}
		dirX /= length;
		dirY /= length;
		float px = -dirY;
		float py = dirX;
		int tailX = tipX - Math.round(dirX * 7);
		int tailY = tipY - Math.round(dirY * 7);
		int leftX = tipX - Math.round(dirX * 4 + px * 3);
		int leftY = tipY - Math.round(dirY * 4 + py * 3);
		int rightX = tipX - Math.round(dirX * 4 - px * 3);
		int rightY = tipY - Math.round(dirY * 4 - py * 3);
		stroke(graphics, tailX, tailY, tipX, tipY, color);
		stroke(graphics, tipX, tipY, leftX, leftY, color);
		stroke(graphics, tipX, tipY, rightX, rightY, color);
	}

	private static void stroke(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		if (steps == 0) {
			graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
			return;
		}
		for (int i = 0; i <= steps; i++) {
			int x = x0 + (x1 - x0) * i / steps;
			int y = y0 + (y1 - y0) * i / steps;
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	private static String clock(Minecraft client) {
		long time = client.level.getOverworldClockTime() % 24000L;
		int hours = (int) ((time / 1000L + 6) % 24);
		int minutes = (int) (time % 1000L * 60L / 1000L);
		return String.format("%02d:%02d", hours, minutes);
	}

	private static boolean editing(Minecraft client) {
		return DimsClient.MOVE_HUD.isDown() && client.gui.screen() == null;
	}

	private static boolean contains(int x, int y, int width, int height, double mx, double my) {
		return mx >= x - 3 && mx < x + width + 3 && my >= y - 3 && my < y + height + 3;
	}

	static int width(Font font, DimsConfig config) {
		int width = config.watermark ? font.width("DimS") + 10 : 0;
		if (config.moduleList) {
			width = Math.max(width, font.width("ShieldBreak") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoCart") + font.width("OFF") + 16);
			width = Math.max(width, font.width("ChestReach") + font.width("OFF") + 16);
			width = Math.max(width, font.width("AutoXBow") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Hitboxes") + font.width("OFF") + 16);
			width = Math.max(width, font.width("Dash") + font.width("OFF") + 16);
		}
		return width;
	}

	static int height(DimsConfig config) {
		int height = 0;
		if (config.watermark) {
			height += 17;
		}
		if (config.moduleList) {
			height += 60;
		}
		return Math.max(0, height - 2);
	}

	private static int glass(int color, int alpha) {
		return net.minecraft.util.ARGB.color(alpha, color);
	}

	private static int line(GuiGraphicsExtractor graphics, Font font, Theme theme, int x, int y, String label, boolean on) {
		String state = on ? "ON" : "OFF";
		int width = font.width(label) + font.width(state) + 16;
		graphics.fill(x, y, x + width, y + 13, theme.glass() ? glass(theme.background, 150) : theme.background);
		graphics.text(font, label, x + 4, y + 3, theme.muted, false);
		graphics.text(font, state, x + width - 4 - font.width(state), y + 3, on ? theme.on : theme.off(), false);
		return y + 15;
	}
}
