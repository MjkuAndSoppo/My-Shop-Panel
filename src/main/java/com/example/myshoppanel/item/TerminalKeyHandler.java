package com.example.myshoppanel.item;

import com.example.myshoppanel.MyShopPanel;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_OpenTerminalPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
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

        // 检查背包是否有报价终端
        boolean hasTerminal = false;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (stack.getItem() instanceof QuotationTerminalItem) {
                hasTerminal = true;
                break;
            }
        }

        if (hasTerminal) {
            // 通过发包让服务端打开主菜单（同步余额）
            NetworkHandler.sendToServer(new C2S_OpenTerminalPacket());
        }
    }
}
