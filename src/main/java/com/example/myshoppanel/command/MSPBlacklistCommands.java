package com.example.myshoppanel.command;

import com.example.myshoppanel.shop.MarketBlacklist;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * /MSPBlacklist add <物品注册名> | remove <物品注册名> | list
 * 需 OP 权限，管理玩家市场报价黑名单。
 */
public class MSPBlacklistCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MSPBlacklist")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(MSPBlacklistCommands::addItem)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(MSPBlacklistCommands::removeItem)))
                        .then(Commands.literal("list")
                                .executes(MSPBlacklistCommands::listItems))
        );
    }

    private static int addItem(CommandContext<CommandSourceStack> ctx) {
        MarketBlacklist mb = MarketBlacklist.getInstance();
        if (mb == null) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.not_init"));
            return 0;
        }

        String itemName = StringArgumentType.getString(ctx, "item");
        // 校验物品是否存在
        var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(itemName));
        if (item == null) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.item_not_found", itemName));
            return 0;
        }

        if (mb.add(itemName)) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("my_shop_panel.cmd.blacklist.added", itemName),
                    true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.already_in"));
            return 0;
        }
    }

    private static int removeItem(CommandContext<CommandSourceStack> ctx) {
        MarketBlacklist mb = MarketBlacklist.getInstance();
        if (mb == null) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.not_init"));
            return 0;
        }

        String itemName = StringArgumentType.getString(ctx, "item");

        if (mb.remove(itemName)) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("my_shop_panel.cmd.blacklist.removed", itemName),
                    true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.not_in"));
            return 0;
        }
    }

    private static int listItems(CommandContext<CommandSourceStack> ctx) {
        MarketBlacklist mb = MarketBlacklist.getInstance();
        if (mb == null) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.blacklist.not_init"));
            return 0;
        }

        var items = mb.getBlacklist();
        if (items.isEmpty()) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("my_shop_panel.cmd.blacklist.empty"),
                    false);
        } else {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("my_shop_panel.cmd.blacklist.list", String.join(", ", items)),
                    false);
        }
        return 1;
    }
}
