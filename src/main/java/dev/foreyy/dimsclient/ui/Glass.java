package dev.foreyy.dimsclient.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

/**
 * A rounded, translucent panel. The GUI pipeline only draws rectangles, so the curve is built
 * from a fan of quads instead of a stack of filled rows.
 */
public final class Glass {
	private static final int CORNER_STEPS = 16;

	private Glass() {
	}

	/** Fills a rounded rectangle. Alpha comes from the colour. */
	public static void round(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		graphics.guiRenderState.addGuiElement(new Sheet(new Matrix3x2f(graphics.pose()), x, y, width, height, r, color, graphics.scissorStack.peek()));
	}

	/** A fully rounded track, the shape of a switch. */
	public static void pill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		round(graphics, x, y, width, height, height / 2, color);
	}

	/** Same shape, but the colour fades from the top edge down to clear. */
	public static void sheen(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int radius, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
		graphics.guiRenderState.addGuiElement(new Sheet(new Matrix3x2f(graphics.pose()), x, y, width, height, r, color, graphics.scissorStack.peek(), true));
	}

	public static int withAlpha(int color, int alpha) {
		return ARGB.color(alpha, color);
	}

	private record Sheet(Matrix3x2f pose, int x, int y, int width, int height, int radius, int color,
			ScreenRectangle scissor, boolean fade) implements GuiElementRenderState {
		Sheet(Matrix3x2f pose, int x, int y, int width, int height, int radius, int color, ScreenRectangle scissor) {
			this(pose, x, y, width, height, radius, color, scissor, false);
		}

		@Override
		public void buildVertices(VertexConsumer vertexConsumer) {
			int right = x + width;
			int bottom = y + height;
			int r = radius;
			quad(vertexConsumer, x + r, y + r, right - r, bottom - r);
			quad(vertexConsumer, x + r, y, right - r, y + r);
			quad(vertexConsumer, x + r, bottom - r, right - r, bottom);
			quad(vertexConsumer, x, y + r, x + r, bottom - r);
			quad(vertexConsumer, right - r, y + r, right, bottom - r);
			corner(vertexConsumer, x + r, y + r, Math.PI, 1.5 * Math.PI);
			corner(vertexConsumer, right - r, y + r, 1.5 * Math.PI, 2.0 * Math.PI);
			corner(vertexConsumer, right - r, bottom - r, 0.0, 0.5 * Math.PI);
			corner(vertexConsumer, x + r, bottom - r, 0.5 * Math.PI, Math.PI);
		}

		private void quad(VertexConsumer vertexConsumer, int x0, int y0, int x1, int y1) {
			vertex(vertexConsumer, x0, y0);
			vertex(vertexConsumer, x0, y1);
			vertex(vertexConsumer, x1, y1);
			vertex(vertexConsumer, x1, y0);
		}

		private void corner(VertexConsumer vertexConsumer, int cx, int cy, double from, double to) {
			double step = (to - from) / CORNER_STEPS;
			for (int i = 0; i < CORNER_STEPS; i++) {
				double a0 = from + step * i;
				double a1 = from + step * (i + 1);
				vertex(vertexConsumer, cx, cy);
				vertex(vertexConsumer, cx + Math.cos(a0) * radius, cy + Math.sin(a0) * radius);
				vertex(vertexConsumer, cx + Math.cos(a1) * radius, cy + Math.sin(a1) * radius);
				vertex(vertexConsumer, cx, cy);
			}
		}

		private void vertex(VertexConsumer vertexConsumer, double px, double py) {
			int tint = color;
			if (fade) {
				float down = Math.min(1.0F, Math.max(0.0F, (float) (py - y) / height));
				tint = ARGB.color(Math.round(((color >>> 24) & 255) * (1.0F - down)), color);
			}
			vertexConsumer.addVertexWith2DPose(pose, (float) px, (float) py).setColor(tint);
		}

		@Override
		public RenderPipeline pipeline() {
			return RenderPipelines.GUI;
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}

		@Override
		public ScreenRectangle scissorArea() {
			return scissor;
		}

		@Override
		public ScreenRectangle bounds() {
			ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
			return scissor != null ? scissor.intersection(bounds) : bounds;
		}
	}
}
