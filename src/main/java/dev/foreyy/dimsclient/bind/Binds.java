package dev.foreyy.dimsclient.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.foreyy.dimsclient.DimsClient;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** The keys live in their own file, so a mod update never resets them. */
public final class Binds {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dimsclient-binds.json");

	private Binds() {
	}

	public static void load() {
		if (!Files.isRegularFile(FILE)) {
			save();
			return;
		}
		Map<?, ?> saved;
		try (Reader reader = Files.newBufferedReader(FILE)) {
			saved = GSON.fromJson(reader, Map.class);
		} catch (IOException | RuntimeException failure) {
			return;
		}
		if (saved == null) {
			return;
		}
		for (KeyMapping mapping : all()) {
			Object name = saved.get(mapping.getName());
			if (name instanceof String key) {
				mapping.setKey(InputConstants.getKey(key));
			}
		}
		KeyMapping.resetMapping();
	}

	public static void save() {
		Map<String, String> saved = new LinkedHashMap<>();
		for (KeyMapping mapping : all()) {
			saved.put(mapping.getName(), mapping.saveString());
		}
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(saved));
		} catch (IOException ignored) {
		}
	}

	private static KeyMapping[] all() {
		return new KeyMapping[] {DimsClient.OPEN, DimsClient.MOVE_HUD, DimsClient.DASH, DimsClient.AIR_HOLD,
			DimsClient.AIR_DROP, DimsClient.AIR_UP, DimsClient.FREE_CAM, DimsClient.TELEPORT, DimsClient.CHASE, DimsClient.XBOW,
			DimsClient.HIDE};
	}
}
