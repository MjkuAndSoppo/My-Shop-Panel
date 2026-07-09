package com.example.myshoppanel.command;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.S2C_EditModePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MSPEditCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MSPEdit")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(MSPEditCommands::setEditMode))
        );
    }

    private static int setEditMode(CommandContext<CommandSourceStack> ctx) {
        boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
        ServerPlayer player = ctx.getSource().getPlayer();

        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspedit.player_only"));
            return 0;
        }

        NetworkHandler.sendToPlayer(new S2C_EditModePacket(enabled), player);

        Component status = Component.translatable(enabled ? "my_shop_panel.cmd.mspedit.on" : "my_shop_panel.cmd.mspedit.off");
        ctx.getSource().sendSuccess(
                () -> Component.translatable("my_shop_panel.cmd.mspedit.toggled", status),
                true
        );

        return 1;
    }
}
