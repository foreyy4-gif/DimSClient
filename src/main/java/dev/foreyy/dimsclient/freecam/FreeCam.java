package dev.foreyy.dimsclient.freecam;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/** A camera that flies through everything while the player stays exactly where they were. */
public final class FreeCam {
	private static final double SPEED = 0.85;
	private static final double FAST = 2.5;
	/** Far below anything the server syncs, so a real entity can never take this id. */
	private static final int CAMERA_ID = -2_000_000_000;

	private static boolean active;
	private static Marker camera;
	private static Vec3 anchor;
	private static float anchorYRot;
	private static float anchorXRot;

	private FreeCam() {
	}

	public static void tick(Minecraft client) {
		while (DimsClient.FREE_CAM.consumeClick()) {
			active = !active;
			if (!active) {
				stop(client);
			}
		}
		if (!active || !DimsClient.CONFIG.freeCamEnabled) {
			if (camera != null) {
				stop(client);
			}
			active = false;
			return;
		}
		LocalPlayer self = client.player;
		if (camera == null || camera.level() != client.level) {
			start(client);
		}
		self.setPos(anchor.x, anchor.y, anchor.z);
		self.setDeltaMovement(Vec3.ZERO);
		self.setYRot(anchorYRot);
		self.setXRot(anchorXRot);
		fly(client);
	}

	private static void start(Minecraft client) {
		LocalPlayer self = client.player;
		anchor = self.position();
		anchorYRot = self.getYRot();
		anchorXRot = self.getXRot();
		camera = new Marker(EntityTypes.MARKER, client.level);
		camera.setId(CAMERA_ID);
		camera.noPhysics = true;
		camera.setPos(anchor.x, anchor.y + self.getEyeHeight(), anchor.z);
		camera.setYRot(anchorYRot);
		camera.setXRot(anchorXRot);
		client.level.addEntity(camera);
		client.setCameraEntity(camera);
	}

	private static void fly(Minecraft client) {
		var options = client.options;
		float yaw = camera.getYRot() * (float) (Math.PI / 180.0);
		double forward = (options.keyUp.isDown() ? 1 : 0) - (options.keyDown.isDown() ? 1 : 0);
		double strafe = (options.keyLeft.isDown() ? 1 : 0) - (options.keyRight.isDown() ? 1 : 0);
		double vertical = (options.keyJump.isDown() ? 1 : 0) - (options.keyShift.isDown() ? 1 : 0);
		double speed = options.keySprint.isDown() ? SPEED * FAST : SPEED;
		double length = Math.sqrt(forward * forward + strafe * strafe);
		double x = 0.0;
		double z = 0.0;
		if (length > 1.0E-4) {
			forward /= length;
			strafe /= length;
			x = (-Mth.sin(yaw) * forward + Mth.cos(yaw) * strafe) * speed;
			z = (Mth.cos(yaw) * forward + Mth.sin(yaw) * strafe) * speed;
		}
		camera.setPos(camera.getX() + x, camera.getY() + vertical * speed, camera.getZ() + z);
		camera.setOldPosAndRot();
	}

	/** Mouse look goes to the camera, never to the player. */
	public static void turn(double xo, double yo) {
		camera.turn(xo, yo);
	}

	public static boolean flying() {
		return active && camera != null && DimsClient.CONFIG.freeCamEnabled;
	}

	/** Where the camera is, which is where the eyes are while it flies. */
	public static Vec3 position() {
		return camera.position();
	}

	/** The direction the camera looks, not the player. */
	public static Vec3 look() {
		return camera.getLookAngle();
	}

	public static void stop(Minecraft client) {
		if (client.getCameraEntity() == camera) {
			client.setCameraEntity(client.player);
		}
		// The body walks to where the camera was, one block a tick, which is what the server accepts.
		if (DimsClient.CONFIG.freeCamTeleport && camera != null && client.player != null) {
			dev.foreyy.dimsclient.teleport.Teleport.to(client.player, camera.position().subtract(0.0, client.player.getEyeHeight(), 0.0));
		}
		if (camera != null && client.level != null) {
			client.level.removeEntity(camera.getId(), Entity.RemovalReason.DISCARDED);
		}
		camera = null;
		active = false;
	}
}
