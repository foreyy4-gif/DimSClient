package dev.foreyy.dimsclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.waypoints.Waypoint;

/**
 * The locator bar only draws a mark inside a 60 degree cone and silently drops the rest.
 * This draws exactly those dropped marks, pinned to whichever edge they fell off.
 */
public final class OffscreenMarks {
	private static final Identifier ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
	private static final Identifier ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");

	private OffscreenMarks() {
	}

	public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		Entity camera = client.getCameraEntity();
		if (camera == null || client.player == null || client.player.connection == null) {
			return;
		}
		Level level = camera.level();
		int top = client.getWindow().getGuiScaledHeight() - 24 - 5;
		int screenMiddle = Mth.ceil((graphics.guiWidth() - 9) / 2.0F);
		client.player.connection.getWaypointManager().forEachWaypoint(camera, waypoint -> {
			double angle = waypoint.yawAngleToCamera(level, client.gameRenderer.mainCamera(), entity -> 1.0F);
			if (Double.isNaN(angle) || (angle > -60.0 && angle <= 60.0)) {
				return;
			}
			double shown = Math.max(-60.0, Math.min(60.0, angle));
			Waypoint.Icon icon = waypoint.icon();
			WaypointStyle style = client.gui.hud.getWaypointStyles().get(icon.style);
			float distance = Mth.sqrt((float) waypoint.distanceSquared(camera));
			int color = icon.color.orElseGet(() -> waypoint.id().map(
				uuid -> ARGB.setBrightness(ARGB.color(255, uuid.hashCode()), 0.9F),
				name -> ARGB.setBrightness(ARGB.color(255, name.hashCode()), 0.9F)));
			int dot = screenMiddle + Mth.floor(shown * 173.0 / 2.0 / 60.0);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.sprite(distance), dot, top - 2, 9, 9, color);
			TrackedWaypoint.PitchDirection pitch = waypoint.pitchDirectionToCamera(level, client.gameRenderer, entity -> 1.0F);
			if (pitch != TrackedWaypoint.PitchDirection.NONE) {
				boolean down = pitch == TrackedWaypoint.PitchDirection.DOWN;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, down ? ARROW_DOWN : ARROW_UP, dot + 1, top + (down ? 6 : -6), 7, 5);
			}
		});
	}
}
