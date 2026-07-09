package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.AdminShopConfig;
import com.example.myshoppanel.shop.AdminShopEntry;
import com.example.myshoppanel.shop.PlayerMarketListing;
import com.example.myshoppanel.shop.PlayerMarketSavedData;
import com.example.myshoppanel.shop.ShopUtils;
import com.example.myshoppanel.shop.TransactionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class C2S_ConfirmTransactionPacket {

    public enum TransactionType {
        MARKET_BUY, MARKET_LIST, ADMIN_BUY, ADMIN_SELL, ADMIN_BUYBACK
    }

    private static final UUID ZERO_UUID = new UUID(0, 0);

    private final TransactionType type;
    private final UUID listingId;
    private final double price;
    private final int quantity;
    private final String adminEntryId;

    /** 市场购买（整单购买，兼容旧逻辑） */
    public C2S_ConfirmTransactionPacket(UUID listingId) {
        this.type = TransactionType.MARKET_BUY;
        this.listingId = listingId;
        this.price = 0;
        this.quantity = -1; // -1 表示整单购买
        this.adminEntryId = null;
    }

    /** 市场购买（指定数量） */
    public C2S_ConfirmTransactionPacket(UUID listingId, int quantity) {
        this.type = TransactionType.MARKET_BUY;
        this.listingId = listingId;
        this.price = 0;
        this.quantity = quantity;
        this.adminEntryId = null;
    }

    public C2S_ConfirmTransactionPacket(TransactionType type, UUID listingId, double price, int quantity, String adminEntryId) {
        this.type = type;
        this.listingId = listingId;
        this.price = price;
        this.quantity = quantity;
        this.adminEntryId = adminEntryId;
    }

    public static void encode(C2S_ConfirmTransactionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeUUID(msg.listingId != null ? msg.listingId : ZERO_UUID);
        buf.writeDouble(msg.price);
        buf.writeInt(msg.quantity);
        buf.writeUtf(msg.adminEntryId != null ? msg.adminEntryId : "");
    }

    public static C2S_ConfirmTransactionPacket decode(FriendlyByteBuf buf) {
        TransactionType type = buf.readEnum(TransactionType.class);
        UUID listingId = buf.readUUID();
        double price = buf.readDouble();
        int quantity = buf.readInt();
        String adminEntryId = buf.readUtf();
        return new C2S_ConfirmTransactionPacket(type,
                listingId.equals(ZERO_UUID) ? null : listingId,
                price, quantity,
                adminEntryId.isEmpty() ? null : adminEntryId);
    }

    public static void handle(C2S_ConfirmTransactionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 负数防护
            if (msg.quantity <= 0 || msg.price < 0) {
                player.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.invalid_params"));
                return;
            }

            switch (msg.type) {
                case MARKET_BUY -> handleMarketBuy(player, msg.listingId, msg.quantity);
                case MARKET_LIST -> player.sendSystemMessage(Component.translatable(
                        "my_shop_panel.tx.msg.list_via_gui"));
                case ADMIN_BUY -> handleAdminBuy(player, msg.adminEntryId, msg.quantity);
                case ADMIN_SELL -> handleAdminSell(player, msg.adminEntryId, msg.quantity);
                case ADMIN_BUYBACK -> handleAdminBuyback(player, msg.adminEntryId, msg.quantity, msg.price);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleMarketBuy(ServerPlayer buyer, UUID listingId, int quantity) {
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(buyer.serverLevel());
        PlayerMarketListing listing = marketData.getListing(listingId).orElse(null);
        if (listing == null) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.listing_gone"));
            return;
        }
        if (listing.getItem().isEmpty()) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.item_data_error"));
            return;
        }

        int stackCount = listing.getItem().getCount();
        int buyQty = (quantity <= 0) ? stackCount : Math.min(quantity, stackCount);

        // 按比例计算价格（精度0.01）
        double unitPrice = ShopUtils.roundAmount(listing.getPrice() / stackCount);
        double totalCost = ShopUtils.roundAmount(unitPrice * buyQty);

        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double bal = points.getPoints(buyer.getUUID());
        if (bal < totalCost) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.insufficient_balance",
                    ShopUtils.fmt(totalCost), ShopUtils.fmt(bal)));
            return;
        }

        int warehoused = TransactionService.commitMarketBuy(buyer, listing, marketData, buyQty);
        if (warehoused >= 0) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.success"));
            ShopUtils.sendWarehouseOverflowMsg(buyer, warehoused,
                    listing.getItem().getDisplayName().getString());
        } else {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.race_condition"));
        }
    }

    private static void handleAdminBuy(ServerPlayer buyer, String entryId, int quantity) {
        if (entryId == null) return;
        if (quantity <= 0) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.invalid_qty"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.item_gone"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.SELLING) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.entry_not_buyable"));
            return;
        }
        // 库存充足性检查：非无限库存时，库存必须 >= 购买数量
        if (!entry.isInfiniteStock() && entry.getStock() < quantity) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.insufficient_stock", entry.getStock()));
            return;
        }
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double totalCost = ShopUtils.roundAmount(entry.getPrice() * quantity);
        if (points.getPoints(buyer.getUUID()) < totalCost) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.insufficient_balance",
                    ShopUtils.fmt(totalCost), ShopUtils.fmt(points.getPoints(buyer.getUUID()))));
            return;
        }
        points.cutPoints(buyer.getUUID(), totalCost);
        ItemStack item = getItemStack(entry.getItemRegistryName());
        int warehoused = 0;
        if (!item.isEmpty()) {
            item.setCount(quantity);
            warehoused = ShopUtils.giveItemWithOverflow(buyer, item);
        }
        if (!entry.isInfiniteStock()) {
            entry.setStock(entry.getStock() - quantity);
            config.save();
        }
        // 100% 利润注入机器人账户
        com.example.myshoppanel.shop.DynamicSystemService.injectBotFunds(
                buyer.serverLevel(), totalCost, "世界商店出售-" + entry.getItemDisplayName());
        buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.buy_success",
                entry.getItemDisplayName(), quantity));
        ShopUtils.sendWarehouseOverflowMsg(buyer, warehoused,
                item.isEmpty() ? Component.translatable("my_shop_panel.misc.item").getString() : item.getDisplayName().getString());
        NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                points.getPoints(buyer.getUUID())), buyer);
    }

    private static final int MAX_TRANSACTION_QTY = 999;

    private static void handleAdminSell(ServerPlayer seller, String entryId, int quantity) {
        if (entryId == null) return;
        if (quantity <= 0) {
            seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.invalid_qty"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.item_gone"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.BUYING) {
            seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.entry_not_sellable"));
            return;
        }
        ItemStack toSell = getItemStack(entry.getItemRegistryName());
        if (toSell.isEmpty()) {
            seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.item_data_error"));
            return;
        }
        int qty = Math.min(quantity, MAX_TRANSACTION_QTY);
        int found = 0;
        for (ItemStack stack : seller.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, toSell)) {
                found += stack.getCount();
                if (found >= qty) break;
            }
        }
        if (found < qty) {
            seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.backpack_low"));
            return;
        }
        // 库存溢出防护 — 在改库存和扣物品之前
        if (!entry.isInfiniteStock()) {
            long newStock = (long) entry.getStock() + qty;
            if (newStock > Integer.MAX_VALUE) {
                seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.stock_full"));
                return;
            }
        }
        seller.getInventory().clearOrCountMatchingItems(
                s -> ItemStack.isSameItemSameTags(s, toSell), qty, seller.getInventory());
        double totalEarn = ShopUtils.roundAmount(entry.getPrice() * qty);
        MSPPointsSavedData points = MSPPointsSavedData.get(seller.serverLevel());
        points.addPoints(seller.getUUID(), totalEarn);
        if (!entry.isInfiniteStock()) {
            entry.setStock(entry.getStock() + qty);
            config.save();
        }
        seller.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.sell_success",
                entry.getItemDisplayName(), qty, ShopUtils.fmt(totalEarn)));
        NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                points.getPoints(seller.getUUID())), seller);
    }

    private static void handleAdminBuyback(ServerPlayer buyer, String entryId, int quantity, double clientPrice) {
        if (entryId == null) return;
        if (quantity <= 0) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.invalid_params"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.item_gone"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.BUYING) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.entry_not_buyback"));
            return;
        }
        if (!entry.isInfiniteStock() && entry.getStock() < quantity) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.insufficient_stock_buyback"));
            return;
        }
        // 服务端独立计算1.3倍买回价，不信任客户端price
        double buybackUnitPrice = Math.ceil(entry.getPrice() * 1.3 * 100) / 100;
        double totalCost = ShopUtils.roundAmount(buybackUnitPrice * quantity);
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        if (points.getPoints(buyer.getUUID()) < totalCost) {
            buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.insufficient_balance",
                    ShopUtils.fmt(totalCost), ShopUtils.fmt(points.getPoints(buyer.getUUID()))));
            return;
        }
        points.cutPoints(buyer.getUUID(), totalCost);
        ItemStack item = getItemStack(entry.getItemRegistryName());
        int warehousedBb = 0;
        if (!item.isEmpty()) {
            item.setCount(quantity);
            warehousedBb = ShopUtils.giveItemWithOverflow(buyer, item);
        }
        if (!entry.isInfiniteStock()) {
            entry.setStock(entry.getStock() - quantity);
            config.save();
        }
        // 25% 买回差价注入机器人账户
        double profit = ShopUtils.roundAmount((buybackUnitPrice - entry.getPrice()) * quantity);
        com.example.myshoppanel.shop.DynamicSystemService.injectBotFunds(
                buyer.serverLevel(), profit, "世界商店买回-" + entry.getItemDisplayName());
        buyer.sendSystemMessage(Component.translatable("my_shop_panel.tx.msg.buyback_success",
                entry.getItemDisplayName(), quantity, ShopUtils.fmt(totalCost)));
        ShopUtils.sendWarehouseOverflowMsg(buyer, warehousedBb,
                item.isEmpty() ? Component.translatable("my_shop_panel.misc.item").getString() : item.getDisplayName().getString());
        NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                points.getPoints(buyer.getUUID())), buyer);
    }

    private static ItemStack getItemStack(String registryName) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(registryName);
            if (rl != null) {
                var item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) return new ItemStack(item);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}
