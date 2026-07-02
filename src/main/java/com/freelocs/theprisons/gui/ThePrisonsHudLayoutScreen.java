package com.freelocs.theprisons.gui;

import com.freelocs.theprisons.ThePrisonsClient;
import com.freelocs.theprisons.config.ThePrisonsConfig;
import com.freelocs.theprisons.ui.ThePrisonsColors;
import com.freelocs.theprisons.ui.ThePrisonsHudRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ThePrisonsHudLayoutScreen extends Screen {
    private final Screen parent;
    private final ThePrisonsConfig config;
    private DragTarget dragTarget;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean wasMouseDown;

    public ThePrisonsHudLayoutScreen(Screen parent, ThePrisonsConfig config) {
        super(Text.literal("HUD Layout"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> close()).dimensions(12, height - 28, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateDrag(mouseX, mouseY);
        context.fill(0, 0, width, height, ThePrisonsColors.BG_OVERLAY);
        drawGrid(context);
        context.drawTextWithShadow(textRenderer, Text.literal("Hover the HUD and use the mouse wheel to resize it."), 12, 12, ThePrisonsColors.FG_PRIMARY);
        context.drawTextWithShadow(textRenderer, Text.literal(String.format("Scale: %.2fx", config.gui.hudScale)), 12, 24, ThePrisonsColors.FG_MUTED);
        ThePrisonsHudRenderer.drawHudOverlay(context, client, config, config.gui.petHudX, config.gui.petHudY, config.gui.hudScale, true);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawGrid(DrawContext context) {
        int grid = Math.max(4, config.gui.gridSize);
        for (int x = 0; x < width; x += grid) {
            context.fill(x, 0, x + 1, height, ThePrisonsColors.GRID);
        }
        for (int y = 0; y < height; y += grid) {
            context.fill(0, y, width, y + 1, ThePrisonsColors.GRID);
        }
    }

    private boolean insideHud(int mouseX, int mouseY) {
        return ThePrisonsHudRenderer.isInsideHud(client, config, mouseX, mouseY);
    }

    private void updateDrag(int mouseX, int mouseY) {
        boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (mouseDown && !wasMouseDown && insideHud(mouseX, mouseY)) {
            dragTarget = DragTarget.HUD;
            dragOffsetX = mouseX - config.gui.petHudX;
            dragOffsetY = mouseY - config.gui.petHudY;
        }

        if (mouseDown && dragTarget != null) {
            ThePrisonsConfig.GuiConfig gui = config.gui;
            int grid = gui.snapToGrid ? gui.gridSize : 1;
            int newX = snap((int) (mouseX - dragOffsetX), grid);
            int newY = snap((int) (mouseY - dragOffsetY), grid);
            gui.petHudX = Math.max(0, newX);
            gui.petHudY = Math.max(0, newY);
            ThePrisonsClient.CONFIG.save();
        }

        if (!mouseDown) {
            dragTarget = null;
        }

        wasMouseDown = mouseDown;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (insideHud((int) mouseX, (int) mouseY)) {
            float step = verticalAmount > 0 ? 0.05f : -0.05f;
            config.gui.hudScale = Math.max(0.5f, Math.min(2.5f, config.gui.hudScale + step));
            ThePrisonsClient.CONFIG.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int snap(int value, int grid) {
        if (grid <= 1) {
            return value;
        }
        return Math.round(value / (float) grid) * grid;
    }

    @Override
    public void close() {
        ThePrisonsClient.CONFIG.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private enum DragTarget {
        HUD
    }
}
