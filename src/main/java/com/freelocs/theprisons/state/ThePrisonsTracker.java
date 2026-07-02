package com.freelocs.theprisons.state;

import com.freelocs.theprisons.ThePrisonsClient;
import com.freelocs.theprisons.cache.ThePrisonsCache.ThePrisonsEntry;
import com.freelocs.theprisons.config.ThePrisonsConfig;
import com.freelocs.theprisons.mixin.ThePrisonsItemCooldownInstanceAccessor;
import com.freelocs.theprisons.mixin.ThePrisonsItemCooldownsAccessor;
import com.freelocs.theprisons.ui.ThePrisonsHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ThePrisonsTracker {
    private static final String PET_TOKEN = "pet";
    private static final String SOURCE_PET = "PET";
    private static final String SOURCE_TRINKET = "TRINKET";

    public void tick(MinecraftClient client) {
        long nowMs = System.currentTimeMillis();
        boolean changed = updateReadyTransitions(nowMs);

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            if (changed) {
                ThePrisonsClient.CACHE.save();
            }
            return;
        }

        ThePrisonsConfig config = ThePrisonsClient.CONFIG.get();
        if (!config.general.persistCooldownCache) {
            return;
        }

        changed |= syncInventory(player, nowMs);
        changed |= syncTrinkets(player, nowMs);

        if (changed) {
            ThePrisonsClient.CACHE.save();
        }
    }

    private boolean syncInventory(ClientPlayerEntity player, long nowMs) {
        return syncStacks(player, player.getInventory().getMainStacks(), nowMs, SOURCE_PET, this::isPet);
    }

    private boolean syncTrinkets(ClientPlayerEntity player, long nowMs) {
        return syncStacks(player, player.getInventory().getMainStacks(), nowMs, SOURCE_TRINKET, this::isTrinket);
    }

    private boolean syncStacks(ClientPlayerEntity player, Iterable<ItemStack> stacks, long nowMs, String source, java.util.function.Predicate<String> matchesType) {
        Set<String> seen = new HashSet<>();
        boolean changed = false;
        ItemCooldownManager cooldowns = player.getItemCooldownManager();
        ThePrisonsItemCooldownsAccessor cooldownsAccessor = (ThePrisonsItemCooldownsAccessor) cooldowns;
        Map<?, ?> activeCooldowns = cooldownsAccessor.theprisons$getCooldowns();
        int tickCount = cooldownsAccessor.theprisons$getTickCount();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            String stackName = stripLevel(stack.getName().getString());
            String normalizedName = normalize(stackName);
            if (!matchesType.test(normalizedName)) {
                continue;
            }

            String key = source + ":" + normalizedName;
            if (!seen.add(key)) {
                continue;
            }

            ThePrisonsEntry entry = ThePrisonsClient.CACHE.getOrCreate(key);
            entry.key = key;
            entry.source = source;
            if (!stackName.equals(entry.displayName)) {
                entry.displayName = stackName;
                changed = true;
            }
            String itemId = getItemId(stack);
            if (!itemId.equals(entry.itemId)) {
                entry.itemId = itemId;
                changed = true;
            }
            if (entry.lastSeenAtMs == 0L || nowMs - entry.lastSeenAtMs >= 5000L) {
                entry.lastSeenAtMs = nowMs;
                changed = true;
            }

            Identifier cooldownGroup = cooldowns.getGroup(stack);
            Object instance = activeCooldowns.get(cooldownGroup);
            boolean onCooldown = cooldowns.isCoolingDown(stack) && instance != null;

            if (onCooldown) {
                int endTick = ((ThePrisonsItemCooldownInstanceAccessor) instance).theprisons$getEndTime();
                int remainingTicks = Math.max(0, endTick - tickCount);
                long cooldownEndsAtMs = nowMs + (remainingTicks * 50L);
                if (cooldownEndsAtMs > entry.cooldownEndsAtMs) {
                    entry.cooldownEndsAtMs = cooldownEndsAtMs;
                    entry.readyAnnounced = false;
                    changed = true;
                }
                continue;
            }

            if (entry.cooldownEndsAtMs <= nowMs) {
                if (!entry.readyAnnounced && ThePrisonsClient.CONFIG.get().general.showReadyAnnouncements) {
                    ThePrisonsHudRenderer.pushAnnouncement(entry.displayName);
                }
                if (!entry.readyAnnounced) {
                    entry.readyAnnounced = true;
                    changed = true;
                }
            }
        }

        changed |= pruneMissingEntries(source, seen);
        return changed;
    }

    private boolean pruneMissingEntries(String source, Set<String> seen) {
        boolean changed = false;
        Set<String> toRemove = new HashSet<>();

        for (Map.Entry<String, ThePrisonsEntry> entry : ThePrisonsClient.CACHE.entries().entrySet()) {
            ThePrisonsEntry value = entry.getValue();
            if (value == null || value.key == null || value.source == null) {
                continue;
            }

            if (!source.equalsIgnoreCase(value.source)) {
                continue;
            }

            if (!seen.contains(value.key)) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            ThePrisonsClient.CACHE.entries().remove(key);
            changed = true;
        }

        return changed;
    }

    private boolean updateReadyTransitions(long nowMs) {
        boolean changed = false;
        for (ThePrisonsEntry entry : ThePrisonsClient.CACHE.entries().values()) {
            if (entry == null || entry.displayName == null) {
                continue;
            }

            if (entry.cooldownEndsAtMs <= nowMs && !entry.readyAnnounced && ThePrisonsClient.CONFIG.get().general.showReadyAnnouncements) {
                ThePrisonsHudRenderer.pushAnnouncement(entry.displayName);
                entry.readyAnnounced = true;
                changed = true;
            }
        }
        return changed;
    }

    private boolean isPet(String displayName) {
        return displayName.contains(PET_TOKEN) && !displayName.equals("pet ready");
    }

    private boolean isTrinket(String displayName) {
        return displayName.contains("trinket")
                || displayName.contains("hook")
                || displayName.contains("orb")
                || displayName.contains("potion");
    }

    private String normalize(String value) {
        return stripLevel(value).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String stripLevel(String value) {
        return value
                .replaceAll("\\s*\\[lvl\\s*\\d+\\]\\s*", " ")
                .replaceAll("\\s*lvl\\s*\\d+\\s*", " ")
                .replaceAll("\\s*level\\s*\\d+\\s*", " ")
                .trim();
    }

    private String getItemId(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id == null ? "unknown" : id.toString();
    }
}
