package dev.foreyy.dimsclient.mixin;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Sword and mace use this margin instead of the pick radius, so the module size is added here too. */
@Mixin(ProjectileUtil.class)
public class AttackMarginMixin {
	@ModifyVariable(method = "getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static float dimsclient$growWeaponMargin(float entityMargin, Entity source) {
		if (DimsClient.CONFIG.hitboxEnabled && DimsClient.CONFIG.hitboxExpand > 0.0F) {
			return entityMargin + DimsClient.CONFIG.hitboxExpand;
		}
		return entityMargin;
	}
}
