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
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.msptest.player_only"));
            return 0;
        }

        SimulateOfflineData.setSimulatedOffline(player.getUUID(), !enabled);

        Component status = Component.translatable(enabled ? "my_shop_panel.cmd.msptest.online" : "my_shop_panel.cmd.msptest.offline");
        ctx.getSource().sendSuccess(
                () -> Component.translatable("my_shop_panel.cmd.msptest.toggled", status),
                true
        );
        return 1;
    }
}
