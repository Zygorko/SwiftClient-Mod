package com.swiftclient.mod;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

@Mod(modid = "swiftclienthud", name = "SwiftClient HUD", version = "1.0.0", clientSideOnly = true)
public class SwiftClientMod {

    private SwiftConfig config;
    private SwiftHudRenderer hudRenderer;
    private boolean rShiftWasDown = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        this.config = new SwiftConfig(mc.mcDataDir);
        this.hudRenderer = new SwiftHudRenderer(this.config);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(this.hudRenderer);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (down && !rShiftWasDown) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null && mc.currentScreen == null) {
                mc.displayGuiScreen(new SwiftMainMenuGui(config, hudRenderer));
            }
        }
        rShiftWasDown = down;
    }
}
