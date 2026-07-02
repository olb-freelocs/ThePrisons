package com.freelocs.theprisons.ui;

import com.freelocs.theprisons.ThePrisonsClient;
import com.freelocs.theprisons.cache.ThePrisonsCache.ThePrisonsEntry;
import com.freelocs.theprisons.config.ThePrisonsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ThePrisonsHudRenderer {
    private static final long ANNOUNCEMENT_DURATION_MS = 4500L;
    private static final List<Announcement> ANNOUNCEMENTS = new ArrayList<>();
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 12;
    private static final int ENTRY_HEIGHT = 11;
    private static final int SECTION_GAP = 6;

    private ThePrisonsHudRenderer() {
    }

    public static void pushAnnouncement(String petName) {
        Announcement announcement = new Announcement(petName, System.currentTimeMillis());
        ANNOUNCEMENTS.removeIf(existing -> existing.petName.equalsIgnoreCase(petName));
        ANNOUNCEMENTS.add(announcement);
    }

    public static HudDimensions drawHudOverlay(DrawContext context, MinecraftClient client, ThePrisonsConfig config, int x, int y, float scale, boolean preview) {
        if (client == null || client.textRenderer == null) {
            return HudDimensions.EMPTY;
        }

        List<TrackedSection> sections = collectSections(client, config);
        if (sections.isEmpty()) {
            return HudDimensions.EMPTY;
        }

        int baseWidth = measureWidth(client, sections);
        int baseHeight = measureHeight(sections);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x / scale, y / scale);
        context.getMatrices().scale(scale, scale);

        drawFrame(context, config, baseWidth, baseHeight);

        int cursorY = PADDING;
        for (int i = 0; i < sections.size(); i++) {
            TrackedSection section = sections.get(i);
            drawSection(context, client, config, section, baseWidth, cursorY);
            cursorY += sectionHeight(section);
            if (i < sections.size() - 1) {
                cursorY += SECTION_GAP;
            }
        }

        if (preview) {
            context.drawTextWithShadow(client.textRenderer, Text.literal("Scroll to resize"), PADDING, baseHeight + 4, ThePrisonsColors.FG_MUTED);
        }

        context.getMatrices().popMatrix();
        return new HudDimensions(Math.round(baseWidth * scale), Math.round(baseHeight * scale));
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            return;
        }

        ThePrisonsConfig config = ThePrisonsClient.CONFIG.get();
        if (config.hud.petHudEnabled) {
            drawHudOverlay(context, client, config, config.gui.petHudX, config.gui.petHudY, config.gui.hudScale, false);
        }

        renderAnnouncements(context, client);
    }

    public static HudDimensions measureHud(MinecraftClient client, ThePrisonsConfig config) {
        List<TrackedSection> sections = collectSections(client, config);
        if (sections.isEmpty()) {
            return HudDimensions.EMPTY;
        }
        int baseWidth = measureWidth(client, sections);
        int baseHeight = measureHeight(sections);
        return new HudDimensions(Math.round(baseWidth * config.gui.hudScale), Math.round(baseHeight * config.gui.hudScale));
    }

    public static boolean isInsideHud(MinecraftClient client, ThePrisonsConfig config, int mouseX, int mouseY) {
        HudDimensions size = measureHud(client, config);
        int scaledWidth = size.width;
        int scaledHeight = size.height;
        int x = config.gui.petHudX;
        int y = config.gui.petHudY;
        return mouseX >= x && mouseX <= x + scaledWidth && mouseY >= y && mouseY <= y + scaledHeight;
    }

    private static List<TrackedSection> collectSections(MinecraftClient client, ThePrisonsConfig config) {
        List<TrackedSection> sections = new ArrayList<>();
        if (config.hud.showTrackedPets) {
            List<HudEntry> pets = collectEntries("PET", ThePrisonsClient.CACHE.entries().values(), client, config);
            if (!pets.isEmpty()) {
                sections.add(new TrackedSection("Pets", ThePrisonsColors.ACCENT_BLUE, pets));
            }
        }
        if (config.hud.showTrinkets) {
            List<HudEntry> trinkets = collectEntries("TRINKET", ThePrisonsClient.CACHE.entries().values(), client, config);
            if (!trinkets.isEmpty()) {
                sections.add(new TrackedSection("Trinkets", ThePrisonsColors.ACCENT_PINK, trinkets));
            }
        }
        return sections;
    }

    private static List<HudEntry> collectEntries(String source, Iterable<ThePrisonsEntry> values, MinecraftClient client, ThePrisonsConfig config) {
        long nowMs = System.currentTimeMillis();
        List<HudEntry> entries = new ArrayList<>();
        for (ThePrisonsEntry entry : values) {
            if (entry == null || entry.displayName == null) {
                continue;
            }
            String category = entry.source == null || entry.source.isBlank() ? "PET" : entry.source;
            if (!source.equalsIgnoreCase(category)) {
                continue;
            }

            boolean ready = entry.cooldownEndsAtMs <= nowMs;
            String status = "";
            int statusColor = ready ? 0xFF45F28A : 0xFFFF4B5C;
            if (ready && config.hud.showReadyStatus) {
                status = "Ready";
            } else if (!ready && config.hud.showCountdown) {
                status = formatRemaining(entry.cooldownEndsAtMs - nowMs);
            }

            entries.add(new HudEntry(stripPetSuffix(entry.displayName), status, ready, statusColor));
        }

        entries.sort(Comparator.comparing(value -> value.displayName.toLowerCase(Locale.ROOT)));
        return entries;
    }

    private static void drawFrame(DrawContext context, ThePrisonsConfig config, int width, int height) {
        context.fill(0, 0, width, height, config.hud.backgroundColor);
        context.fill(0, 0, width, 1, config.hud.borderColor);
        context.fill(0, height - 1, width, height, config.hud.borderColor);
        context.fill(0, 0, 1, height, config.hud.borderColor);
        context.fill(width - 1, 0, width, height, config.hud.borderColor);
    }

    private static void drawSection(DrawContext context, MinecraftClient client, ThePrisonsConfig config, TrackedSection section, int width, int startY) {
        int textColor = section.accentColor;
        context.drawTextWithShadow(client.textRenderer, Text.literal(section.title), PADDING, startY, textColor);
        int cursorY = startY + HEADER_HEIGHT;
        for (HudEntry entry : section.entries) {
            context.drawTextWithShadow(client.textRenderer, Text.literal(entry.displayName), PADDING, cursorY, ThePrisonsColors.ACCENT_BLUE);
            if (!entry.statusText.isEmpty()) {
                int statusX = width - PADDING - client.textRenderer.getWidth(entry.statusText);
                context.drawTextWithShadow(client.textRenderer, Text.literal(entry.statusText), statusX, cursorY, entry.statusColor);
            }
            cursorY += ENTRY_HEIGHT;
        }
    }

    private static int measureWidth(MinecraftClient client, List<TrackedSection> sections) {
        int width = 0;
        for (TrackedSection section : sections) {
            int sectionWidth = client.textRenderer.getWidth(section.title);
            for (HudEntry entry : section.entries) {
                int entryWidth = client.textRenderer.getWidth(entry.displayName);
                if (!entry.statusText.isEmpty()) {
                    entryWidth += 10 + client.textRenderer.getWidth(entry.statusText);
                }
                sectionWidth = Math.max(sectionWidth, entryWidth);
            }
            width = Math.max(width, sectionWidth + PADDING * 2);
        }
        return Math.max(width, 120);
    }

    private static int measureHeight(List<TrackedSection> sections) {
        int height = PADDING;
        for (int i = 0; i < sections.size(); i++) {
            TrackedSection section = sections.get(i);
            height += sectionHeight(section);
            if (i < sections.size() - 1) {
                height += SECTION_GAP;
            }
        }
        return height + PADDING;
    }

    private static int sectionHeight(TrackedSection section) {
        return HEADER_HEIGHT + (section.entries.size() * ENTRY_HEIGHT);
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

        String itemName = stripPetSuffix(active.petName);
        String title = "Ready";
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        float scale = 1.4f;
        int centerX = screenWidth / 2;
        int y = Math.round(screenHeight * 0.25f);

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        int scaledCenterX = Math.round(centerX / scale);
        int scaledY = Math.round(y / scale);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(itemName), scaledCenterX, scaledY, ThePrisonsColors.ACCENT_BLUE);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(title), scaledCenterX, scaledY + 10, ThePrisonsColors.ACCENT_PINK);
        context.getMatrices().popMatrix();
    }

    private static String stripPetSuffix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(" pet")) {
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

    public static final class HudDimensions {
        public static final HudDimensions EMPTY = new HudDimensions(0, 0);
        public final int width;
        public final int height;

        public HudDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public static final class Announcement {
        public final String petName;
        public final long startedAtMs;

        public Announcement(String petName, long startedAtMs) {
            this.petName = petName;
            this.startedAtMs = startedAtMs;
        }
    }

    private static final class TrackedSection {
        private final String title;
        private final int accentColor;
        private final List<HudEntry> entries;

        private TrackedSection(String title, int accentColor, List<HudEntry> entries) {
            this.title = title;
            this.accentColor = accentColor;
            this.entries = entries;
        }
    }

    private static final class HudEntry {
        private final String displayName;
        private final String statusText;
        private final boolean ready;
        private final int statusColor;

        private HudEntry(String displayName, String statusText, boolean ready, int statusColor) {
            this.displayName = displayName;
            this.statusText = statusText;
            this.ready = ready;
            this.statusColor = statusColor;
        }
    }
}
