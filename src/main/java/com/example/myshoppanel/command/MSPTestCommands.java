package com.example.myshoppanel.command;

import com.example.myshoppanel.economy.SimulateOfflineData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /MSPtest 测试指令。
 * /MSPtest OnlineMode T/F  — 模拟玩家不在线状态，用于测试冗余仓库功能。
 */
public class MSPTestCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MSPtest")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("OnlineMode")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(MSPTestCommands::setOnlineMode)))
        );
    }

    private static int setOnlineMode(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        ServerPlayer player = ctx.getSource().getPlayer();

        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("§c[MSPtest] 该指令只能由玩家执行。"));
            return 0;
        }

        SimulateOfflineData.setSimulatedOffline(player.getUUID(), !enabled);

        String status = enabled
                ? "§a§l模拟在线（正常模式）"
                : "§c§l模拟离线（下架物品将进入冗余仓库）";
        ctx.getSource().sendSuccess(
                () -> Component.literal("§e[MSPtest] OnlineMode = " + status),
                true
        );
        return 1;
    }
}
