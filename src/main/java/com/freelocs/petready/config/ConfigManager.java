package com.freelocs.petready.config;

import com.freelocs.petready.PetReadyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("petready.json");

    private PetReadyConfig config = new PetReadyConfig();

    public PetReadyConfig get() {
        return config;
    }

    public void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }

            PetReadyConfig loaded = GSON.fromJson(Files.readString(CONFIG_PATH), PetReadyConfig.class);
            config = loaded == null ? new PetReadyConfig() : loaded;
            config.normalize();
        } catch (Exception exception) {
            PetReadyClient.LOGGER.warn("Failed to load config", exception);
            config = new PetReadyConfig();
        }
    }

    public void save() {
        try {
            config.normalize();
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException exception) {
            PetReadyClient.LOGGER.warn("Failed to save config", exception);
        }
    }
}
