package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The grown box has to be emitted with the rest of the F3+B gizmos, or it is collected too late. */
@Mixin(EntityHitboxDebugRenderer.class)
public class HitboxDebugMixin {
	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void dimsclient$drawGrownBoxes(double camX, double camY, double camZ, net.minecraft.util.debug.DebugValueAccess debugValues,
			net.minecraft.client.renderer.culling.Frustum frustum, float partialTicks, CallbackInfo callback) {
		if (!DimsClient.CONFIG.hitboxEnabled || !DimsClient.CONFIG.hitboxVisible || DimsClient.CONFIG.hitboxExpand <= 0.0F) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity) || entity.isInvisible() || entity == client.player) {
				continue;
			}
			float tick = client.getDeltaTracker().getGameTimeDeltaPartialTick(!client.level.tickRateManager().isEntityFrozen(entity));
			Vec3 offset = entity.getPosition(tick).subtract(entity.position());
			AABB grown = entity.getBoundingBox().inflate(DimsClient.CONFIG.hitboxExpand).move(offset);
			Gizmos.cuboid(grown, GizmoStyle.stroke(0xFF55DDFF));
		}
	}
}
