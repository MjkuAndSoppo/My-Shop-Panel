package com.example.myshoppanel.compat.ftbquests;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * FTB Quests MSPP 点数奖励。
 * 完成任务时给予指定数量的 MSPP 点数。
 *
 * 使用方式（需 FTB Quests 依赖）：
 * 1. 在 FTB Quests 编辑器中创建奖励时选择 "MSPP Points"
 * 2. 设置奖励金额
 * 3. 玩家完成任务后自动获得 MSPP
 */
public class MSPPReward {

    private final long amount;

    public MSPPReward() {
        this.amount = 100;
    }

    public MSPPReward(long amount) {
        this.amount = amount;
    }

    public void reward(ServerPlayer player) {
        if (player == null) return;
        MSPPointsSavedData points = MSPPointsSavedData.get(player.serverLevel());
        points.addPoints(player.getUUID(), amount);
    }
}