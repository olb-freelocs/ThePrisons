package com.freelocs.theprisons.cache;

import com.freelocs.theprisons.ThePrisonsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThePrisonsCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_PATH = FabricLoader.getInstance().getConfigDir().resolve("theprisons-cache.json");
    private static final Path LEGACY_CACHE_PATH = FabricLoader.getInstance().getConfigDir().resolve("theprisons-cache.json");

    private final Map<String, ThePrisonsEntry> entries = new LinkedHashMap<>();

    public Map<String, ThePrisonsEntry> entries() {
        return entries;
    }

    public ThePrisonsEntry getOrCreate(String key) {
        return entries.computeIfAbsent(key, ThePrisonsEntry::new);
    }

    public void load() {
        try {
            Path sourcePath = Files.exists(CACHE_PATH) ? CACHE_PATH : LEGACY_CACHE_PATH;
            if (!Files.exists(sourcePath)) {
                save();
                return;
            }

            ThePrisonsStore store = GSON.fromJson(Files.readString(sourcePath), ThePrisonsStore.class);
            entries.clear();
            if (store != null && store.entries != null) {
                for (ThePrisonsEntry entry : store.entries.values()) {
                    if (entry != null && entry.key != null && !entry.key.isBlank()) {
                        if (entry.displayName != null) {
                            entry.displayName = stripLevel(entry.displayName);
                        }
                        if (entry.source == null || entry.source.isBlank()) {
                            entry.source = "PET";
                        }
                        entries.put(entry.key, entry);
                    }
                }
            }
        } catch (Exception exception) {
            ThePrisonsClient.LOGGER.warn("Failed to load cooldown cache", exception);
            entries.clear();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CACHE_PATH.getParent());
            Files.writeString(CACHE_PATH, GSON.toJson(new ThePrisonsStore(entries)));
        } catch (IOException exception) {
            ThePrisonsClient.LOGGER.warn("Failed to save cooldown cache", exception);
        }
    }

    public static final class ThePrisonsStore {
        Map<String, ThePrisonsEntry> entries = new LinkedHashMap<>();

        public ThePrisonsStore() {
        }

        public ThePrisonsStore(Map<String, ThePrisonsEntry> entries) {
            this.entries = entries;
        }
    }

    public static final class ThePrisonsEntry {
        public String key;
        public String source = "PET";
        public String displayName;
        public String itemId;
        public long cooldownEndsAtMs;
        public long lastSeenAtMs;
        public boolean readyAnnounced;

        public ThePrisonsEntry() {
        }

        public ThePrisonsEntry(String key) {
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
