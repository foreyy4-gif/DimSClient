package dev.foreyy.dimsclient;

import dev.foreyy.dimsclient.air.AirStuck;
import dev.foreyy.dimsclient.cart.ArrowTracker;
import dev.foreyy.dimsclient.dash.SpearDash;
import dev.foreyy.dimsclient.elytra.ElytraTarget;
import dev.foreyy.dimsclient.freecam.FreeCam;
import dev.foreyy.dimsclient.glow.BlockGlow;
import dev.foreyy.dimsclient.mine.AutoMine;
import dev.foreyy.dimsclient.search.AutoSearch;
import dev.foreyy.dimsclient.seed.SeedCracker;
import dev.foreyy.dimsclient.speed.Speed;
import dev.foreyy.dimsclient.stun.StunSlam;
import dev.foreyy.dimsclient.teleport.Teleport;
import dev.foreyy.dimsclient.wet.Wet;
import dev.foreyy.dimsclient.wet.WetRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class DimsClient implements ClientModInitializer {
	public static final DimsConfig CONFIG = new DimsConfig();
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dimsclient", "dimsclient"));
	public static final KeyMapping OPEN = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.open", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
		public static final KeyMapping MOVE_HUD = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.dimsclient.movehud", GLFW.GLFW_KEY_V, CATEGORY));
	public static final KeyMapping DASH = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.dash", GLFW.GLFW_KEY_R, CATEGORY));
	public static final KeyMapping AIR_HOLD = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.airhold", GLFW.GLFW_KEY_G, CATEGORY));
	public static final KeyMapping AIR_DROP = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.airdrop", GLFW.GLFW_KEY_H, CATEGORY));
	public static final KeyMapping AIR_UP = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.airup", GLFW.GLFW_KEY_J, CATEGORY));
	public static final KeyMapping FREE_CAM = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.freecam", GLFW.GLFW_KEY_B, CATEGORY));
	public static final KeyMapping TELEPORT = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.teleport", GLFW.GLFW_KEY_Y, CATEGORY));
	public static final KeyMapping CHASE = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.chase", GLFW.GLFW_KEY_H, CATEGORY));
	public static final KeyMapping XBOW = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.xbow", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
	public static final KeyMapping HIDE = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.dimsclient.hide", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
	@Override
	public void onInitializeClient() {
		CONFIG.load();
		dev.foreyy.dimsclient.bind.Binds.load();
		ClientTickEvents.END_CLIENT_TICK.register(DimsClient::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES,
			Identifier.fromNamespaceAndPath("dimsclient", "overlay"), (graphics, tickCounter) -> DimsHud.render(graphics));
		LevelRenderEvents.BEFORE_TRANSLUCENT_TERRAIN.register(WetRenderer::render);
	}

	private static void tick(Minecraft client) {
		if (client.player != null && client.level != null && CONFIG.chestEnabled && client.gui.screen() == null) {
			dev.foreyy.dimsclient.chest.ChestReach.tick(client);
		}
		// The carried slot is sent to the server at the start of the tick, so the crossbow
		// sequence has to pick its slot before that or the server uses whatever was held.
		if (client.player != null && client.level != null && CONFIG.xbowEnabled && client.gui.screen() == null) {
			dev.foreyy.dimsclient.xbow.AutoXBow.tick(client);
		}
		dev.foreyy.dimsclient.hide.Hide.tick(client);
		if (dev.foreyy.dimsclient.hide.Hide.hidden()) {
			return;
		}
		while (OPEN.consumeClick()) {
			if (client.gui.screen() == null) {
				AutoMine.rememberLookedAt(client);
				client.setScreenAndShow(new DimsScreen(null));
			}
		}
		if (client.player == null || client.level == null || client.gui.screen() != null) {
			if (!CONFIG.stunEnabled) {
				StunSlam.reset();
			}
			if (client.player == null || client.level == null || !CONFIG.cartEnabled) {
				ArrowTracker.clear();
			}
			if (client.gui.screen() != null) {
				StunSlam.reset();
				releaseCursor(client);
				return;
			}
		}

		if (CONFIG.stunEnabled) {
			StunSlam.tick(client);
		} else {
			StunSlam.reset();
		}

		if (client.player != null && client.level != null && CONFIG.cartEnabled) {
			ArrowTracker.tick(client);
		} else {
			ArrowTracker.clear();
		}

		if (client.player != null && client.level != null && CONFIG.dashEnabled && client.gui.screen() == null) {
			SpearDash.tick(client);
		}

		if (client.player != null && client.level != null && CONFIG.glowEnabled) {
			BlockGlow.tick(client);
		}


		if (client.player != null) {
			Speed.tick(client.player);
		}

		if (client.player != null && client.level != null && CONFIG.seedEnabled) {
			SeedCracker.tick(client);
		}

		if (client.player != null && client.level != null) {
			AutoSearch.tick(client);
		}

		if (client.player != null && client.level != null && CONFIG.elytraEnabled && client.gui.screen() == null) {
			ElytraTarget.tick(client);
		}

		if (client.player != null && CONFIG.airEnabled && client.gui.screen() == null) {
			AirStuck.tick(client.player);
		}

		if (client.player != null && client.level != null && CONFIG.freeCamEnabled && client.gui.screen() == null) {
			FreeCam.tick(client);
		} else if (client.player != null) {
			FreeCam.stop(client);
		}

		if (client.player != null && CONFIG.teleportEnabled && client.gui.screen() == null) {
			Teleport.tick(client.player);
		}

		if (client.player != null && client.level != null) {
			Wet.tick(client);
		}

		if (client.player != null) {
			dev.foreyy.dimsclient.freecam.FreeF5.tick(client);
		}

		if (client.player != null && client.gui.screen() == null) {
			dev.foreyy.dimsclient.fly.VanillaFly.tick(client.player);
		}

		if (client.player != null && client.gui.screen() == null) {
			dev.foreyy.dimsclient.totem.AutoTotem.tick(client);
		}

		if (client.player != null && client.level != null && CONFIG.baritoneEnabled && client.gui.screen() == null) {
			dev.foreyy.dimsclient.baritone.Baritone.tick(client);
		}

		if (client.player != null && client.level != null && CONFIG.mineEnabled && client.gui.screen() == null) {
			AutoMine.tick(client);
		} else if (client.gameMode != null) {
			AutoMine.stop(client.gameMode);
		}

		releaseCursor(client);
	}

	/** Free the cursor while the HUD move key is held, and grab it again on release. */
	private static void releaseCursor(Minecraft client) {
		boolean editing = MOVE_HUD.isDown() && client.gui.screen() == null && client.player != null;
		if (editing) {
			if (client.mouseHandler.isMouseGrabbed()) {
				client.mouseHandler.releaseMouse();
			}
		} else if (!client.mouseHandler.isMouseGrabbed() && client.gui.screen() == null && client.isWindowActive()
			&& org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().handle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) {
			client.mouseHandler.grabMouse();
		}
	}

	public static void toast(Minecraft client, String text) {
		if (client.player != null) {
			client.gui.hud.setOverlayMessage(Component.literal(text), true);
		}
	}
}
