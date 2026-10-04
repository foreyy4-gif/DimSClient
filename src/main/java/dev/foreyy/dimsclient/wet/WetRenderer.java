package dev.foreyy.dimsclient.wet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Water texture over every exposed face, darkened so the block reads as soaked,
 * with a sun glint that slides across the surface. The glint is baked into the
 * vertex colour, so it shows up with or without a shader pack.
 */
public final class WetRenderer {
	private static final int TINT = 0x3F76A0;
	private static final float ALPHA = 0.55F;
	private static final RenderType SURFACE = RenderType.create(
		"dims_wet",
		RenderSetup.builder(net.minecraft.client.renderer.RenderPipelines.TRANSLUCENT_BLOCK)
			.withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
			.useLightmap()
			.useOverlay()
			.sortOnUpload()
			.createRenderSetup());

	private WetRenderer() {
	}

	public static void render(LevelRenderContext context) {
		List<Wet.Face> faces = Wet.faces();
		if (faces.isEmpty()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		float time = context.levelState().gameTime;
		Vector3f sun = sunDirection(context.levelState().skyRenderState.sunAngle);
		TextureAtlasSprite water = client.getModelManager().getFluidStateModelSet()
			.get(Fluids.WATER.defaultFluidState()).stillMaterial().sprite();
		CardinalLighting shade = client.level == null ? CardinalLighting.DEFAULT : client.level.cardinalLighting();
		context.submitNodeCollector().submitCustomGeometry(context.poseStack(), SURFACE, (pose, buffer) -> {
			for (Wet.Face face : faces) {
				sheet(buffer, pose, face, camera, water, shade, sun, time, light(client, face));
			}
		});
	}

	private static void sheet(VertexConsumer buffer, PoseStack.Pose pose, Wet.Face face, Vec3 camera, TextureAtlasSprite water,
			CardinalLighting shade, Vector3f sun, float time, int light) {
		Direction direction = face.direction();
		float nx = direction.getStepX();
		float ny = direction.getStepY();
		float nz = direction.getStepZ();
		float darkened = shade.byFace(direction) * 0.62F;
		float x0 = (float) (face.x() + face.x0() - camera.x);
		float y0 = (float) (face.y() + face.y0() - camera.y);
		float z0 = (float) (face.z() + face.z0() - camera.z);
		float x1 = (float) (face.x() + face.x1() - camera.x);
		float y1 = (float) (face.y() + face.y1() - camera.y);
		float z1 = (float) (face.z() + face.z1() - camera.z);
		float u0 = water.getU0();
		float u1 = water.getU1();
		float v0 = water.getV0();
		float v1 = water.getV1();
		corner(buffer, pose, x0, y0, z0, u0, v0, nx, ny, nz, darkened, sun, time, light, 0.0F, 0.0F);
		corner(buffer, pose, x0, y1, z0, u0, v1, nx, ny, nz, darkened, sun, time, light, 0.0F, 1.0F);
		corner(buffer, pose, x1, y1, z1, u1, v1, nx, ny, nz, darkened, sun, time, light, 1.0F, 1.0F);
		corner(buffer, pose, x1, y0, z1, u1, v0, nx, ny, nz, darkened, sun, time, light, 1.0F, 0.0F);
	}

	private static void corner(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
			float nx, float ny, float nz, float darkened, Vector3f sun, float time, int light, float across, float up) {
		int red = clamp((TINT >> 16 & 0xFF) * darkened);
		int green = clamp((TINT >> 8 & 0xFF) * darkened);
		int blue = clamp((TINT & 0xFF) * darkened);
		int color = (int) (ALPHA * 255.0F) << 24 | red << 16 | green << 8 | blue;
		float tilt = glint(across, up, nx, ny, nz, sun, time);
		float nnx = nx + (1.0F - nx * nx) * tilt;
		float nny = ny + (1.0F - ny * ny) * tilt;
		float nnz = nz;
		float length = Mth.sqrt(nnx * nnx + nny * nny + nnz * nnz);
		buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
			.setNormal(nnx / length, nny / length, nnz / length);
	}

	/** Ripple in the normal, so a shader pack's own specular picks it up as wet. */
	private static float glint(float across, float up, float nx, float ny, float nz, Vector3f sun, float time) {
		float facing = Math.abs(nx * sun.x + ny * sun.y + nz * sun.z);
		float wave = Mth.sin((across * 6.0F + time * 0.05F)) * Mth.cos(up * 5.0F + time * 0.04F);
		return wave * (0.15F + 0.35F * facing);
	}

	/** Same angle the sky uses, so the glint tracks the sun instead of the clock. */
	private static Vector3f sunDirection(float sunAngle) {
		float yaw = (sunAngle - 0.25F) * Mth.TWO_PI;
		return new Vector3f(-Mth.sin(yaw), Mth.cos(yaw), 0.15F).normalize();
	}

	private static int light(Minecraft client, Wet.Face face) {
		if (client.level == null) {
			return LightCoordsUtil.FULL_BRIGHT;
		}
		Direction direction = face.direction();
		return LightCoordsUtil.getLightCoords(client.level,
			BlockPos(face).relative(direction));
	}

	private static net.minecraft.core.BlockPos BlockPos(Wet.Face face) {
		return new net.minecraft.core.BlockPos(face.x(), face.y(), face.z());
	}

	private static int clamp(float channel) {
		return (int) Mth.clamp(channel, 0.0F, 255.0F);
	}
}
