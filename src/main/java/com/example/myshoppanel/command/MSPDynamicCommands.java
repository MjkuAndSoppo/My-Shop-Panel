package com.example.myshoppanel.command;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.shop.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * /mspdynamic 指令集 — 控制动态系统（智能机器人）。
 */
public class MSPDynamicCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mspdynamic")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("on")
                        .executes(MSPDynamicCommands::turnOn))
                .then(Commands.literal("off")
                        .executes(MSPDynamicCommands::turnOff))
                .then(Commands.literal("status")
                        .executes(MSPDynamicCommands::status))
                .then(Commands.literal("interval")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(20, 72000))
                                .executes(MSPDynamicCommands::setInterval)))
                .then(Commands.literal("reload")
                        .executes(MSPDynamicCommands::reload))
                .then(Commands.literal("clearquotes")
                        .executes(MSPDynamicCommands::clearQuotes))
                .then(Commands.literal("fee")
                        .executes(MSPDynamicCommands::feeStatus)
                        .then(Commands.literal("off")
                                .executes(MSPDynamicCommands::feeOff))
                        .then(Commands.literal("on")
                                .executes(MSPDynamicCommands::feeOn))
                        .then(Commands.literal("rate")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01, 1.0))
                                        .executes(MSPDynamicCommands::setFeeRate)))
                        .then(Commands.literal("markup")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                                        .executes(MSPDynamicCommands::setMarkup)))
                        .then(Commands.literal("bulk")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 1.0))
                                        .executes(MSPDynamicCommands::setBulk))))
        );
    }

    private static int turnOn(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "§c动态系统未初始化。");
            return 0;
        }
        cfg.setEnabled(true);
        send(ctx, "§a动态系统已开启。");
        return 1;
    }

    private static int turnOff(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "§c动态系统未初始化。");
            return 0;
        }
        cfg.setEnabled(false);
        send(ctx, "§e动态系统已关闭。");
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "§c动态系统未初始化。");
            return 0;
        }
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        MSPPointsSavedData points = MSPPointsSavedData.get(level);
        double botBalance = points.getPoints(DynamicSystemData.BOT_UUID);
        PlayerMarketSavedData market = PlayerMarketSavedData.get(level);
        long botListings = market.getAllListings().stream()
                .filter(l -> l.getSellerUUID().equals(DynamicSystemData.BOT_UUID))
                .count();

        send(ctx, "§6========== 动态系统状态 ==========");
        send(ctx, "  状态: " + (cfg.isEnabled() ? "§a运行中" : "§c已关闭"));
        send(ctx, "  巡检间隔: §e" + cfg.getTickInterval() + "§f tick (" + (cfg.getTickInterval() / 20) + "秒)");
        send(ctx, "  机器人资金: §6" + ShopUtils.fmt(botBalance));
        send(ctx, "  最低留存: §6" + ShopUtils.fmt(cfg.getMinRetainedFunds()));
        send(ctx, "  当前挂单: §b" + botListings + "§f / " + cfg.getMaxListings());
        send(ctx, "  每轮最多上架: §e" + cfg.getMaxListingItemCount() + "§f 次尝试");
        send(ctx, "  补货随机上限: §e" + cfg.getRestockMaxCount() + "§f 个");
        send(ctx, "  报价组条目: §e" + QuoteGroupData.size() + "§f 种");
        send(ctx, "  手续费率: §e" + String.format("%.0f%%", ListingFeeCalculator.getFeeRate() * 100));
        send(ctx, "  涨价惩罚上限: §e" + String.format("%.1fx", ListingFeeCalculator.getMaxMarkupPenalty()));
        send(ctx, "  批量折扣下限: §e" + String.format("%.1fx", ListingFeeCalculator.getMinBulkDiscount()));
        send(ctx, "§6==================================");
        return 1;
    }

    private static int setInterval(CommandContext<CommandSourceStack> ctx) {
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "§c动态系统未初始化。");
            return 0;
        }
        cfg.setTickInterval(ticks);
        send(ctx, "§a巡检间隔已设为 " + ticks + " tick (" + (ticks / 20) + "秒)。");
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg != null) cfg.save();

        AdminShopConfig.loadInstance();
        MarketBlacklist.loadInstance();
        ListingFeeCalculator.load(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
        DynamicSystemData.loadInstance(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
        QuoteGroupData.loadInstance(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
        send(ctx, "§a动态系统配置已重载。");
        return 1;
    }

    private static int clearQuotes(CommandContext<CommandSourceStack> ctx) {
        int count = QuoteGroupData.size();
        QuoteGroupData.clear();
        send(ctx, "§a报价组已清空，共删除 §6" + count + " §a条记录。");
        return 1;
    }

    private static int feeStatus(CommandContext<CommandSourceStack> ctx) {
        send(ctx, "§6========== 手续费配置 ==========");
        send(ctx, "  状态: " + (ListingFeeCalculator.isFeeEnabled() ? "§a开启" : "§c关闭"));
        send(ctx, "  手续费率: §e" + String.format("%.0f%%", ListingFeeCalculator.getFeeRate() * 100));
        send(ctx, "  涨价惩罚上限: §e" + String.format("%.1fx", ListingFeeCalculator.getMaxMarkupPenalty()));
        send(ctx, "  批量折扣下限: §e" + String.format("%.1fx", ListingFeeCalculator.getMinBulkDiscount()));
        send(ctx, "  每件折扣: §e" + String.format("%.2f", ListingFeeCalculator.getBulkDiscountPerItem()));
        send(ctx, "§6==================================");
        return 1;
    }

    private static int feeOff(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(false);
        ListingFeeCalculator.save();
        send(ctx, "§e手续费已关闭，上架不再收取手续费。");
        return 1;
    }

    private static int feeOn(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(true);
        ListingFeeCalculator.save();
        send(ctx, "§a手续费已开启。");
        return 1;
    }

    private static int setFeeRate(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setFeeRate(v);
        ListingFeeCalculator.save();
        send(ctx, "§a手续费率已设为 " + String.format("%.0f%%", v * 100));
        return 1;
    }

    private static int setMarkup(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMaxMarkupPenalty(v);
        ListingFeeCalculator.save();
        send(ctx, "§a涨价惩罚上限已设为 " + String.format("%.1fx", v));
        return 1;
    }

    private static int setBulk(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMinBulkDiscount(v);
        ListingFeeCalculator.save();
        send(ctx, "§a批量折扣下限已设为 " + String.format("%.1fx", v));
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
}
