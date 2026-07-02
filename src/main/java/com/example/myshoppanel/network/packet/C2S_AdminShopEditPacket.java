package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.AdminShopConfig;
import com.example.myshoppanel.shop.AdminShopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class C2S_AdminShopEditPacket {

    public enum EditType {
        ADD, REMOVE, STOCK_ADJUST, TOGGLE_STOCK, TOGGLE_MODE
    }

    private final EditType type;
    private final String entryId;
    private final double amount;
    private final String itemRegName;
    private final String itemDisplayName;
    private final int stockQty;

    public C2S_AdminShopEditPacket(String itemRegName, String itemDisplayName, double price, int stockQty) {
        this.type = EditType.ADD;
        this.entryId = null;
        this.amount = price;
        this.itemRegName = itemRegName;
        this.itemDisplayName = itemDisplayName;
        this.stockQty = stockQty;
    }

    public C2S_AdminShopEditPacket(EditType type, String entryId) {
        this.type = type;
        this.entryId = entryId;
        this.amount = 0;
        this.itemRegName = null;
        this.itemDisplayName = null;
        this.stockQty = 0;
    }

    public C2S_AdminShopEditPacket(EditType type, String entryId, int delta) {
        this.type = type;
        this.entryId = entryId;
        this.amount = delta;
        this.itemRegName = null;
        this.itemDisplayName = null;
        this.stockQty = 0;
    }

    public static void encode(C2S_AdminShopEditPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeUtf(msg.entryId != null ? msg.entryId : "");
        // STOCK_ADJUST 的 amount 写入int而非double，避免double→int截断问题
        if (msg.type == EditType.STOCK_ADJUST) {
            buf.writeInt((int) msg.amount);
        } else {
            buf.writeDouble(msg.amount);
        }
        buf.writeUtf(msg.itemRegName != null ? msg.itemRegName : "");
        buf.writeUtf(msg.itemDisplayName != null ? msg.itemDisplayName : "");
        buf.writeInt(msg.stockQty);
    }

    public static C2S_AdminShopEditPacket decode(FriendlyByteBuf buf) {
        EditType type = buf.readEnum(EditType.class);
        String entryId = buf.readUtf();
        double amount;
        if (type == EditType.STOCK_ADJUST) {
            amount = buf.readInt();
        } else {
            amount = buf.readDouble();
        }
        String regName = buf.readUtf();
        String displayName = buf.readUtf();
        int stockQty = buf.readInt();

        if (type == EditType.ADD) {
            return new C2S_AdminShopEditPacket(
                    regName.isEmpty() ? null : regName,
                    displayName.isEmpty() ? null : displayName,
                    amount, stockQty);
        } else if (type == EditType.STOCK_ADJUST) {
            return new C2S_AdminShopEditPacket(type,
                    entryId.isEmpty() ? null : entryId, (int) amount);
        } else {
            return new C2S_AdminShopEditPacket(type,
                    entryId.isEmpty() ? null : entryId);
        }
    }

    public static void handle(C2S_AdminShopEditPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!player.hasPermissions(2)) return;

            AdminShopConfig config = AdminShopConfig.getInstance();
            if (config == null) return;

            switch (msg.type) {
                case ADD -> {
                    if (msg.itemRegName != null && msg.itemDisplayName != null) {
                        AdminShopEntry entry = new AdminShopEntry(
                                UUID.randomUUID().toString(),
                                msg.itemRegName,
                                msg.itemDisplayName,
                                Math.max(0.01, msg.amount),
                                AdminShopEntry.ShopMode.SELLING,
                                false,
                                Math.max(0, msg.stockQty)
                        );
                        config.addEntry(entry);
                    }
                }
                case REMOVE -> {
                    if (msg.entryId != null) config.removeEntry(msg.entryId);
                }
                case STOCK_ADJUST -> {
                    if (msg.entryId != null) {
                        AdminShopEntry entry = config.getEntry(msg.entryId);
                        if (entry != null && !entry.isInfiniteStock()) {
                            int delta = (int) msg.amount;
                            if (delta != 0) {
                                long newStock = (long) entry.getStock() + delta;
                                if (newStock > Integer.MAX_VALUE) newStock = Integer.MAX_VALUE;
                                if (newStock < 0) newStock = 0;
                                entry.setStock((int) newStock);
                                config.save();
                            }
                        }
                    }
                }
                case TOGGLE_STOCK -> {
                    if (msg.entryId != null) {
                        AdminShopEntry entry = config.getEntry(msg.entryId);
                        if (entry != null) {
                            entry.setInfiniteStock(!entry.isInfiniteStock());
                            config.save();
                        }
                    }
                }
                case TOGGLE_MODE -> {
                    if (msg.entryId != null) {
                        AdminShopEntry entry = config.getEntry(msg.entryId);
                        if (entry != null) {
                            entry.setMode(entry.getMode() == AdminShopEntry.ShopMode.SELLING
                                    ? AdminShopEntry.ShopMode.BUYING
                                    : AdminShopEntry.ShopMode.SELLING);
                            config.save();
                        }
                    }
                }
            }

            NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(config.getAllEntries(),
                    MSPPointsSavedData.get(player.serverLevel()).getPoints(player.getUUID())), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
