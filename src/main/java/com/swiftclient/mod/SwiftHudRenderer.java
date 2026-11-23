package com.swiftclient.mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stat.StatBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class SwiftHudRenderer {

    private final SwiftConfig config;
    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();
    private boolean sprintToggled = false;
    private boolean sprintKeyWasDown = false;

    public SwiftHudRenderer(SwiftConfig config) {
        this.config = config;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.currentScreen != null) {
            return;
        }

        SwiftConfig.HudElementConfig tsCfg = config.get("ToggleSprint");
        if (!tsCfg.enabled) {
            return;
        }

        int sprintKeyCode = mc.gameSettings.keyBindSprint.getKeyCode();
        boolean down = sprintKeyCode != 0 && Keyboard.isKeyDown(sprintKeyCode);
        if (down && !sprintKeyWasDown) {
            sprintToggled = !sprintToggled;
        }
        sprintKeyWasDown = down;

        boolean movingForward = player.movementInput != null && player.movementInput.moveForward > 0.0F;
        if (sprintToggled && movingForward && !player.isSneaking() && !player.isUsingItem()) {
            player.setSprinting(true);
        }
    }

    @SubscribeEvent
    public void onMouseClick(MouseEvent event) {
        if (!event.buttonstate) {
            return;
        }
        long now = System.currentTimeMillis();
        if (event.button == 0) {
            leftClicks.add(now);
        } else if (event.button == 1) {
            rightClicks.add(now);
        }
    }

    private int countCps(List<Long> clicks) {
        long cutoff = System.currentTimeMillis() - 1000L;
        Iterator<Long> it = clicks.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            }
        }
        return clicks.size();
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        for (String modName : SwiftConfig.MOD_ORDER) {
            SwiftConfig.HudElementConfig cfg = config.get(modName);
            if (!cfg.enabled) {
                continue;
            }
            drawModule(mc, modName, cfg, sw, sh, false);
        }
    }

    public void drawModule(
            Minecraft mc,
            String modName,
            SwiftConfig.HudElementConfig cfg,
            int screenW,
            int screenH,
            boolean previewMode
    ) {
        FontRenderer fr = mc.fontRendererObj;
        int drawX = (int) (cfg.x * screenW);
        int drawY = (int) (cfg.y * screenH);
        int boxBg = cfg.background ? 0x88090D16 : 0x00000000;

        GlStateManager.pushMatrix();
        GlStateManager.translate(drawX, drawY, 0.0F);
        GlStateManager.scale(cfg.scale, cfg.scale, 1.0F);

        if ("FPS".equals(modName)) {
            String txt = "FPS: " + Minecraft.getDebugFPS();
            int w = fr.getStringWidth(txt) + 8;
            Gui.drawRect(-w / 2, -6, w / 2, 10, boxBg);
            fr.drawStringWithShadow(txt, -fr.getStringWidth(txt) / 2, -2, cfg.colored ? 0xFF38BDF8 : 0xFFFFFFFF);
        } else if ("CPS".equals(modName)) {
            int lcps = previewMode ? 12 : countCps(leftClicks);
            int rcps = previewMode ? 8 : countCps(rightClicks);
            String txt = "CPS: " + lcps + " | " + rcps;
            int w = fr.getStringWidth(txt) + 8;
            Gui.drawRect(-w / 2, -6, w / 2, 10, boxBg);
            fr.drawStringWithShadow(txt, -fr.getStringWidth(txt) / 2, -2, 0xFFFFFFFF);
        } else if ("Ping".equals(modName)) {
            int ping = 24;
            if (!previewMode && mc.getNetHandler() != null && mc.thePlayer != null) {
                NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
                if (info != null) {
                    ping = info.getResponseTime();
                }
            }
            String txt = "Ping: " + ping + " ms";
            int w = fr.getStringWidth(txt) + 8;
            Gui.drawRect(-w / 2, -6, w / 2, 10, boxBg);
            fr.drawStringWithShadow(txt, -fr.getStringWidth(txt) / 2, -2, cfg.colored ? 0xFF22C55E : 0xFFFFFFFF);
        } else if ("ToggleSprint".equals(modName)) {
            String state = previewMode ? "Toggled" : (sprintToggled ? "Toggled" : "Normal");
            String txt = "Sprint: " + state;
            int w = fr.getStringWidth(txt) + 8;
            Gui.drawRect(-w / 2, -6, w / 2, 10, boxBg);
            fr.drawStringWithShadow(txt, -fr.getStringWidth(txt) / 2, -2, cfg.colored ? 0xFF38BDF8 : 0xFFFFFFFF);
        } else if ("Title".equals(modName)) {
            String txt = "SwiftClient";
            int w = fr.getStringWidth(txt) + 12;
            Gui.drawRect(-w / 2, -7, w / 2, 11, boxBg);
            fr.drawStringWithShadow(txt, -fr.getStringWidth(txt) / 2, -2, cfg.colored ? 0xFF38BDF8 : 0xFFFFFFFF);
        } else if ("Armor Status".equals(modName)) {
            List<String> lines = new ArrayList<>();
            if (previewMode) {
                lines.add("Helm: 363/363");
                lines.add("Klata: 528/528");
                lines.add("Spodnie: 495/495");
                lines.add("Buty: 429/429");
                lines.add("Reka: 1561/1561");
            } else if (mc.thePlayer != null) {
                ItemStack[] arm = mc.thePlayer.inventory.armorInventory;
                String[] slots = {"Buty", "Spodnie", "Klata", "Helm"};
                for (int i = arm.length - 1; i >= 0; i--) {
                    ItemStack stack = arm[i];
                    if (stack != null && stack.isItemStackDamageable()) {
                        int rem = stack.getMaxDamage() - stack.getItemDamage();
                        lines.add(slots[i] + ": " + rem + "/" + stack.getMaxDamage());
                    }
                }
                ItemStack held = mc.thePlayer.getCurrentEquippedItem();
                if (held != null && held.isItemStackDamageable()) {
                    int rem = held.getMaxDamage() - held.getItemDamage();
                    lines.add("Reka: " + rem + "/" + held.getMaxDamage());
                }
            }
            int yOff = 0;
            for (String line : lines) {
                int w = fr.getStringWidth(line) + 8;
                Gui.drawRect(-w / 2, yOff - 3, w / 2, yOff + 9, boxBg);
                fr.drawStringWithShadow(line, -fr.getStringWidth(line) / 2, yOff, 0xFFFFFFFF);
                yOff += 13;
            }
        } else if ("Keystrokes".equals(modName)) {
            boolean wDown = previewMode || Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
            boolean aDown = previewMode || Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
            boolean sDown = previewMode || Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
            boolean dDown = previewMode || Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

            drawKeyBox(fr, "W", -9, -20, 18, 16, wDown, boxBg, cfg.colored);
            drawKeyBox(fr, "A", -29, -2, 18, 16, aDown, boxBg, cfg.colored);
            drawKeyBox(fr, "S", -9, -2, 18, 16, sDown, boxBg, cfg.colored);
            drawKeyBox(fr, "D", 11, -2, 18, 16, dDown, boxBg, cfg.colored);
            drawKeyBox(fr, "LMB", -29, 16, 28, 14, !previewMode && mc.gameSettings.keyBindAttack.isKeyDown(), boxBg, cfg.colored);
            drawKeyBox(fr, "RMB", 1, 16, 28, 14, !previewMode && mc.gameSettings.keyBindUseItem.isKeyDown(), boxBg, cfg.colored);
        } else if ("PotionEffect".equals(modName)) {
            List<String> lines = new ArrayList<>();
            if (!previewMode && mc.thePlayer != null) {
                Collection<PotionEffect> active = mc.thePlayer.getActivePotionEffects();
                for (PotionEffect pe : active) {
                    Potion potion = Potion.potionTypes[pe.getPotionID()];
                    if (potion != null) {
                        String pName = StatBase.translateFormal(potion.getName());
                        String dur = Potion.getDurationString(pe);
                        lines.add(pName + " " + (pe.getAmplifier() + 1) + " (" + dur + ")");
                    }
                }
            }
            if (lines.isEmpty() && previewMode) {
                lines.add("Speed II (0:45)");
            }
            int yOff = 0;
            for (String line : lines) {
                int w = fr.getStringWidth(line) + 8;
                Gui.drawRect(-w / 2, yOff - 4, w / 2, yOff + 10, boxBg);
                fr.drawStringWithShadow(line, -fr.getStringWidth(line) / 2, yOff, 0xFFFFFFFF);
                yOff += 15;
            }
        } else if ("TNT Timer".equals(modName)) {
            String timerText = null;
            if (previewMode) {
                timerText = "TNT: 2.4s";
            } else if (mc.theWorld != null) {
                double bestDist = Double.MAX_VALUE;
                for (Entity ent : mc.theWorld.loadedEntityList) {
                    if (ent instanceof EntityTNTPrimed) {
                        double d = ent.getDistanceSqToEntity(mc.thePlayer);
                        if (d < bestDist) {
                            bestDist = d;
                            float sec = Math.max(0.0F, ((EntityTNTPrimed) ent).fuse / 20.0F);
                            timerText = String.format("TNT: %.1fs", sec);
                        }
                    }
                }
            }
            if (timerText != null) {
                int w = fr.getStringWidth(timerText) + 10;
                int tntBg = cfg.background ? 0xAA1E0B0B : 0x00000000;
                Gui.drawRect(-w / 2, -7, w / 2, 11, tntBg);
                fr.drawStringWithShadow(timerText, -fr.getStringWidth(timerText) / 2, -2, cfg.colored ? 0xFFF87171 : 0xFFFFFFFF);
            }
        }

        GlStateManager.popMatrix();
    }

    private void drawKeyBox(
            FontRenderer fr,
            String label,
            int x,
            int y,
            int w,
            int h,
            boolean active,
            int idleBg,
            boolean colored
    ) {
        int activeBg = colored ? 0xCC0EA5E9 : 0xCC64748B;
        int bg = active ? activeBg : idleBg;
        Gui.drawRect(x, y, x + w, y + h, bg);
        fr.drawStringWithShadow(label, x + (w - fr.getStringWidth(label)) / 2, y + (h - 8) / 2 + 1, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public void onRenderSelfNametagInF5(RenderPlayerEvent.Specials.Pre event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP self = mc.thePlayer;
        if (self == null || event.entityPlayer != self || mc.gameSettings.thirdPersonView == 0) {
            return;
        }

        String tag = "[Swift] " + self.getName();
        FontRenderer fr = mc.fontRendererObj;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) event.x, (float) (event.y + self.height + 0.35D), (float) event.z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);

        float scale = 0.02666667F;
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int halfW = fr.getStringWidth(tag) / 2;
        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.disableTexture2D();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-halfW - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        wr.pos(-halfW - 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        wr.pos(halfW + 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        wr.pos(halfW + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tess.draw();
        GlStateManager.enableTexture2D();

        fr.drawString(tag, -halfW, 0, 0x20FFFFFF);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        fr.drawString(tag, -halfW, 0, -1);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
