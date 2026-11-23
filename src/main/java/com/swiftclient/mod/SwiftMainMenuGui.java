package com.swiftclient.mod;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

public class SwiftMainMenuGui extends GuiScreen {

    private final SwiftConfig config;
    private final SwiftHudRenderer renderer;
    private boolean editHudLayout = true;
    private String selectedMod = "Keystrokes";
    private String draggedMod = null;

    public SwiftMainMenuGui(SwiftConfig config, SwiftHudRenderer renderer) {
        this.config = config;
        this.renderer = renderer;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int cx = width / 2;
        int cy = height / 2;

        buttonList.add(new GuiButton(1, cx - 70, cy + 4, 140, 20, "Mods"));
        buttonList.add(new GuiButton(
                2,
                cx - 70,
                cy + 28,
                140,
                20,
                editHudLayout ? "Ukryj ramki HUD" : "Edytuj położenie i rozmiar"
        ));
        buttonList.add(new GuiButton(3, cx - 70, cy + 52, 140, 20, "Resetuj pozycje"));
        buttonList.add(new GuiButton(4, cx - 70, cy + 76, 67, 18, "Rozmiar -"));
        buttonList.add(new GuiButton(5, cx + 3, cy + 76, 67, 18, "Rozmiar +"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        SwiftConfig.HudElementConfig selCfg = config.get(selectedMod);
        if (button.id == 1) {
            mc.displayGuiScreen(new SwiftModsGui(this, config));
        } else if (button.id == 2) {
            editHudLayout = !editHudLayout;
            config.save();
            initGui();
        } else if (button.id == 3) {
            config.resetDefaults();
            config.save();
            draggedMod = null;
            initGui();
        } else if (button.id == 4) {
            selCfg.scale = (float) SwiftConfig.clamp(selCfg.scale - 0.1, 0.5, 2.0);
            config.save();
        } else if (button.id == 5) {
            selCfg.scale = (float) SwiftConfig.clamp(selCfg.scale + 0.1, 0.5, 2.0);
            config.save();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        if (editHudLayout) {
            for (String modName : SwiftConfig.MOD_ORDER) {
                SwiftConfig.HudElementConfig cfg = config.get(modName);
                int mx = (int) (cfg.x * width);
                int my = (int) (cfg.y * height);
                int boxW = (int) (60 * cfg.scale);
                int boxH = (int) (18 * cfg.scale);
                int borderColor = modName.equals(selectedMod) ? 0x8838BDF8 : 0x440EA5E9;
                drawRect(mx - boxW, my - boxH, mx + boxW, my + boxH, borderColor);
                renderer.drawModule(mc, modName, cfg, width, height, true);
                String label = modName + " (" + String.format("%.1fx", cfg.scale) + ")";
                drawCenteredString(fontRendererObj, label, mx, my - 4, 0xFFFFFF);
            }
            drawCenteredString(
                    fontRendererObj,
                    "Wybrany: " + selectedMod + " • Przeciągnij LPM lub użyj Rozmiar - / + / Kółko myszy",
                    width / 2,
                    12,
                    0x38BDF8
            );
        }

        int cx = width / 2;
        int cy = height / 2 - 22;
        drawRect(cx - 16, cy - 30, cx + 16, cy - 10, 0xFF0EA5E9);
        drawRect(cx - 7, cy - 25, cx + 7, cy - 15, 0xFF0C121D);
        drawCenteredString(fontRendererObj, "SwiftClient", cx, cy - 4, 0xF8FAFC);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (editHudLayout && mouseButton == 0) {
            for (String modName : SwiftConfig.MOD_ORDER) {
                SwiftConfig.HudElementConfig cfg = config.get(modName);
                int mx = (int) (cfg.x * width);
                int my = (int) (cfg.y * height);
                int radX = (int) (60 * cfg.scale);
                int radY = (int) (18 * cfg.scale);
                if (Math.abs(mouseX - mx) <= radX && Math.abs(mouseY - my) <= radY) {
                    selectedMod = modName;
                    draggedMod = modName;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (editHudLayout && draggedMod != null && clickedMouseButton == 0) {
            SwiftConfig.HudElementConfig cfg = config.get(draggedMod);
            cfg.x = SwiftConfig.clamp((double) mouseX / Math.max(1, width), 0.05, 0.95);
            cfg.y = SwiftConfig.clamp((double) mouseY / Math.max(1, height), 0.05, 0.95);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (draggedMod != null) {
            config.save();
            draggedMod = null;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (editHudLayout && wheel != 0) {
            SwiftConfig.HudElementConfig cfg = config.get(selectedMod);
            float delta = wheel > 0 ? 0.1F : -0.1F;
            cfg.scale = (float) SwiftConfig.clamp(cfg.scale + delta, 0.5, 2.0);
            config.save();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
