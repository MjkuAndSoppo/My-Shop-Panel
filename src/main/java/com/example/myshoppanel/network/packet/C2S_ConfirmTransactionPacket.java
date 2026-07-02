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

    public C2S_ConfirmTransactionPacket(UUID listingId) {
        this.type = TransactionType.MARKET_BUY;
        this.listingId = listingId;
        this.price = 0;
        this.quantity = 1;
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
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 无效的交易参数。"));
                return;
            }

            switch (msg.type) {
                case MARKET_BUY -> handleMarketBuy(player, msg.listingId);
                case MARKET_LIST -> player.sendSystemMessage(Component.literal(
                        "§e[MyShopPanel] 上架请通过上架界面操作。"));
                case ADMIN_BUY -> handleAdminBuy(player, msg.adminEntryId, msg.quantity);
                case ADMIN_SELL -> handleAdminSell(player, msg.adminEntryId, msg.quantity);
                case ADMIN_BUYBACK -> handleAdminBuyback(player, msg.adminEntryId, msg.quantity, msg.price);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleMarketBuy(ServerPlayer buyer, UUID listingId) {
        PlayerMarketSavedData marketData = PlayerMarketSavedData.get(buyer.serverLevel());
        PlayerMarketListing listing = marketData.getListing(listingId).orElse(null);
        if (listing == null) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该挂单已不存在或已售出。"));
            return;
        }
        if (listing.getItem().isEmpty()) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该挂单物品数据异常。"));
            return;
        }
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double bal = points.getPoints(buyer.getUUID());
        if (bal < listing.getPrice()) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 余额不足！需要 §6"
                    + ShopUtils.fmt(listing.getPrice()) + "§c，当前余额: §6" + ShopUtils.fmt(bal)));
            return;
        }
        boolean ok = TransactionService.commitMarketBuy(buyer, listing, marketData);
        if (ok) {
            buyer.sendSystemMessage(Component.literal("§a[MyShopPanel] 交易成功！"));
        } else {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 交易失败：该挂单可能已被抢先购买，已退款。"));
        }
    }

    private static void handleAdminBuy(ServerPlayer buyer, String entryId, int quantity) {
        if (entryId == null) return;
        if (quantity <= 0) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 数量无效。"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该物品已不存在。"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.SELLING) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该条目不支持购买。"));
            return;
        }
        // 库存充足性检查：非无限库存时，库存必须 >= 购买数量
        if (!entry.isInfiniteStock() && entry.getStock() < quantity) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 库存不足！当前库存: §6" + entry.getStock()));
            return;
        }
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        double totalCost = ShopUtils.roundAmount(entry.getPrice() * quantity);
        if (points.getPoints(buyer.getUUID()) < totalCost) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 余额不足！需要 §6"
                    + ShopUtils.fmt(totalCost) + "§c，当前余额: §6"
                    + ShopUtils.fmt(points.getPoints(buyer.getUUID()))));
            return;
        }
        points.cutPoints(buyer.getUUID(), totalCost);
        ItemStack item = getItemStack(entry.getItemRegistryName());
        if (!item.isEmpty()) {
            item.setCount(quantity);
            if (!buyer.getInventory().add(item)) buyer.drop(item, false);
        }
        if (!entry.isInfiniteStock()) {
            entry.setStock(entry.getStock() - quantity);
            config.save();
        }
        buyer.sendSystemMessage(Component.literal("§a[MyShopPanel] 购买成功！获得了 §6"
                + entry.getItemDisplayName() + " x" + quantity));
        NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                points.getPoints(buyer.getUUID())), buyer);
    }

    private static final int MAX_TRANSACTION_QTY = 999;

    private static void handleAdminSell(ServerPlayer seller, String entryId, int quantity) {
        if (entryId == null) return;
        if (quantity <= 0) {
            seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 数量无效。"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 该物品已不存在。"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.BUYING) {
            seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 该条目不支持出售。"));
            return;
        }
        ItemStack toSell = getItemStack(entry.getItemRegistryName());
        if (toSell.isEmpty()) {
            seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 物品数据异常。"));
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
            seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 背包中该物品不足！"));
            return;
        }
        // 库存溢出防护 — 在改库存和扣物品之前
        if (!entry.isInfiniteStock()) {
            long newStock = (long) entry.getStock() + qty;
            if (newStock > Integer.MAX_VALUE) {
                seller.sendSystemMessage(Component.literal("§c[MyShopPanel] 库存已达上限！"));
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
        seller.sendSystemMessage(Component.literal("§a[MyShopPanel] 卖出 §6"
                + entry.getItemDisplayName() + " x" + qty + " §a获得 §6"
                + ShopUtils.fmt(totalEarn)));
        NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                points.getPoints(seller.getUUID())), seller);
    }

    private static void handleAdminBuyback(ServerPlayer buyer, String entryId, int quantity, double clientPrice) {
        if (entryId == null) return;
        if (quantity <= 0) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 无效的交易参数。"));
            return;
        }
        AdminShopConfig config = AdminShopConfig.getInstance();
        if (config == null) return;
        AdminShopEntry entry = config.getEntry(entryId);
        if (entry == null) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该物品已不存在。"));
            return;
        }
        if (entry.getMode() != AdminShopEntry.ShopMode.BUYING) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 该条目不支持买回。"));
            return;
        }
        if (!entry.isInfiniteStock() && entry.getStock() < quantity) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 库存不足，无法买回！"));
            return;
        }
        // 服务端独立计算1.3倍买回价，不信任客户端price
        double buybackUnitPrice = Math.ceil(entry.getPrice() * 1.3 * 100) / 100;
        double totalCost = ShopUtils.roundAmount(buybackUnitPrice * quantity);
        MSPPointsSavedData points = MSPPointsSavedData.get(buyer.serverLevel());
        if (points.getPoints(buyer.getUUID()) < totalCost) {
            buyer.sendSystemMessage(Component.literal("§c[MyShopPanel] 余额不足！需要 §6"
                    + ShopUtils.fmt(totalCost) + "§c，当前余额: §6"
                    + ShopUtils.fmt(points.getPoints(buyer.getUUID()))));
            return;
        }
        points.cutPoints(buyer.getUUID(), totalCost);
        ItemStack item = getItemStack(entry.getItemRegistryName());
        if (!item.isEmpty()) {
            item.setCount(quantity);
            if (!buyer.getInventory().add(item)) buyer.drop(item, false);
        }
        if (!entry.isInfiniteStock()) {
            entry.setStock(entry.getStock() - quantity);
            config.save();
        }
        buyer.sendSystemMessage(Component.literal("§a[MyShopPanel] 以§c1.3倍§a价格买回 §6"
                + entry.getItemDisplayName() + " x" + quantity + " §a花费 §6"
                + ShopUtils.fmt(totalCost)));
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
