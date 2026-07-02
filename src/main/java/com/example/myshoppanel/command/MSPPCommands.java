package com.example.myshoppanel.command;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.shop.ShopUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class MSPPCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("msp")
                        .then(Commands.literal("pay")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                                .executes(MSPPCommands::payPoints))))
                        .then(Commands.literal("get")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(MSPPCommands::getPoints)))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(MSPPCommands::setPoints))))
                        .then(Commands.literal("add")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(MSPPCommands::addPoints))))
                        .then(Commands.literal("cut")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(MSPPCommands::cutPoints))))
        );
    }

    private static int payPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer fromPlayer = ctx.getSource().getPlayerOrException();
        ServerPlayer toPlayer = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");

        if (fromPlayer.getUUID().equals(toPlayer.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("§c[MS.P.Points] 你不能给自己转账！"));
            return 0;
        }
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double fromBalance = data.getPoints(fromPlayer.getUUID());
        if (fromBalance < amount) {
            ctx.getSource().sendFailure(Component.literal("§c[MS.P.Points] 余额不足！当前余额: §6" + ShopUtils.fmt(fromBalance)));
            return 0;
        }
        data.cutPoints(fromPlayer.getUUID(), amount);
        data.addPoints(toPlayer.getUUID(), amount);

        fromPlayer.sendSystemMessage(Component.literal("§e[MS.P.Points] §f已向 §6" + toPlayer.getName().getString()
                + " §f转账 §6" + ShopUtils.fmt(amount) + " §f，剩余余额: §6" + ShopUtils.fmt(data.getPoints(fromPlayer.getUUID()))));
        toPlayer.sendSystemMessage(Component.literal("§e[MS.P.Points] §6" + fromPlayer.getName().getString()
                + " §f向你转账了 §6" + ShopUtils.fmt(amount) + " §f，当前余额: §6" + ShopUtils.fmt(data.getPoints(toPlayer.getUUID()))));
        return 1;
    }

    private static int getPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double points = data.getPoints(uuid);
        ctx.getSource().sendSuccess(() -> Component.literal("§e[MS.P.Points] §f" + target.getName().getString()
                + " §a余额: §6" + ShopUtils.fmt(points)), false);
        return 1;
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double old = data.getPoints(uuid);
        data.setPoints(uuid, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§e[MS.P.Points] §f已将 " + target.getName().getString()
                + " 的点数设为 §6" + ShopUtils.fmt(amount) + "§f（原余额: §6" + ShopUtils.fmt(old) + "§f）"), true);
        return 1;
    }

    private static int addPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double newBalance = data.addPoints(uuid, amount);
        ctx.getSource().sendSuccess(() -> Component.literal("§e[MS.P.Points] §f已给 " + target.getName().getString()
                + " 增加 §6" + ShopUtils.fmt(amount) + " §f点（余额: §6" + ShopUtils.fmt(newBalance) + "§f）"), true);
        return 1;
    }

    private static int cutPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double oldPoints = data.getPoints(uuid);
        double newBalance = data.cutPoints(uuid, amount);
        String warning = newBalance < 0 ? " §c⚠ 该玩家已产生透支/欠款！" : "";
        final String fw = warning;
        ctx.getSource().sendSuccess(() -> Component.literal("§e[MS.P.Points] §f已从 " + target.getName().getString()
                + " 扣除 §6" + ShopUtils.fmt(amount) + " §f点。当前余额: §6" + ShopUtils.fmt(newBalance)
                + "§f（原余额: §6" + ShopUtils.fmt(oldPoints) + "§f）" + fw), true);
        return 1;
    }
}
