package com.example.myshoppanel.item;

import com.example.myshoppanel.MyShopPanel;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_OpenTerminalPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MyShopPanel.MODID, value = Dist.CLIENT)
public class TerminalKeyHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getKey() != TerminalKeyMapping.OPEN_TERMINAL.getKey().getValue()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // 是否需要拥有报价终端由服务端配置决定，客户端直接请求服务端打开
        NetworkHandler.sendToServer(new C2S_OpenTerminalPacket());
    }
}
