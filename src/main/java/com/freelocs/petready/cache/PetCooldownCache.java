package com.freelocs.petready.cache;

import com.freelocs.petready.PetReadyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PetCooldownCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_PATH = FabricLoader.getInstance().getConfigDir().resolve("petready-cache.json");

    private final Map<String, PetCooldownEntry> entries = new LinkedHashMap<>();

    public Map<String, PetCooldownEntry> entries() {
        return entries;
    }

    public PetCooldownEntry getOrCreate(String key) {
        return entries.computeIfAbsent(key, PetCooldownEntry::new);
    }

    public void load() {
        try {
            if (!Files.exists(CACHE_PATH)) {
                save();
                return;
            }

            PetCooldownStore store = GSON.fromJson(Files.readString(CACHE_PATH), PetCooldownStore.class);
            entries.clear();
            if (store != null && store.entries != null) {
                for (PetCooldownEntry entry : store.entries.values()) {
                    if (entry != null && entry.key != null && !entry.key.isBlank()) {
                        if (entry.displayName != null) {
                            entry.displayName = stripLevel(entry.displayName);
                        }
                        entries.put(entry.key, entry);
                    }
                }
            }
        } catch (Exception exception) {
            PetReadyClient.LOGGER.warn("Failed to load cooldown cache", exception);
            entries.clear();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            Files.writeString(CACHE_PATH, GSON.toJson(new PetCooldownStore(entries)));
        } catch (IOException exception) {
            PetReadyClient.LOGGER.warn("Failed to save cooldown cache", exception);
        }
    }

    public static final class PetCooldownStore {
        Map<String, PetCooldownEntry> entries = new LinkedHashMap<>();

        public PetCooldownStore() {
        }

        public PetCooldownStore(Map<String, PetCooldownEntry> entries) {
            this.entries = entries;
        }
    }

    public static final class PetCooldownEntry {
        public String key;
        public String displayName;
        public String itemId;
        public long cooldownEndsAtMs;
        public long lastSeenAtMs;
        public boolean readyAnnounced;

        public PetCooldownEntry() {
        }

        public PetCooldownEntry(String key) {
            this.key = key;
        }

        public boolean isReady(long nowMs) {
            return cooldownEndsAtMs <= nowMs;
        }
    }

    private static String stripLevel(String value) {
        return value
                .replaceAll("\\s*\\[lvl\\s*\\d+\\]\\s*", " ")
                .replaceAll("\\s*lvl\\s*\\d+\\s*", " ")
                .replaceAll("\\s*level\\s*\\d+\\s*", " ")
                .trim();
    }
}
