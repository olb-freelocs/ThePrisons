package com.freelocs.petready.state;

import com.freelocs.petready.PetReadyClient;
import com.freelocs.petready.cache.PetCooldownCache.PetCooldownEntry;
import com.freelocs.petready.config.PetReadyConfig;
import com.freelocs.petready.mixin.ItemCooldownInstanceAccessor;
import com.freelocs.petready.mixin.ItemCooldownsAccessor;
import com.freelocs.petready.ui.PetReadyHudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PetCooldownTracker {
    private static final String PET_TOKEN = "pet";

    public void tick(MinecraftClient client) {
        long nowMs = System.currentTimeMillis();
        boolean changed = updateReadyTransitions(nowMs);

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            if (changed) {
                PetReadyClient.CACHE.save();
            }
            return;
        }

        PetReadyConfig config = PetReadyClient.CONFIG.get();
        if (!config.general.persistCooldownCache) {
            return;
        }

        changed |= scanInventory(player, nowMs);

        if (changed) {
            PetReadyClient.CACHE.save();
        }
    }

    private boolean scanInventory(ClientPlayerEntity player, long nowMs) {
        PlayerInventory inventory = player.getInventory();
        ItemCooldownManager cooldowns = player.getItemCooldownManager();
        ItemCooldownsAccessor cooldownsAccessor = (ItemCooldownsAccessor) cooldowns;
        Map<?, ?> activeCooldowns = cooldownsAccessor.petready$getCooldowns();
        int tickCount = cooldownsAccessor.petready$getTickCount();
        Set<String> seen = new HashSet<>();
        boolean changed = false;

        for (ItemStack stack : inventory.getMainStacks()) {
            if (stack.isEmpty()) {
                continue;
            }

            String displayName = normalize(stack.getName().getString());
            if (!isPet(displayName)) {
                continue;
            }

            seen.add(displayName);
            PetCooldownEntry entry = PetReadyClient.CACHE.getOrCreate(displayName);
            entry.key = displayName;
            String stackName = stripLevel(stack.getName().getString());
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
                int endTick = ((ItemCooldownInstanceAccessor) instance).petready$getEndTime();
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
                if (!entry.readyAnnounced && PetReadyClient.CONFIG.get().general.showReadyAnnouncements) {
                    PetReadyHudRenderer.pushAnnouncement(entry.displayName);
                }
                if (!entry.readyAnnounced) {
                    entry.readyAnnounced = true;
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean updateReadyTransitions(long nowMs) {
        boolean changed = false;
        for (PetCooldownEntry entry : PetReadyClient.CACHE.entries().values()) {
            if (entry == null || entry.displayName == null) {
                continue;
            }

            if (entry.cooldownEndsAtMs <= nowMs && !entry.readyAnnounced && PetReadyClient.CONFIG.get().general.showReadyAnnouncements) {
                PetReadyHudRenderer.pushAnnouncement(entry.displayName);
                entry.readyAnnounced = true;
                changed = true;
            }
        }
        return changed;
    }

    private boolean isPet(String displayName) {
        return displayName.contains(PET_TOKEN) && !displayName.equals("pet ready");
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
