package com.freelocs.petready.ui;

import com.freelocs.petready.PetReadyClient;
import com.freelocs.petready.cache.PetCooldownCache.PetCooldownEntry;
import com.freelocs.petready.config.PetReadyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

public final class PetReadyHudRenderer {
    private static final long ANNOUNCEMENT_DURATION_MS = 4500L;
    private static final java.util.List<Announcement> ANNOUNCEMENTS = new java.util.ArrayList<>();

    private PetReadyHudRenderer() {
    }

    public static void pushAnnouncement(String petName) {
        Announcement announcement = new Announcement(petName, System.currentTimeMillis());
        ANNOUNCEMENTS.removeIf(existing -> existing.petName.equalsIgnoreCase(petName));
        ANNOUNCEMENTS.add(announcement);
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        PetReadyConfig config = PetReadyClient.CONFIG.get();
        if (config.hud.petHudEnabled) {
            renderPetHud(context, client);
        }

        renderAnnouncements(context, client);
    }

    private static void renderPetHud(DrawContext context, MinecraftClient client) {
        List<PetCooldownEntry> entries = PetReadyClient.CACHE.entries().values().stream()
                .filter(entry -> entry != null && entry.displayName != null)
                .sorted(Comparator.comparing(entry -> entry.displayName.toLowerCase()))
                .toList();

        if (entries.isEmpty()) {
            return;
        }

        PetReadyConfig config = PetReadyClient.CONFIG.get();
        int x = config.gui.petHudX;
        int y = config.gui.petHudY;
        int width = 200;
        int lineHeight = 16;

        context.drawTextWithShadow(client.textRenderer, Text.literal("Pet HUD"), x, y, 0xFF000000);

        long nowMs = System.currentTimeMillis();
        int lineY = y + 14;
        for (PetCooldownEntry entry : entries) {
            boolean ready = entry.cooldownEndsAtMs <= nowMs;
            int statusColor = ready ? 0xFF45F28A : 0xFFFF4B5C;
            String status = ready ? "Ready" : formatRemaining(entry.cooldownEndsAtMs - nowMs);

            context.drawTextWithShadow(client.textRenderer, Text.literal(entry.displayName), x, lineY, PetReadyColors.FG_PRIMARY);
            if ((ready && config.hud.showReadyStatus) || (!ready && config.hud.showCountdown)) {
                context.drawTextWithShadow(client.textRenderer, Text.literal(status), x + width - client.textRenderer.getWidth(status), lineY, statusColor);
            }
            lineY += lineHeight;
        }
    }

    private static void renderAnnouncements(DrawContext context, MinecraftClient client) {
        if (ANNOUNCEMENTS.isEmpty()) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        Announcement active = ANNOUNCEMENTS.stream()
                .filter(announcement -> nowMs - announcement.startedAtMs <= ANNOUNCEMENT_DURATION_MS)
                .findFirst()
                .orElse(null);
        if (active == null) {
            ANNOUNCEMENTS.removeIf(announcement -> nowMs - announcement.startedAtMs > ANNOUNCEMENT_DURATION_MS);
            return;
        }

        String petName = stripPetSuffix(active.petName);
        String title = "Pet Ready";
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float scale = 1.45f;
        int centerX = screenWidth / 2;
        int y = Math.round(screenHeight * 0.25f);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        int scaledCenterX = Math.round(centerX / scale);
        int scaledY = Math.round(y / scale);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(petName), scaledCenterX, scaledY, PetReadyColors.ACCENT_BLUE);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(title), scaledCenterX, scaledY + 10, PetReadyColors.ACCENT_PINK);
        context.getMatrices().popMatrix();
    }

    private static String stripPetSuffix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.toLowerCase(java.util.Locale.ROOT).endsWith(" pet")) {
            return trimmed.substring(0, trimmed.length() - 4).trim();
        }
        return trimmed;
    }

    private static String formatRemaining(long remainingMs) {
        long totalSeconds = Math.max(0L, remainingMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static final class Announcement {
        public final String petName;
        public final long startedAtMs;

        public Announcement(String petName, long startedAtMs) {
            this.petName = petName;
            this.startedAtMs = startedAtMs;
        }
    }
}
