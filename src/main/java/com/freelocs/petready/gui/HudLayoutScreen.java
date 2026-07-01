package com.freelocs.petready.gui;

import com.freelocs.petready.PetReadyClient;
import com.freelocs.petready.config.PetReadyConfig;
import com.freelocs.petready.ui.PetReadyColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class HudLayoutScreen extends Screen {
    private final Screen parent;
    private final PetReadyConfig config;
    private DragTarget dragTarget;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean wasMouseDown;

    public HudLayoutScreen(Screen parent, PetReadyConfig config) {
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
        context.fill(0, 0, width, height, PetReadyColors.BG_OVERLAY);
        drawGrid(context);
        drawElement(context, config.gui.petHudX, config.gui.petHudY, 180, 86, "Pet HUD", dragTarget == DragTarget.PET_HUD);
        context.drawTextWithShadow(textRenderer, Text.literal("Drag the HUD. Positions are saved instantly."), 12, 12, PetReadyColors.FG_PRIMARY);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawGrid(DrawContext context) {
        int grid = Math.max(4, config.gui.gridSize);
        for (int x = 0; x < width; x += grid) {
            context.fill(x, 0, x + 1, height, PetReadyColors.GRID);
        }
        for (int y = 0; y < height; y += grid) {
            context.fill(0, y, width, y + 1, PetReadyColors.GRID);
        }
    }

    private void drawElement(DrawContext context, int x, int y, int width, int height, String label, boolean active) {
        int border = active ? PetReadyColors.ACCENT_CYAN : PetReadyColors.BORDER;
        context.fill(x, y, x + width, y + height, 0x66000000);
        context.fill(x, y, x + width, y + 1, border);
        context.fill(x, y + height - 1, x + width, y + height, border);
        context.fill(x, y, x + 1, y + height, border);
        context.fill(x + width - 1, y, x + width, y + height, border);
        context.drawTextWithShadow(textRenderer, Text.literal(label), x + 6, y + 6, PetReadyColors.FG_PRIMARY);
    }

    private boolean inside(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    private void updateDrag(int mouseX, int mouseY) {
        boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (mouseDown && !wasMouseDown) {
            if (inside(mouseX, mouseY, config.gui.petHudX, config.gui.petHudY, 180, 86)) {
                dragTarget = DragTarget.PET_HUD;
                dragOffsetX = mouseX - config.gui.petHudX;
                dragOffsetY = mouseY - config.gui.petHudY;
            }
        }

        if (mouseDown && dragTarget != null) {
            PetReadyConfig.GuiConfig gui = config.gui;
            int grid = gui.snapToGrid ? gui.gridSize : 1;
            int newX = snap((int) (mouseX - dragOffsetX), grid);
            int newY = snap((int) (mouseY - dragOffsetY), grid);
            if (dragTarget == DragTarget.PET_HUD) {
                gui.petHudX = Math.max(0, newX);
                gui.petHudY = Math.max(0, newY);
            }
            PetReadyClient.CONFIG.save();
        }

        if (!mouseDown) {
            dragTarget = null;
        }

        wasMouseDown = mouseDown;
    }

    private int snap(int value, int grid) {
        if (grid <= 1) {
            return value;
        }
        return Math.round(value / (float) grid) * grid;
    }

    @Override
    public void close() {
        PetReadyClient.CONFIG.save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private enum DragTarget {
        PET_HUD
    }
}
