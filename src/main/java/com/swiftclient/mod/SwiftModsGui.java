package com.swiftclient.mod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class SwiftModsGui extends GuiScreen {

    private final GuiScreen parent;
    private final SwiftConfig config;
    private GuiTextField searchField;
    private String searchQuery = "";
    private String editingMod = null;
    private final List<String> visibleMods = new ArrayList<>();

    public SwiftModsGui(GuiScreen parent, SwiftConfig config) {
        this.parent = parent;
        this.config = config;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        if (editingMod != null) {
            buildModSettingsButtons();
            return;
        }

        int searchW = 150;
        int searchX = width - searchW - 14;
        int searchY = 12;

        searchField = new GuiTextField(0, fontRendererObj, searchX, searchY, searchW, 18);
        searchField.setText(searchQuery);
        searchField.setFocused(true);

        rebuildModButtons();
    }

    private void rebuildModButtons() {
        buttonList.clear();
        visibleMods.clear();

        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        for (String name : SwiftConfig.MOD_ORDER) {
            if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) {
                visibleMods.add(name);
            }
        }

        int startY = 38;
        int rowH = 21;
        int colW = Math.min(260, width - 36);
        int colX = (width - colW) / 2;

        for (int i = 0; i < visibleMods.size(); i++) {
            String name = visibleMods.get(i);
            SwiftConfig.HudElementConfig cfg = config.get(name);
            String stateLabel = cfg.enabled ? "§aON" : "§cOFF";
            buttonList.add(new GuiButton(100 + i, colX, startY + i * rowH, colW, 19, name + "  [" + stateLabel + "§r]"));
        }

        buttonList.add(new GuiButton(999, width / 2 - 60, height - 26, 120, 20, "Wstecz"));
    }

    private void buildModSettingsButtons() {
        SwiftConfig.HudElementConfig cfg = config.get(editingMod);
        int cx = width / 2;
        int cy = height / 2;

        buttonList.add(new GuiButton(
                201,
                cx - 95,
                cy - 30,
                190,
                20,
                "Moduł: " + (cfg.enabled ? "§aWłączony" : "§cWyłączony")
        ));
        buttonList.add(new GuiButton(
                202,
                cx - 95,
                cy - 4,
                190,
                20,
                "Kolory: " + (cfg.colored ? "§aWłączone" : "§fWyłączone (białe)")
        ));
        buttonList.add(new GuiButton(
                203,
                cx - 95,
                cy + 22,
                190,
                20,
                "Tło: " + (cfg.background ? "§aWłączone" : "§7Wyłączone")
        ));
        buttonList.add(new GuiButton(209, cx - 60, cy + 54, 120, 20, "Wstecz do modów"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (editingMod != null) {
            SwiftConfig.HudElementConfig cfg = config.get(editingMod);
            if (button.id == 201) {
                cfg.enabled = !cfg.enabled;
                config.save();
                buildModSettingsButtons();
            } else if (button.id == 202) {
                cfg.colored = !cfg.colored;
                config.save();
                buildModSettingsButtons();
            } else if (button.id == 203) {
                cfg.background = !cfg.background;
                config.save();
                buildModSettingsButtons();
            } else if (button.id == 209) {
                editingMod = null;
                initGui();
            }
            return;
        }

        if (button.id == 999) {
            config.save();
            mc.displayGuiScreen(parent);
            return;
        }
        int idx = button.id - 100;
        if (idx >= 0 && idx < visibleMods.size()) {
            editingMod = visibleMods.get(idx);
            initGui();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            if (editingMod != null) {
                editingMod = null;
                initGui();
            } else {
                config.save();
                mc.displayGuiScreen(parent);
            }
            return;
        }
        if (editingMod == null && searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            searchQuery = searchField.getText();
            rebuildModButtons();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (editingMod == null && searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        if (editingMod != null) {
            drawCenteredString(fontRendererObj, "§lUstawienia: " + editingMod, width / 2, height / 2 - 54, 0x38BDF8);
        } else {
            drawString(fontRendererObj, "§lSwiftClient Mods", 16, 16, 0x38BDF8);
            drawString(fontRendererObj, "Wyszukaj:", searchField.xPosition - 58, searchField.yPosition + 5, 0xCBD5E1);
            searchField.drawTextBox();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
