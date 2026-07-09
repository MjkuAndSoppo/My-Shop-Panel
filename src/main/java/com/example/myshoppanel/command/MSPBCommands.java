package com.example.myshoppanel.command;

import com.example.myshoppanel.economy.SimulateOfflineData;
import com.example.myshoppanel.shop.PlayerMarketListing;
import com.example.myshoppanel.shop.PlayerMarketSavedData;
import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import com.example.myshoppanel.shop.ShopUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MSPBCommands {

    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("MSPB")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("up")
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                        .executes(ctx -> { doUpItem(ctx, 1); return 1; })
                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> { doUpItem(ctx, IntegerArgumentType.getInteger(ctx, "quantity")); return 1; }))))
                        .then(Commands.literal("dn")
                                .then(Commands.literal("all")
                                        .executes(MSPBCommands::delistAll))
                                .then(Commands.argument("from", IntegerArgumentType.integer(100))
                                        .executes(MSPBCommands::delistSingle)
                                        .then(Commands.argument("to", IntegerArgumentType.integer(100))
                                                .executes(MSPBCommands::delistRange))))
        );
    }

    private static void doUpItem(CommandContext<CommandSourceStack> ctx, int quantity) {
        try { upItem(ctx, quantity); } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.fail", e.getMessage()));
        }
    }

    private static void upItem(CommandContext<CommandSourceStack> ctx, int quantity) throws Exception {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.hold_item")); return; }
        // 不可堆叠物品强制数量为1
        final int listQty = held.getMaxStackSize() <= 1 ? 1 : quantity;
        double price = DoubleArgumentType.getDouble(ctx, "price");
        ItemStack toList = held.copy(); toList.setCount(listQty);
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, held)) { found += stack.getCount(); if (found >= listQty) break; }
        }
        if (found < listQty) { ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.not_enough", found)); return; }
        player.getInventory().clearOrCountMatchingItems(s -> ItemStack.isSameItemSameTags(s, held), listQty, player.getInventory());
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        int displayId = marketData.nextDisplayId();
        PlayerMarketListing listing = new PlayerMarketListing(UUID.randomUUID(), SERVER_UUID, "§b服务器", toList, price, System.currentTimeMillis(), displayId);
        marketData.addListing(listing);
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspb.listed", toList.getDisplayName().getString(), listQty, ShopUtils.fmt(price), displayId), true);
    }

    // ========== dn 子命令 ==========

    /** 下架单个报价单：/MSPB dn <id> */
    private static int delistSingle(CommandContext<CommandSourceStack> ctx) {
        int displayId = IntegerArgumentType.getInteger(ctx, "from");
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        Optional<PlayerMarketListing> found = marketData.removeListingByDisplayId(displayId);
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.not_found", displayId));
            return 0;
        }
        refundListing(ctx, found.get());
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspb.delisted", displayId,
                found.get().getItem().getDisplayName().getString()), true);
        return 1;
    }

    /** 下架范围报价单：/MSPB dn <from> <to> */
    private static int delistRange(CommandContext<CommandSourceStack> ctx) {
        int from = IntegerArgumentType.getInteger(ctx, "from");
        int to = IntegerArgumentType.getInteger(ctx, "to");
        if (from > to) {
            int tmp = from; from = to; to = tmp;
        }
        final int fFrom = from, fTo = to;
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        List<PlayerMarketListing> removed = marketData.removeListingsByDisplayIdRange(from, to);
        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.range_empty", fFrom, fTo));
            return 0;
        }
        for (PlayerMarketListing listing : removed) {
            refundListing(ctx, listing);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspb.range_delisted", removed.size(), fFrom, fTo), true);
        return removed.size();
    }

    /** 下架所有报价单：/MSPB dn all */
    private static int delistAll(CommandContext<CommandSourceStack> ctx) {
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        List<PlayerMarketListing> removed = marketData.removeAllListings();
        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("my_shop_panel.cmd.mspb.none"));
            return 0;
        }
        for (PlayerMarketListing listing : removed) {
            refundListing(ctx, listing);
        }
        final int count = removed.size();
        ctx.getSource().sendSuccess(() -> Component.translatable("my_shop_panel.cmd.mspb.all_delisted", count), true);
        return count;
    }

    /** 退款：在线退回背包/掉落，离线放入冗余仓库 */
    private static void refundListing(CommandContext<CommandSourceStack> ctx, PlayerMarketListing listing) {
        ItemStack item = listing.getItem();
        ServerPlayer seller = ctx.getSource().getLevel().getServer()
                .getPlayerList().getPlayer(listing.getSellerUUID());
        boolean simulatedOffline = SimulateOfflineData.isSimulatedOffline(listing.getSellerUUID());
        if (seller != null && !simulatedOffline) {
            if (!seller.getInventory().add(item)) {
                seller.drop(item, false);
            }
            seller.sendSystemMessage(Component.translatable("my_shop_panel.cmd.mspb.admin_delisted", listing.getDisplayId()));
        } else {
            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(ctx.getSource().getLevel());
            warehouse.addItem(listing.getSellerUUID(), item);
        }
    }
}
