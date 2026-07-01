package com.freelocs.petready.config;

public final class PetReadyConfig {
    public final GeneralConfig general = new GeneralConfig();
    public final HudConfig hud = new HudConfig();
    public final GuiConfig gui = new GuiConfig();

    public void normalize() {
        hud.petHudColor = clampColor(hud.petHudColor, 0xFFA8E8FF);
        hud.readyColor = clampColor(hud.readyColor, 0xFFF84EA8);
        hud.backgroundColor = clampColor(hud.backgroundColor, 0xAA0E1220);
        hud.borderColor = clampColor(hud.borderColor, 0x44FFFFFF);
        gui.gridSize = clamp(gui.gridSize, 6, 32);
        gui.petHudX = Math.max(0, gui.petHudX);
        gui.petHudY = Math.max(0, gui.petHudY);
        gui.announcementX = Math.max(0, gui.announcementX);
        gui.announcementY = Math.max(0, gui.announcementY);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampColor(int color, int fallback) {
        return color == 0 ? fallback : color;
    }

    public static final class GeneralConfig {
        public boolean showReadyAnnouncements = true;
        public boolean persistCooldownCache = true;
    }

    public static final class HudConfig {
        public boolean petHudEnabled = true;
        public boolean showTrackedPets = true;
        public boolean showReadyStatus = true;
        public boolean showCountdown = true;
        public int petHudColor = 0xFFA8E8FF;
        public int readyColor = 0xFFF84EA8;
        public int backgroundColor = 0xAA0E1220;
        public int borderColor = 0x44FFFFFF;
    }

    public static final class GuiConfig {
        public boolean snapToGrid = true;
        public int gridSize = 12;
        public int petHudX = 16;
        public int petHudY = 16;
        public int announcementX = 16;
        public int announcementY = 72;
    }
}
