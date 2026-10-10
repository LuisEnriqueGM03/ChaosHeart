package com.chaos.heart.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.chaos.heart.ChaosHeart;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.loading.FMLPaths;

public final class HeartConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HeartConfig instance = new HeartConfig();

	public boolean showInTab = true;
	public boolean showAboveName = true;
	public String symbol = "❤";

	public static HeartConfig get() {
		return instance;
	}

	public static void load(Path configDir) {
		Path file = configDir.resolve("chaosheart.json");
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				HeartConfig loaded = GSON.fromJson(reader, HeartConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | RuntimeException e) {
				ChaosHeart.LOGGER.error("Config inválida, usando valores por defecto", e);
			}
		}
		if (instance.symbol == null || instance.symbol.isEmpty()) {
			instance.symbol = "❤";
		}
		try {
			Files.createDirectories(configDir);
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			ChaosHeart.LOGGER.error("No se pudo escribir la config", e);
		}
	}

	public static void load() {
		load(FMLPaths.CONFIGDIR.get());
	}
}
