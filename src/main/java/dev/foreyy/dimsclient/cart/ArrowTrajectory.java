package dev.foreyy.dimsclient.cart;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ArrowTrajectory {
    public static final int HORIZON_TICKS = 100;
    public static final int PLACEMENT_LEAD_TICKS = 5;

    private ArrowTrajectory() {}

    public static Optional<BlockPos> impactInTicks(Level level, AbstractArrow arrow, int ticksAhead) {
        if (ticksAhead < 1 || ticksAhead > HORIZON_TICKS || arrow.getDeltaMovement().lengthSqr() < 1.0E-8 || arrow.isNoPhysics()) {
            return Optional.empty();
        }

        Vec3 position = arrow.position();
        Vec3 velocity = arrow.getDeltaMovement();

        for (int tick = 1; tick <= ticksAhead; tick++) {
            Vec3 next = position.add(velocity);
            BlockHitResult hit = level.clipIncludingBorder(new ClipContext(
                    position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
            if (hit.getType() != HitResult.Type.MISS) {
                return tick == ticksAhead ? Optional.of(hit.getBlockPos()) : Optional.empty();
            }
            position = next;
            velocity = velocity.scale(0.99).add(0.0, -0.05, 0.0);
        }
        return Optional.empty();
    }
}
