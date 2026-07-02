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
                                .then(Commands.argument("id", IntegerArgumentType.integer(100))
                                        .executes(MSPBCommands::downItem)))
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

    private static int downItem(CommandContext<CommandSourceStack> ctx) {
        int displayId = IntegerArgumentType.getInteger(ctx, "id");
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(ctx.getSource().getLevel());
        Optional<PlayerMarketListing> found = marketData.removeListingByDisplayId(displayId);
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c[MSPB] 报价单 #" + displayId + " 不存在。"));
            return 0;
        }
        PlayerMarketListing listing = found.get();
        ItemStack item = listing.getItem();

        // 查找卖家是否在线（考虑模拟离线测试）
        ServerPlayer seller = ctx.getSource().getLevel().getServer()
                .getPlayerList().getPlayer(listing.getSellerUUID());
        boolean simulatedOffline = SimulateOfflineData.isSimulatedOffline(listing.getSellerUUID());
        if (seller != null && !simulatedOffline) {
            // 在线：退回背包或掉落
            if (!seller.getInventory().add(item)) {
                seller.drop(item, false);
            }
            seller.sendSystemMessage(Component.literal(
                    "§a[MSPB] 你的报价单 #" + displayId + " 已被管理员下架，物品已退回背包。"));
        } else {
            // 离线：进入冗余仓库
            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(ctx.getSource().getLevel());
            warehouse.addItem(listing.getSellerUUID(), item);
        }

        ctx.getSource().sendSuccess(() -> Component.literal("§a[MSPB] 报价单 #" + displayId + " §f(§6"
                + listing.getItem().getDisplayName().getString() + "§f) 已下架，物品已退回。"), true);
        return 1;
    }
}
