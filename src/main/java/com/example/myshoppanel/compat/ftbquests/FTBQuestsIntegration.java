package com.example.myshoppanel.compat.ftbquests;

import net.minecraftforge.fml.InterModComms;

/**
 * FTB Quests 集成 — 向 FTB Quests 注册 MSPP 点数奖励。
 * 通过 InterModComms 发送奖励工厂，无需硬依赖 FTB Quests API。
 */
public class FTBQuestsIntegration {

    public static void sendIMC() {
        // 向 FTB Quests 注册 MSPP 奖励类型
        InterModComms.sendTo("ftbquests", "registerReward",
                () -> "my_shop_panel:msp_points");
    }
}