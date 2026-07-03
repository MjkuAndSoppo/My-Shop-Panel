package com.example.myshoppanel.shop;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.economy.SimulateOfflineData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class TransactionService {

    public static class TransactionPreview {
        public final UUID playerUUID;
        public final double currentBalance;
        public final double newBalance;
        public final double cost;
        public final boolean willOverdraft;
        public final String description;

        public TransactionPreview(UUID playerUUID, double currentBalance, double newBalance,
                                  double cost, String description) {
            this.playerUUID = playerUUID;
            this.currentBalance = currentBalance;
            this.newBalance = newBalance;
            this.cost = cost;
            this.willOverdraft = newBalance < 0;
            this.description = description;
        }
    }

    public static TransactionPreview previewMarketBuy(ServerPlayer buyer, PlayerMarketListing listing) {
        UUID buyerUUID = buyer.getUUID();
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double current = points.getPoints(buyerUUID);
        double newBalance = current - listing.getPrice();
        String desc = "购买 §6" + listing.getItem().getDisplayName().getString()
                + " x" + listing.getItem().getCount()
                + " §f(卖家: §6" + listing.getSellerName() + "§f)";
        return new TransactionPreview(buyerUUID, current, newBalance, listing.getPrice(), desc);
    }

    /**
     * 执行市场购买。
     * @return 放入冗余仓库的数量（-1 = 失败）
     */
    public static int commitMarketBuy(ServerPlayer buyer, PlayerMarketListing listing,
                                       PlayerMarketSavedData marketData, int buyQty) {
        int stackCount = listing.getItem().getCount();
        int actualQty = Math.min(buyQty, stackCount);
        if (actualQty <= 0) return -1;

        // 按比例计算价格
        double unitPrice = ShopUtils.roundAmount(listing.getPrice() / stackCount);
        double totalCost = ShopUtils.roundAmount(unitPrice * actualQty);

        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double bal = points.getPoints(buyer.getUUID());
        if (bal < totalCost) return -1;

        // 扣款并转账
        points.cutPoints(buyer.getUUID(), totalCost);
        points.addPoints(listing.getSellerUUID(), totalCost);

        // 给予物品（背包满时进冗余仓库）
        ItemStack toGive = listing.getItem().copy();
        toGive.setCount(actualQty);
        int warehoused = ShopUtils.giveItemWithOverflow(buyer, toGive);

        if (actualQty >= stackCount) {
            // 整单售出，移除挂单
            marketData.removeListing(listing.getListingId());
        } else {
            // 部分售出，减少挂单数量并保留
            int remaining = stackCount - actualQty;
            double remainingPrice = ShopUtils.roundAmount(unitPrice * remaining);
            marketData.removeListing(listing.getListingId());
            ItemStack remainingItem = listing.getItem().copy();
            remainingItem.setCount(remaining);
            PlayerMarketListing newListing = new PlayerMarketListing(
                    UUID.randomUUID(),
                    listing.getSellerUUID(),
                    listing.getSellerName(),
                    remainingItem,
                    remainingPrice,
                    System.currentTimeMillis(),
                    listing.getDisplayId()
            );
            marketData.addListing(newListing);
        }
        return warehoused;
    }

    public static void commitMarketList(ServerPlayer seller, ItemStack item, double price,
                                        PlayerMarketSavedData marketData) {
        ItemStack toList = item.copy();
        seller.getInventory().clearOrCountMatchingItems(
                stack -> ItemStack.isSameItemSameTags(stack, item),
                item.getCount(),
                seller.getInventory()
        );
        int displayId = marketData.nextDisplayId();
        PlayerMarketListing listing = new PlayerMarketListing(
                UUID.randomUUID(),
                seller.getUUID(),
                seller.getName().getString(),
                toList,
                price,
                System.currentTimeMillis(),
                displayId
        );
        marketData.addListing(listing);
    }

    /**
     * 执行下架。只有卖家本人或管理员可操作。在线→退回背包(满则掉落)；离线→进入冗余仓库。
     * @return true=成功下架
     */
    public static boolean commitDelist(ServerPlayer player, int displayId,
                                       PlayerMarketSavedData marketData) {
        var listing = marketData.getListingByDisplayId(displayId);
        if (listing.isEmpty()) return false;
        PlayerMarketListing l = listing.get();
        // 权限检查：只有卖家本人或管理员可下架
        if (!player.getUUID().equals(l.getSellerUUID()) && !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[MyShopPanel] 你没有权限下架该报价单。"));
            return false;
        }
        var removed = marketData.removeListingByDisplayId(displayId);
        if (removed.isEmpty()) return false;
        return returnItemToPlayerOrWarehouse(player, removed.get().getItem(),
                l.getSellerUUID(), l.getSellerName());
    }

    /**
     * 将物品退回给指定玩家（在线→背包/掉落，离线→冗余仓库）。
     */
    public static boolean returnItemToPlayerOrWarehouse(ServerPlayer currentPlayer, ItemStack item,
                                                         UUID sellerUUID, String sellerName) {
        ServerPlayer seller = currentPlayer.serverLevel().getServer()
                .getPlayerList().getPlayer(sellerUUID);
        // 模拟离线测试模式
        boolean simulatedOffline = SimulateOfflineData.isSimulatedOffline(sellerUUID);
        if (seller != null && !simulatedOffline) {
            int warehoused = ShopUtils.giveItemWithOverflow(seller, item);
            seller.sendSystemMessage(Component.literal(
                    "§a[MyShopPanel] 你的报价单已被管理员下架，物品已退回背包。"));
            ShopUtils.sendWarehouseOverflowMsg(seller, warehoused,
                    item.getDisplayName().getString());
        } else {
            // 卖家离线，进入冗余仓库
            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(currentPlayer.serverLevel());
            warehouse.addItem(sellerUUID, item);
        }
        return true;
    }
}
