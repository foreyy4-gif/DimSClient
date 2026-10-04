package dev.foreyy.dimsclient.cart;

import dev.foreyy.dimsclient.DimsClient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArrowTracker {
    private static final int MAX_AGE_TICKS = 100;
    private static final Map<UUID, TrackedArrow> ARROWS = new HashMap<>();
    private static final Set<UUID> COMPLETED = new HashSet<>();
    private static final Map<UUID, Placement> PLACEMENTS = new HashMap<>();
    private static float yawVelocity;
    private static float pitchVelocity;
    private static Object trackedLevel;

    private ArrowTracker() {}

    public static void clear() {
        ARROWS.clear();
        COMPLETED.clear();
        PLACEMENTS.clear();
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        trackedLevel = null;
    }

    public static void tick(Minecraft client) {
        if (trackedLevel != client.level) {
            ARROWS.clear();
            COMPLETED.clear();
            PLACEMENTS.clear();
            yawVelocity = 0.0F;
            pitchVelocity = 0.0F;
            trackedLevel = client.level;
        }

        long now = client.level.getGameTime();
        Iterator<Map.Entry<UUID, Placement>> placementIterator = PLACEMENTS.entrySet().iterator();
        while (placementIterator.hasNext()) {
            Map.Entry<UUID, Placement> entry = placementIterator.next();
            Placement placement = entry.getValue();
            if (now > placement.deadline || playerDistanceTooFar(client, placement.railPos)) {
                placementIterator.remove();
                continue;
            }
            if (placement.stage == PlacementStage.RAIL) {
                if (!client.level.getBlockState(placement.railPos).is(Blocks.RAIL)) {
                    if (!client.level.getBlockState(placement.railPos).canBeReplaced()) {
                        placementIterator.remove();
                    }
                    continue;
                }
                var cartHit = DimsClient.CONFIG.cartLegit
                        ? aimedNear(client, placement.railPos)
                        : new BlockHitResult(Vec3.atCenterOf(placement.railPos), Direction.UP,
                        placement.railPos, false);
                if (cartHit == null) continue;
                int cartSlot = findHotbarSlot(client.player, Items.TNT_MINECART);
                if (cartSlot < 0) {
                    placementIterator.remove();
                    continue;
                }
                boolean placed = useItemInSlot(client, cartSlot, () -> client.gameMode.useItemOn(client.player,
                        InteractionHand.MAIN_HAND, cartHit));
                if (!placed) continue;
                placement.stage = PlacementStage.CART;
            } else if (client.level.getEntitiesOfClass(AbstractMinecart.class,
                    new AABB(placement.railPos)).isEmpty()) {
                placementIterator.remove();
            }
        }

        for (Entity renderedEntity : client.level.entitiesForRendering()) {
            if (renderedEntity instanceof AbstractArrow arrow && arrow.getOwner() == client.player
                    && !arrow.isNoPhysics()) {
                ARROWS.putIfAbsent(arrow.getUUID(), new TrackedArrow(now));
            }
        }

        boolean aiming = false;
        Iterator<Map.Entry<UUID, TrackedArrow>> iterator = ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedArrow> entry = iterator.next();
            Entity entity = client.level.getEntity(entry.getKey());
            if (!(entity instanceof AbstractArrow arrow) || arrow.isRemoved()
                    || now - entry.getValue().firstSeen > MAX_AGE_TICKS || arrow.isNoPhysics()) {
                iterator.remove();
                continue;
            }

            var predicted = ArrowTrajectory.impactInTicks(client.level, arrow, DimsClient.CONFIG.cartLeadTicks);
            if (predicted.isPresent()) {
                BlockPos impact = predicted.get();
                if (!aiming && DimsClient.CONFIG.cartSmoothAim) {
                    smoothAim(client, Vec3.atCenterOf(impact.above()));
                    aiming = true;
                }
                if (!COMPLETED.contains(arrow.getUUID()) && placeCart(client, arrow, impact)) {
                    COMPLETED.add(arrow.getUUID());
                }
            }
        }
    }

    private static void smoothAim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float targetYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Mth.atan2(delta.y, horizontal) * (180.0 / Math.PI));
        float yawError = Mth.wrapDegrees(targetYaw - client.player.getYRot());
        float pitchError = targetPitch - client.player.getXRot();
        float responsiveness = 0.10F + DimsClient.CONFIG.cartAimSpeed * 0.012F;
        float acceleration = 0.35F + DimsClient.CONFIG.cartAimSpeed * 0.035F;
        float maxSpeed = 1.0F + DimsClient.CONFIG.cartAimSpeed * 0.18F;

        yawVelocity = Mth.clamp(yawVelocity + Mth.clamp(yawError * responsiveness - yawVelocity * 0.35F,
                -acceleration, acceleration), -maxSpeed, maxSpeed);
        pitchVelocity = Mth.clamp(pitchVelocity + Mth.clamp(pitchError * responsiveness - pitchVelocity * 0.35F,
                -acceleration, acceleration), -maxSpeed, maxSpeed);
        if (Math.abs(yawError) < 0.15F) yawVelocity = 0.0F;
        if (Math.abs(pitchError) < 0.15F) pitchVelocity = 0.0F;

        client.player.setYRot(client.player.getYRot() + yawVelocity);
        client.player.setXRot(Mth.clamp(client.player.getXRot() + pitchVelocity, -90.0F, 90.0F));
    }

    private static boolean placeCart(Minecraft client, AbstractArrow arrow, BlockPos impact) {
        var player = client.player;
        var level = client.level;
        var gameMode = client.gameMode;
        if (player == null || level == null || gameMode == null) return false;
        if (!gameMode.getPlayerMode().isSurvival() && !gameMode.getPlayerMode().isCreative()) return false;

        BlockPos railPos = impact.above();
        BlockPos supportPos = railPos.below();
        if (!level.hasChunkAt(railPos) || !level.getBlockState(railPos).canBeReplaced()
                || !level.getBlockState(supportPos).isSolidRender()) return false;
        if (player.distanceToSqr(Vec3.atCenterOf(railPos)) > DimsClient.CONFIG.cartRange * DimsClient.CONFIG.cartRange) return false;
        int railSlot = findHotbarSlot(player, Items.RAIL);
        if (railSlot < 0 || findHotbarSlot(player, Items.TNT_MINECART) < 0) return false;

        BlockHitResult railHit = DimsClient.CONFIG.cartLegit
                ? aimedNear(client, supportPos)
                : new BlockHitResult(Vec3.atCenterOf(supportPos).add(0.0, 0.5, 0.0),
                Direction.UP, supportPos, false);
        if (railHit == null) return false;
        boolean placed = useItemInSlot(client, railSlot, () -> gameMode.useItemOn(player, InteractionHand.MAIN_HAND, railHit));
        if (!placed) return false;
        // Dedicated servers don't apply the client's block prediction, so the rail
        // would otherwise never show up before the cart tick. Show it locally; the
        // server's own update replaces this state.
        showRailLocally(client, player, railHit, railSlot, railPos);
        PLACEMENTS.put(arrow.getUUID(), new Placement(railPos, level.getGameTime() + 10));
        return true;
    }

    private static void showRailLocally(Minecraft client, net.minecraft.client.player.LocalPlayer player,
                                        BlockHitResult hit, int slot, BlockPos railPos) {
        if (client.level.getBlockState(railPos).is(Blocks.RAIL)) return;
        ItemStack stack = player.getInventory().getItem(slot);
        BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
        BlockState state = Blocks.RAIL.getStateForPlacement(context);
        if (state == null) state = Blocks.RAIL.defaultBlockState();
        client.level.setBlock(railPos, state, 11);
    }

    private static boolean useItemInSlot(Minecraft client, int slot, java.util.function.Supplier<InteractionResult> useItem) {
        int oldSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(slot);
        try {
            InteractionResult result = useItem.get();
            return result != null && result.consumesAction();
        } finally {
            if (!DimsClient.CONFIG.cartKeepSlot) {
                client.player.getInventory().setSelectedSlot(oldSlot);
            }
        }
    }

    private static BlockHitResult aimedNear(Minecraft client, BlockPos target) {
        BlockHitResult hit = crosshairHit(client);
        if (hit == null) return null;
        BlockPos aimed = hit.getBlockPos();
        if (Math.abs(aimed.getX() - target.getX()) > 1 || Math.abs(aimed.getY() - target.getY()) > 1
                || Math.abs(aimed.getZ() - target.getZ()) > 1) {
            return null;
        }
        return new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
    }

    private static BlockHitResult crosshairHit(Minecraft client) {
        Vec3 eye = client.player.getEyePosition();
        float yaw = (float) Math.toRadians(client.player.getYRot());
        float pitch = (float) Math.toRadians(client.player.getXRot());
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        var hit = client.level.clip(new ClipContext(eye,
                eye.add(look.scale(DimsClient.CONFIG.cartRange)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, client.player));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return null;
        return hit;
    }

    private static boolean playerDistanceTooFar(Minecraft client, BlockPos pos) {
        return client.player.distanceToSqr(Vec3.atCenterOf(pos))
                > DimsClient.CONFIG.cartRange * DimsClient.CONFIG.cartRange;
    }

    private static int findHotbarSlot(net.minecraft.client.player.LocalPlayer player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(item)) return slot;
        }
        return -1;
    }

    private enum PlacementStage { RAIL, CART }

    private static final class Placement {
        private final BlockPos railPos;
        private final long deadline;
        private PlacementStage stage = PlacementStage.RAIL;

        private Placement(BlockPos railPos, long deadline) {
            this.railPos = railPos;
            this.deadline = deadline;
        }
    }

    private record TrackedArrow(long firstSeen) {}
}
