package dev.foreyy.dimsclient.hide;

import dev.foreyy.dimsclient.DimsClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/** Moves the mod out of the mods folder and switches every feature off until the same key is pressed. */
public final class Hide {
	private static final String JAR = "dimsclient-1.0.0.jar";
	private static boolean hidden;

	private Hide() {
	}

	public static boolean hidden() {
		return hidden;
	}

	public static void tick(Minecraft client) {
		while (DimsClient.HIDE.consumeClick()) {
			hidden = !hidden;
			if (hidden) {
				stash();
				if (client.gui.screen() != null) {
					client.setScreenAndShow(null);
				}
			} else {
				restore();
			}
		}
	}

	/** The jar is moved to Documents, so the mods folder and ModMenu no longer list it. */
	private static void stash() {
		Path mods = FabricLoader.getInstance().getGameDir().resolve("mods").resolve(JAR);
		Path documents = Path.of(System.getProperty("user.home"), "Документы", JAR);
		try {
			if (Files.isRegularFile(mods)) {
				Files.createDirectories(documents.getParent());
				Files.move(mods, documents, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ignored) {
		}
	}

	private static void restore() {
		Path mods = FabricLoader.getInstance().getGameDir().resolve("mods").resolve(JAR);
		Path documents = Path.of(System.getProperty("user.home"), "Документы", JAR);
		try {
			if (Files.isRegularFile(documents)) {
				Files.move(documents, mods, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ignored) {
		}
	}
}
