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
            ctx.getSource().sendFailure(Component.literal("§c[MSPB] 指令执行失败: " + e.getMessage()));
        }
    }

    private static void upItem(CommandContext<CommandSourceStack> ctx, int quantity) throws Exception {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) { ctx.getSource().sendFailure(Component.literal("§c[MSPB] 请手持要上架的物品。")); return; }
        // 不可堆叠物品强制数量为1
        final int listQty = held.getMaxStackSize() <= 1 ? 1 : quantity;
        double price = DoubleArgumentType.getDouble(ctx, "price");
        ItemStack toList = held.copy(); toList.setCount(listQty);
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, held)) { found += stack.getCount(); if (found >= listQty) break; }
        }
        if (found < listQty) { ctx.getSource().sendFailure(Component.literal("§c[MSPB] 背包中物品不足，你有 " + found + " 个。")); return; }
        player.getInventory().clearOrCountMatchingItems(s -> ItemStack.isSameItemSameTags(s, held), listQty, player.getInventory());
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        int displayId = marketData.nextDisplayId();
        PlayerMarketListing listing = new PlayerMarketListing(UUID.randomUUID(), SERVER_UUID, "§b服务器", toList, price, System.currentTimeMillis(), displayId);
        marketData.addListing(listing);
        ctx.getSource().sendSuccess(() -> Component.literal("§a[MSPB] 已上架 §6" + toList.getDisplayName().getString() + " x" + listQty + " §f标价 §6" + ShopUtils.fmt(price) + " §f报价单ID: §e#" + displayId), true);
    }

    // ========== dn 子命令 ==========

    /** 下架单个报价单：/MSPB dn <id> */
    private static int delistSingle(CommandContext<CommandSourceStack> ctx) {
        int displayId = IntegerArgumentType.getInteger(ctx, "from");
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        Optional<PlayerMarketListing> found = marketData.removeListingByDisplayId(displayId);
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c[MSPB] 报价单 #" + displayId + " 不存在。"));
            return 0;
        }
        refundListing(ctx, found.get());
        ctx.getSource().sendSuccess(() -> Component.literal("§a[MSPB] 报价单 #" + displayId + " §f(§6"
                + found.get().getItem().getDisplayName().getString() + "§f) 已下架，物品已退回。"), true);
        return 1;
    }

    /** 下架范围报价单：/MSPB dn <from> <to> */
    private static int delistRange(CommandContext<CommandSourceStack> ctx) {
        int from = IntegerArgumentType.getInteger(ctx, "from");
        int to = IntegerArgumentType.getInteger(ctx, "to");
        if (from > to) {
            int tmp = from; from = to; to = tmp;
        }
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        List<PlayerMarketListing> removed = marketData.removeListingsByDisplayIdRange(from, to);
        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c[MSPB] 范围 #" + from + "~#" + to + " 内没有报价单。"));
            return 0;
        }
        for (PlayerMarketListing listing : removed) {
            refundListing(ctx, listing);
        }
        final int fFrom = from, fTo = to;
        ctx.getSource().sendSuccess(() -> Component.literal("§a[MSPB] 已下架 #" + fFrom + "~#" + fTo + " 范围内的 " + removed.size() + " 个报价单，物品已退回。"), true);
        return removed.size();
    }

    /** 下架所有报价单：/MSPB dn all */
    private static int delistAll(CommandContext<CommandSourceStack> ctx) {
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        List<PlayerMarketListing> removed = marketData.removeAllListings();
        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c[MSPB] 当前没有任何报价单。"));
            return 0;
        }
        for (PlayerMarketListing listing : removed) {
            refundListing(ctx, listing);
        }
        final int count = removed.size();
        ctx.getSource().sendSuccess(() -> Component.literal("§a[MSPB] 已下架全部 " + count + " 个报价单，物品已退回。"), true);
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
            seller.sendSystemMessage(Component.literal(
                    "§a[MSPB] 你的报价单 #" + listing.getDisplayId() + " 已被管理员下架，物品已退回背包。"));
        } else {
            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(ctx.getSource().getLevel());
            warehouse.addItem(listing.getSellerUUID(), item);
        }
    }
}
