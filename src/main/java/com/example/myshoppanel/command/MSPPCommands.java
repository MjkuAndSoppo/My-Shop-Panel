package com.example.myshoppanel.command;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.S2C_OpenMenuPacket;
import com.example.myshoppanel.shop.ListingFeeCalculator;
import com.example.myshoppanel.shop.ServerConfig;
import com.example.myshoppanel.shop.ShopUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                        .then(Commands.literal("open")
                                .executes(MSPPCommands::openShop))
                        .then(Commands.literal("openmod")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("t");
                                            builder.suggest("f");
                                            return builder.buildFuture();
                                        })
                                        .executes(MSPPCommands::setOpenMode)))
                        .then(Commands.literal("fee")
                                .requires(src -> src.hasPermission(2))
                                .executes(MSPPCommands::feeStatus)
                                .then(Commands.literal("on")
                                        .executes(MSPPCommands::feeOn))
                                .then(Commands.literal("off")
                                        .executes(MSPPCommands::feeOff))
                                .then(Commands.literal("rate")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01, 1.0))
                                                .executes(MSPPCommands::setFeeRate)))
                                .then(Commands.literal("markup")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                                                .executes(MSPPCommands::setMarkup)))
                                .then(Commands.literal("bulk")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 1.0))
                                                .executes(MSPPCommands::setBulk))))
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
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspp.self_transfer"));
            return 0;
        }
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double fromBalance = data.getPoints(fromPlayer.getUUID());
        if (fromBalance < amount) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspp.insufficient", ShopUtils.fmt(fromBalance)));
            return 0;
        }
        data.cutPoints(fromPlayer.getUUID(), amount);
        data.addPoints(toPlayer.getUUID(), amount);

        fromPlayer.sendSystemMessage(Component.translatable("my_shop_panel.cmd.mspp.transfer_sent",
                ShopUtils.fmt(amount), toPlayer.getName().getString(), ShopUtils.fmt(data.getPoints(fromPlayer.getUUID()))));
        toPlayer.sendSystemMessage(Component.translatable("my_shop_panel.cmd.mspp.transfer_received",
                fromPlayer.getName().getString(), ShopUtils.fmt(amount), ShopUtils.fmt(data.getPoints(toPlayer.getUUID()))));
        return 1;
    }

    private static int getPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double points = data.getPoints(uuid);
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspp.balance",
                target.getName().getString(), ShopUtils.fmt(points)), false);
        return 1;
    }

    private static int setPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double old = data.getPoints(uuid);
        data.setPoints(uuid, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspp.set",
                target.getName().getString(), ShopUtils.fmt(amount), ShopUtils.fmt(old)), true);
        return 1;
    }

    private static int addPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double newBalance = data.addPoints(uuid, amount);
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspp.add",
                ShopUtils.fmt(amount), target.getName().getString(), ShopUtils.fmt(newBalance)), true);
        return 1;
    }

    private static int cutPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        double amount = DoubleArgumentType.getDouble(ctx, "amount");
        UUID uuid = target.getUUID();
        MSPPointsSavedData data = MSPPointsSavedData.get(ctx.getSource().getLevel());
        double oldPoints = data.getPoints(uuid);
        double newBalance = data.cutPoints(uuid, amount);
        Component warning = newBalance < 0 ? Component.translatable("my_shop_panel.cmd.mspp.overdraft_warn") : Component.empty();
        final Component fw = warning;
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspp.cut",
                ShopUtils.fmt(amount), target.getName().getString(), ShopUtils.fmt(newBalance), ShopUtils.fmt(oldPoints))
                .append(fw), true);
        return 1;
    }

    /** /msp open — 直接打开商店主菜单 */
    private static int openShop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        double balance = MSPPointsSavedData.get(player.serverLevel()).getPoints(player.getUUID());
        NetworkHandler.sendToPlayer(
                new S2C_OpenMenuPacket(S2C_OpenMenuPacket.MenuType.MAIN_MENU, balance), player);
        return 1;
    }

    /** /msp openmod t|f — 设置 I 键打开商店是否需要拥有报价终端 */
    private static int setOpenMode(CommandContext<CommandSourceStack> ctx) {
        String value = StringArgumentType.getString(ctx, "value");
        boolean require;
        if (value.equalsIgnoreCase("t") || value.equalsIgnoreCase("true")) {
            require = true;
        } else if (value.equalsIgnoreCase("f") || value.equalsIgnoreCase("false")) {
            require = false;
        } else {
            ctx.getSource().sendFailure(Component.literal("§c参数只能是 t 或 f"));
            return 0;
        }
        ServerConfig.setRequireTerminalForHotkey(require);
        ServerConfig.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aI 键打开商店" + (require ? "§e需要" : "§e不需要") + "§a拥有报价终端"), true);
        return 1;
    }

    /** /msp fee — 上架手续费状态 */
    private static int feeStatus(CommandContext<CommandSourceStack> ctx) {
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_header");
        String feeStatusKey = ListingFeeCalculator.isFeeEnabled()
                ? "my_shop_panel.cmd.dynamic.fee_on" : "my_shop_panel.cmd.dynamic.fee_off";
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_status_line", Component.translatable(feeStatusKey));
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_rate", String.format("%.0f%%", ListingFeeCalculator.getFeeRate() * 100));
        feeSend(ctx, "my_shop_panel.cmd.dynamic.markup", String.format("%.1fx", ListingFeeCalculator.getMaxMarkupPenalty()));
        feeSend(ctx, "my_shop_panel.cmd.dynamic.bulk_discount", String.format("%.1fx", ListingFeeCalculator.getMinBulkDiscount()));
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_per_item", String.format("%.2f", ListingFeeCalculator.getBulkDiscountPerItem()));
        feeSend(ctx, "my_shop_panel.cmd.dynamic.footer");
        return 1;
    }

    private static int feeOn(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(true);
        ListingFeeCalculator.save();
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_enabled");
        return 1;
    }

    private static int feeOff(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(false);
        ListingFeeCalculator.save();
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_disabled");
        return 1;
    }

    private static int setFeeRate(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setFeeRate(v);
        ListingFeeCalculator.save();
        feeSend(ctx, "my_shop_panel.cmd.dynamic.fee_rate_set", String.format("%.0f%%", v * 100));
        return 1;
    }

    private static int setMarkup(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMaxMarkupPenalty(v);
        ListingFeeCalculator.save();
        feeSend(ctx, "my_shop_panel.cmd.dynamic.markup_set", String.format("%.1fx", v));
        return 1;
    }

    private static int setBulk(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMinBulkDiscount(v);
        ListingFeeCalculator.save();
        feeSend(ctx, "my_shop_panel.cmd.dynamic.bulk_set", String.format("%.1fx", v));
        return 1;
    }

    private static void feeSend(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key, args), false);
    }
}
