package dev.foreyy.dimsclient.xbow;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * On its key: rails three blocks ahead along the look, a TNT minecart on them, then a crossbow shot.
 * With the fire option on, a flint and steel lights the block in front of the rail first.
 */
public final class AutoXBow {
	private static final int AHEAD = 3;

	private enum Step {
		IDLE, RAIL, CART, FIRE, SHOOT
	}

	private static Step step = Step.IDLE;
	/** Tick the current step was entered, so a step the server rejects cannot spin forever. */
	private static long since;
	/** The server only hears about a slot change next tick, so the use waits one tick. */
	private static int heldSlot = -1;

	private AutoXBow() {
	}

	public static void tick(Minecraft client) {
		while (DimsClient.XBOW.consumeClick()) {
			if (step == Step.IDLE) {
				step = Step.RAIL;
				since = client.level.getGameTime();
			} else {
				step = Step.IDLE;
				heldSlot = -1;
			}
		}
		if (step == Step.IDLE || client.player == null || client.level == null || client.gameMode == null) {
			return;
		}
		if (client.level.getGameTime() - since > 60) {
			step = Step.IDLE;
			heldSlot = -1;
			return;
		}
		switch (step) {
			case RAIL -> {
				if (placeRail(client)) {
					advance(client, Step.CART);
				}
			}
			case CART -> {
				if (placeCart(client)) {
					advance(client, DimsClient.CONFIG.xbowFire ? Step.FIRE : Step.SHOOT);
				}
			}
			case FIRE -> {
				if (light(client)) {
					advance(client, Step.SHOOT);
				}
			}
			case SHOOT -> {
				if (shoot(client)) {
					step = Step.IDLE;
				}
			}
			case IDLE -> {
			}
		}
	}

	private static void advance(Minecraft client, Step next) {
		step = next;
		since = client.level.getGameTime();
	}

	/** The block three ahead, floored to where the player stands so the rail has ground under it. */
	private static BlockPos railPos(LocalPlayer player) {
		Vec3 look = player.getLookAngle();
		double flat = Math.sqrt(look.x * look.x + look.z * look.z);
		double x = player.getX() + (flat < 1.0E-4 ? 0.0 : look.x / flat) * AHEAD;
		double z = player.getZ() + (flat < 1.0E-4 ? 0.0 : look.z / flat) * AHEAD;
		return BlockPos.containing(x, Math.floor(player.getY()), z);
	}

	private static boolean placeRail(Minecraft client) {
		LocalPlayer player = client.player;
		BlockPos rail = railPos(player);
		BlockPos support = rail.below();
		if (!client.level.getBlockState(support).isFaceSturdy(client.level, support, Direction.UP) || !client.level.getBlockState(rail).canBeReplaced()) {
			say(client, "нет опоры под рельсами");
			return false;
		}
		int slot = slot(player, Items.RAIL);
		if (slot < 0) {
			say(client, "нет рельсов на хотбаре");
			return false;
		}
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.99, 0.0), Direction.UP, support, false);
		if (!use(client, slot, () -> client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit))) {
			return false;
		}
		showRail(client, player, hit, slot, rail);
		return true;
	}

	/** The server answers next tick, so the rail is shown locally in the meantime. */
	private static void showRail(Minecraft client, LocalPlayer player, BlockHitResult hit, int slot, BlockPos rail) {
		ItemStack stack = player.getInventory().getItem(slot);
		BlockPlaceContext context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
		BlockState state = Blocks.RAIL.getStateForPlacement(context);
		client.level.setBlock(rail, state == null ? Blocks.RAIL.defaultBlockState() : state, 11);
	}

	private static boolean placeCart(Minecraft client) {
		LocalPlayer player = client.player;
		BlockPos rail = railPos(player);
		if (!client.level.getBlockState(rail).is(Blocks.RAIL)) {
			return false;
		}
		int slot = slot(player, Items.TNT_MINECART);
		if (slot < 0) {
			say(client, "нет TNT-вагонетки на хотбаре");
			return false;
		}
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(rail), Direction.UP, rail, false);
		return use(client, slot, () -> client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit));
	}

	/** Lights the block one closer than the rail, so the fire sits in front of it. */
	private static boolean light(Minecraft client) {
		LocalPlayer player = client.player;
		BlockPos rail = railPos(player);
		Vec3 look = player.getLookAngle();
		int dx = (int) Math.signum(Math.round(look.x));
		int dz = (int) Math.signum(Math.round(look.z));
		BlockPos fire = rail.offset(-dx, 0, -dz);
		if (!client.level.getBlockState(fire.below()).isFaceSturdy(client.level, fire.below(), Direction.UP) || !client.level.getBlockState(fire).canBeReplaced()) {
			return true;
		}
		int slot = slot(player, Items.FLINT_AND_STEEL);
		if (slot < 0) {
			return true;
		}
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(fire.below()).add(0.0, 0.99, 0.0), Direction.UP, fire.below(), false);
		return use(client, slot, () -> client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit));
	}

	private static boolean shoot(Minecraft client) {
		LocalPlayer player = client.player;
		int slot = crossbow(player);
		if (slot < 0) {
			say(client, "нет арбалета на хотбаре");
			return false;
		}
		if (player.getInventory().getSelectedSlot() != slot) {
			hold(player, slot);
			return false;
		}
		if (heldSlot == slot) {
			heldSlot = -1;
			return false;
		}
		ItemStack stack = player.getInventory().getItem(slot);
		if (!net.minecraft.world.item.CrossbowItem.isCharged(stack) && slot(player, Items.ARROW) < 0) {
			say(client, "нет стрел на хотбаре");
			return false;
		}
		aim(player);
		client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
		return net.minecraft.world.item.CrossbowItem.isCharged(stack);
	}

	/** Points the shot at the rail so it lands on the cart. */
	private static void aim(LocalPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 at = Vec3.atCenterOf(railPos(player));
		double dx = at.x - eye.x;
		double dy = at.y - eye.y;
		double dz = at.z - eye.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		player.setYRot((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F);
		player.setXRot((float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI))));
	}

	private static int crossbow(LocalPlayer player) {
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(Items.CROSSBOW)) {
				return slot;
			}
		}
		return -1;
	}

	private static int slot(LocalPlayer player, net.minecraft.world.item.Item item) {
		for (int i = 0; i < 9; i++) {
			if (player.getInventory().getItem(i).is(item)) {
				return i;
			}
		}
		return -1;
	}

	private static void say(Minecraft client, String text) {
		if (client.player != null) {
			client.gui.hud.setOverlayMessage(Component.literal("AutoXBow: " + text), false);
		}
	}

	/** Picks a slot and tells the server now. The normal sync only happens at the start of the next tick. */
	private static void hold(LocalPlayer player, int slot) {
		player.getInventory().setSelectedSlot(slot);
		player.connection.send(new ServerboundSetCarriedItemPacket(slot));
		heldSlot = slot;
	}

	private static boolean use(Minecraft client, int slot, java.util.function.Supplier<InteractionResult> action) {
		LocalPlayer player = client.player;
		if (player.getInventory().getSelectedSlot() != slot) {
			hold(player, slot);
			return false;
		}
		if (heldSlot == slot) {
			heldSlot = -1;
			return false;
		}
		InteractionResult result = action.get();
		return result != null && result.consumesAction();
	}
}
