package com.example.myshoppanel.compat.jei;

import com.example.myshoppanel.MyShopPanel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.InterModComms;

/**
 * JEI 集成 — 通过 InterModComms 向 JEI 注册报价终端信息。
 * 无需硬依赖 JEI API，运行时自动检测。
 */
public class JEIIntegration {

    public static void sendIMC() {
        // 向 JEI 注册报价终端为信息物品
        ItemStack terminal = new ItemStack(MyShopPanel.QUOTATION_TERMINAL.get());
        InterModComms.sendTo("jei", "addItem", () -> terminal);
    }
}