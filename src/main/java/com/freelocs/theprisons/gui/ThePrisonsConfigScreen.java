package com.freelocs.theprisons.gui;

import com.freelocs.theprisons.ThePrisonsClient;
import com.freelocs.theprisons.config.ThePrisonsConfig;
import com.freelocs.theprisons.ui.ThePrisonsColors;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public final class ThePrisonsConfigScreen extends Screen {
    private static final Identifier LOGO = Identifier.of(ThePrisonsClient.MOD_ID, "icon.png");

    private final Screen parent;
    private final int selectedCategory;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public ThePrisonsConfigScreen(Screen parent) {
        this(parent, 0);
    }

    public ThePrisonsConfigScreen(Screen parent, int selectedCategory) {
        super(Text.literal("ThePrisons"));
        this.parent = parent;
        this.selectedCategory = selectedCategory;
    }

    @Override
    protected void init() {
        panelW = (int) (width * 0.74);
        panelH = (int) (height * 0.82);
        panelW = Math.max(700, Math.min(panelW, 960));
        panelH = Math.max(420, Math.min(panelH, 640));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        layout();
    }

    private void layout() {
        ThePrisonsConfig config = ThePrisonsClient.CONFIG.get();
        int headerH = 68;
        int sidebarW = 176;
        int sidebarX = panelX + 16;
        int sidebarY = panelY + headerH + 18;
        int contentX = panelX + sidebarW + 28;
        int contentY = panelY + headerH + 18;
        int rowW = panelW - sidebarW - 56;

        addDrawableChild(ButtonWidget.builder(Text.literal("General"), button ->
                client.setScreen(new ThePrisonsConfigScreen(parent, 0))).dimensions(sidebarX, sidebarY, 156, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("HUD"), button ->
                client.setScreen(new ThePrisonsConfigScreen(parent, 1))).dimensions(sidebarX, sidebarY + 28, 156, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("GUI"), button ->
                client.setScreen(new ThePrisonsConfigScreen(parent, 2))).dimensions(sidebarX, sidebarY + 56, 156, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("HUD Layout"), button ->
                client.setScreen(new ThePrisonsHudLayoutScreen(this, config))).dimensions(contentX, panelY + panelH - 40, 128, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(panelX + panelW - 84, panelY + panelH - 40, 72, 20).build());

        if (selectedCategory == 0) {
            addDrawableChild(ButtonWidget.builder(toggleLabel("Ready announcements", config.general.showReadyAnnouncements), button -> {
                config.general.showReadyAnnouncements = !config.general.showReadyAnnouncements;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 0));
            }).dimensions(contentX, contentY + 4, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(toggleLabel("Persist cooldown cache", config.general.persistCooldownCache), button -> {
                config.general.persistCooldownCache = !config.general.persistCooldownCache;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 0));
            }).dimensions(contentX, contentY + 32, rowW, 20).build());
        } else if (selectedCategory == 1) {
            addDrawableChild(ButtonWidget.builder(toggleLabel("HUD", config.hud.petHudEnabled), button -> {
                config.hud.petHudEnabled = !config.hud.petHudEnabled;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 1));
            }).dimensions(contentX, contentY + 4, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(toggleLabel("Show pet section", config.hud.showTrackedPets), button -> {
                config.hud.showTrackedPets = !config.hud.showTrackedPets;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 1));
            }).dimensions(contentX, contentY + 32, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(toggleLabel("Show trinkets", config.hud.showTrinkets), button -> {
                config.hud.showTrinkets = !config.hud.showTrinkets;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 1));
            }).dimensions(contentX, contentY + 60, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(toggleLabel("Show ready status", config.hud.showReadyStatus), button -> {
                config.hud.showReadyStatus = !config.hud.showReadyStatus;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 1));
            }).dimensions(contentX, contentY + 88, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(toggleLabel("Show countdown", config.hud.showCountdown), button -> {
                config.hud.showCountdown = !config.hud.showCountdown;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 1));
            }).dimensions(contentX, contentY + 116, rowW, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(toggleLabel("Snap to grid", config.gui.snapToGrid), button -> {
                config.gui.snapToGrid = !config.gui.snapToGrid;
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 2));
            }).dimensions(contentX, contentY + 4, rowW, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Grid size: " + config.gui.gridSize), button -> {
                config.gui.gridSize = cycle(config.gui.gridSize, 6, 12, 16, 24, 32);
                ThePrisonsClient.CONFIG.save();
                client.setScreen(new ThePrisonsConfigScreen(parent, 2));
            }).dimensions(contentX, contentY + 32, rowW, 20).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ThePrisonsConfig config = ThePrisonsClient.CONFIG.get();

        context.fill(0, 0, width, height, ThePrisonsColors.BG_OVERLAY);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, ThePrisonsColors.BG_PANEL);
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, ThePrisonsColors.ACCENT_CYAN);
        context.fill(panelX, panelY + 2, panelX + panelW, panelY + 4, ThePrisonsColors.ACCENT_PINK);
        context.fill(panelX, panelY + 4, panelX + panelW, panelY + 6, ThePrisonsColors.ACCENT_PURPLE);

        int headerH = 68;
        int sidebarW = 176;

        context.fill(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, ThePrisonsColors.BORDER);
        context.fill(panelX + sidebarW, panelY + headerH, panelX + sidebarW + 1, panelY + panelH, ThePrisonsColors.BORDER);
        context.fill(panelX + 12, panelY + headerH + 18, panelX + sidebarW - 12, panelY + panelH - 16, ThePrisonsColors.BG_SIDE);
        context.fill(panelX + sidebarW + 14, panelY + headerH + 18, panelX + panelW - 14, panelY + panelH - 16, ThePrisonsColors.BG_SECTION);

        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, panelX + 16, panelY + 14, 0f, 0f, 40, 40, 500, 500);
        context.drawTextWithShadow(textRenderer, Text.literal("ThePrisons"), panelX + 68, panelY + 18, ThePrisonsColors.FG_PRIMARY);
        context.drawTextWithShadow(textRenderer, Text.literal("Client-side pet and Trinkets HUD"), panelX + 68, panelY + 32, ThePrisonsColors.FG_MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal("TrustTheCat"), panelX + 68, panelY + 46, ThePrisonsColors.ACCENT_CYAN);

        drawSidebarLabel(context, "General", 0, panelX + 20, panelY + headerH + 28);
        drawSidebarLabel(context, "HUD", 1, panelX + 20, panelY + headerH + 56);
        drawSidebarLabel(context, "GUI", 2, panelX + 20, panelY + headerH + 84);

        int contentX = panelX + sidebarW + 28;
        int contentY = panelY + headerH + 18;
        String pageTitle = selectedCategory == 0 ? "General" : selectedCategory == 1 ? "HUD" : "GUI";
        String pageSubtitle = selectedCategory == 0
                ? "Announcement and cache behavior"
                : selectedCategory == 1
                ? "Pets and Trinkets cooldown display"
                : "Screen positions, grid snapping, and scale";

        context.drawTextWithShadow(textRenderer, Text.literal(pageTitle), contentX, contentY + 4, ThePrisonsColors.FG_PRIMARY);
        context.drawTextWithShadow(textRenderer, Text.literal(pageSubtitle), contentX, contentY + 18, ThePrisonsColors.FG_MUTED);
        context.fill(contentX, contentY + 28, panelX + panelW - 24, contentY + 29, ThePrisonsColors.BORDER_HI);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSidebarLabel(DrawContext context, String label, int category, int x, int y) {
        int color = selectedCategory == category ? ThePrisonsColors.FG_PRIMARY : ThePrisonsColors.FG_MUTED;
        int accent = selectedCategory == category
                ? (category == 0 ? ThePrisonsColors.ACCENT_CYAN : category == 1 ? ThePrisonsColors.ACCENT_PINK : ThePrisonsColors.ACCENT_PURPLE)
                : ThePrisonsColors.BORDER;
        context.fill(x - 6, y - 2, x - 2, y + 12, accent);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x, y, color);
    }

    private Text toggleLabel(String label, boolean enabled) {
        return Text.literal(label + ": " + (enabled ? "On" : "Off"));
    }

    private int cycle(int current, int... values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    @Override
    public void close() {
        ThePrisonsClient.CONFIG.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
