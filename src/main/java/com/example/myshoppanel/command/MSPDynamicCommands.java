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
                .then(Commands.literal("category")
                        .executes(MSPDynamicCommands::categoryList)
                        .then(Commands.literal("set")
                                .then(Commands.argument("tabId", StringArgumentType.word())
                                        .then(Commands.literal("weight")
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
                                                        .executes(MSPDynamicCommands::categorySetWeight)))
                                        .then(Commands.literal("cost")
                                                .then(Commands.argument("restock", IntegerArgumentType.integer(1, 1000000))
                                                        .then(Commands.argument("listing", IntegerArgumentType.integer(1, 10000000))
                                                                .executes(MSPDynamicCommands::categorySetCost))))
                                        .then(Commands.literal("off")
                                                .executes(MSPDynamicCommands::categoryOff))
                                        .then(Commands.literal("on")
                                                .executes(MSPDynamicCommands::categoryOn)))))
        );
    }

    private static int turnOn(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "my_shop_panel.cmd.dynamic.not_init");
            return 0;
        }
        cfg.setEnabled(true);
        send(ctx, "my_shop_panel.cmd.dynamic.enabled");
        return 1;
    }

    private static int turnOff(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "my_shop_panel.cmd.dynamic.not_init");
            return 0;
        }
        cfg.setEnabled(false);
        send(ctx, "my_shop_panel.cmd.dynamic.disabled");
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "my_shop_panel.cmd.dynamic.not_init");
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

        send(ctx, "my_shop_panel.cmd.dynamic.status_header");
        String statusKey = cfg.isEnabled() ? "my_shop_panel.cmd.dynamic.status_running" : "my_shop_panel.cmd.dynamic.status_stopped";
        send(ctx, "my_shop_panel.cmd.dynamic.status_line", Component.translatable(statusKey));
        send(ctx, "my_shop_panel.cmd.dynamic.interval", cfg.getTickInterval(), cfg.getTickInterval() / 20);
        send(ctx, "my_shop_panel.cmd.dynamic.bot_balance", ShopUtils.fmt(botBalance));
        send(ctx, "my_shop_panel.cmd.dynamic.min_retained", ShopUtils.fmt(cfg.getMinRetainedFunds()));
        send(ctx, "my_shop_panel.cmd.dynamic.current_listings", botListings, cfg.getMaxListings());
        send(ctx, "my_shop_panel.cmd.dynamic.max_attempts", cfg.getMaxListingItemCount());
        send(ctx, "my_shop_panel.cmd.dynamic.restock_max", cfg.getRestockMaxCount());
        send(ctx, "my_shop_panel.cmd.dynamic.quote_count", QuoteGroupData.size());
        send(ctx, "my_shop_panel.cmd.dynamic.fee_rate", String.format("%.0f%%", ListingFeeCalculator.getFeeRate() * 100));
        send(ctx, "my_shop_panel.cmd.dynamic.markup", String.format("%.1fx", ListingFeeCalculator.getMaxMarkupPenalty()));
        send(ctx, "my_shop_panel.cmd.dynamic.bulk_discount", String.format("%.1fx", ListingFeeCalculator.getMinBulkDiscount()));
        send(ctx, "my_shop_panel.cmd.dynamic.footer");
        return 1;
    }

    private static int setInterval(CommandContext<CommandSourceStack> ctx) {
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null) {
            send(ctx, "my_shop_panel.cmd.dynamic.not_init");
            return 0;
        }
        cfg.setTickInterval(ticks);
        send(ctx, "my_shop_panel.cmd.dynamic.interval_set", ticks, ticks / 20);
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
        DynamicCategoryConfig.loadInstance(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
        QuoteGroupData.loadInstance(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
        send(ctx, "my_shop_panel.cmd.dynamic.reloaded");
        return 1;
    }

    private static int clearQuotes(CommandContext<CommandSourceStack> ctx) {
        int count = QuoteGroupData.size();
        QuoteGroupData.clear();
        send(ctx, "my_shop_panel.cmd.dynamic.quotes_cleared", count);
        return 1;
    }

    private static int feeStatus(CommandContext<CommandSourceStack> ctx) {
        send(ctx, "my_shop_panel.cmd.dynamic.fee_header");
        String feeStatusKey = ListingFeeCalculator.isFeeEnabled() ? "my_shop_panel.cmd.dynamic.fee_on" : "my_shop_panel.cmd.dynamic.fee_off";
        send(ctx, "my_shop_panel.cmd.dynamic.fee_status_line", Component.translatable(feeStatusKey));
        send(ctx, "my_shop_panel.cmd.dynamic.fee_rate", String.format("%.0f%%", ListingFeeCalculator.getFeeRate() * 100));
        send(ctx, "my_shop_panel.cmd.dynamic.markup", String.format("%.1fx", ListingFeeCalculator.getMaxMarkupPenalty()));
        send(ctx, "my_shop_panel.cmd.dynamic.bulk_discount", String.format("%.1fx", ListingFeeCalculator.getMinBulkDiscount()));
        send(ctx, "my_shop_panel.cmd.dynamic.fee_per_item", String.format("%.2f", ListingFeeCalculator.getBulkDiscountPerItem()));
        send(ctx, "my_shop_panel.cmd.dynamic.footer");
        return 1;
    }

    private static int feeOff(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(false);
        ListingFeeCalculator.save();
        send(ctx, "my_shop_panel.cmd.dynamic.fee_disabled");
        return 1;
    }

    private static int feeOn(CommandContext<CommandSourceStack> ctx) {
        ListingFeeCalculator.setFeeEnabled(true);
        ListingFeeCalculator.save();
        send(ctx, "my_shop_panel.cmd.dynamic.fee_enabled");
        return 1;
    }

    private static int setFeeRate(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setFeeRate(v);
        ListingFeeCalculator.save();
        send(ctx, "my_shop_panel.cmd.dynamic.fee_rate_set", String.format("%.0f%%", v * 100));
        return 1;
    }

    private static int setMarkup(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMaxMarkupPenalty(v);
        ListingFeeCalculator.save();
        send(ctx, "my_shop_panel.cmd.dynamic.markup_set", String.format("%.1fx", v));
        return 1;
    }

    private static int setBulk(CommandContext<CommandSourceStack> ctx) {
        double v = DoubleArgumentType.getDouble(ctx, "value");
        ListingFeeCalculator.setMinBulkDiscount(v);
        ListingFeeCalculator.save();
        send(ctx, "my_shop_panel.cmd.dynamic.bulk_set", String.format("%.1fx", v));
        return 1;
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String key, Object... args) {
        ctx.getSource().sendSuccess(() -> Component.translatable(key, args), false);
    }

    // ===== 分类配置命令 =====

    private static int categoryList(CommandContext<CommandSourceStack> ctx) {
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();
        if (catCfg == null) {
            send(ctx, "my_shop_panel.cmd.category.not_init");
            return 0;
        }
        var cats = catCfg.getAll();
        send(ctx, "my_shop_panel.cmd.category.header");
        if (cats.isEmpty()) {
            send(ctx, "my_shop_panel.cmd.category.empty");
        } else {
            for (DynamicCategoryConfig.CategoryConfig c : cats) {
                String statusKey = c.enabled ? "my_shop_panel.cmd.category.status_enabled" : "my_shop_panel.cmd.category.status_disabled";
                String status = Component.translatable(statusKey).getString();
                send(ctx, "my_shop_panel.cmd.category.entry", c.creativeTabId,
                        String.format("%.1f", c.weight), c.restockCost, c.listingCost, status);
            }
        }
        send(ctx, "my_shop_panel.cmd.dynamic.footer");
        return 1;
    }

    private static int categorySetWeight(CommandContext<CommandSourceStack> ctx) {
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();
        if (catCfg == null) {
            send(ctx, "my_shop_panel.cmd.category.not_init");
            return 0;
        }
        String tabId = StringArgumentType.getString(ctx, "tabId");
        double weight = DoubleArgumentType.getDouble(ctx, "value");
        catCfg.setWeight(tabId, weight);
        send(ctx, "my_shop_panel.cmd.category.weight_set", tabId, String.format("%.1f", weight));
        return 1;
    }

    private static int categorySetCost(CommandContext<CommandSourceStack> ctx) {
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();
        if (catCfg == null) {
            send(ctx, "my_shop_panel.cmd.category.not_init");
            return 0;
        }
        String tabId = StringArgumentType.getString(ctx, "tabId");
        int restock = IntegerArgumentType.getInteger(ctx, "restock");
        int listing = IntegerArgumentType.getInteger(ctx, "listing");
        catCfg.setCost(tabId, restock, listing);
        send(ctx, "my_shop_panel.cmd.category.cost_set", tabId, restock, listing);
        return 1;
    }

    private static int categoryOff(CommandContext<CommandSourceStack> ctx) {
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();
        if (catCfg == null) {
            send(ctx, "my_shop_panel.cmd.category.not_init");
            return 0;
        }
        String tabId = StringArgumentType.getString(ctx, "tabId");
        catCfg.setEnabled(tabId, false);
        send(ctx, "my_shop_panel.cmd.category.disabled", tabId);
        return 1;
    }

    private static int categoryOn(CommandContext<CommandSourceStack> ctx) {
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();
        if (catCfg == null) {
            send(ctx, "my_shop_panel.cmd.category.not_init");
            return 0;
        }
        String tabId = StringArgumentType.getString(ctx, "tabId");
        catCfg.setEnabled(tabId, true);
        send(ctx, "my_shop_panel.cmd.category.enabled", tabId);
        return 1;
    }
}
