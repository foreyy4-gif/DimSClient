package dev.foreyy.dimsclient.ui;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The server locator only exists when the server sends waypoints. This one is drawn from the
 * players the client already has, so it stays when the server turns the locator off.
 */
public final class PlayerLocator {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
	private static final Identifier DOT = Identifier.withDefaultNamespace("hud/locator_bar_dot/default_0");
	private static final Identifier ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
	private static final Identifier ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");

	private PlayerLocator() {
	}

	public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		if (!DimsClient.CONFIG.locatorEnabled || client.player == null || client.level == null || client.gui.screen() != null) {
			return;
		}
		Entity camera = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
		int top = client.getWindow().getGuiScaledHeight() - 24 - 5;
		int left = Mth.ceil((graphics.guiWidth() - 182) / 2.0F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 182, 5);
		int middle = Mth.ceil((graphics.guiWidth() - 9) / 2.0F);
		Vec3 look = camera.getLookAngle();
		double facing = Math.atan2(-look.x, look.z);
		for (AbstractClientPlayer other : client.level.players()) {
			if (other == client.player || !other.isAlive()) {
				continue;
			}
			double dx = other.getX() - camera.getX();
			double dz = other.getZ() - camera.getZ();
			double angle = Math.toDegrees(Mth.wrapDegrees(Math.atan2(-dx, dz) - facing));
			if (angle <= -60.0 || angle > 60.0) {
				continue;
			}
			int color = ARGB.setBrightness(ARGB.color(255, other.getUUID().hashCode()), 0.9F);
			int dot = middle + Mth.floor(angle * 173.0 / 2.0 / 60.0);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DOT, dot, top - 2, 9, 9, color);
			double dy = other.getY() - camera.getY();
			if (Math.abs(dy) > 8.0) {
				boolean down = dy < 0.0;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, down ? ARROW_DOWN : ARROW_UP, dot + 1, top + (down ? 6 : -6), 7, 5);
			}
		}
	}
}
