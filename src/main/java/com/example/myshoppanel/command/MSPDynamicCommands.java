package com.example.myshoppanel.command;

import com.example.myshoppanel.shop.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /mspdynamic 指令集 — 报价组行情与动态分类配置。
 */
public class MSPDynamicCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mspdynamic")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(MSPDynamicCommands::reload))
                .then(Commands.literal("clearquotes")
                        .executes(MSPDynamicCommands::clearQuotes))
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

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();

        AdminShopConfig.loadInstance();
        MarketBlacklist.loadInstance();
        ListingFeeCalculator.load(src.getServer().getServerDirectory().toPath().resolve("config").resolve("my_shop_panel"));
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
