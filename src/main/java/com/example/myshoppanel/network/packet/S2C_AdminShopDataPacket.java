package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.client.ClientPacketHandlers;
import com.example.myshoppanel.shop.AdminShopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_AdminShopDataPacket {

    private static final int MAX_ENTRIES = 65536;

    private final List<AdminShopEntry> entries;
    private final double balance;

    public S2C_AdminShopDataPacket(List<AdminShopEntry> entries, double balance) {
        this.entries = entries != null ? entries : new ArrayList<>();
        this.balance = balance;
    }

    public List<AdminShopEntry> getEntries() { return entries; }
    public double getBalance() { return balance; }

    public static void encode(S2C_AdminShopDataPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entries.size());
        for (AdminShopEntry entry : msg.entries) {
            buf.writeUtf(entry.getEntryId());
            buf.writeUtf(entry.getItemRegistryName());
            buf.writeUtf(entry.getItemDisplayName());
            buf.writeDouble(entry.getPrice());
            buf.writeEnum(entry.getMode());
            buf.writeBoolean(entry.isInfiniteStock());
            buf.writeInt(entry.getStock());
        }
        buf.writeDouble(msg.balance);
    }

    public static S2C_AdminShopDataPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        if (size < 0 || size > MAX_ENTRIES) {
            return new S2C_AdminShopDataPacket(new ArrayList<>(), 0);
        }
        List<AdminShopEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String entryId = buf.readUtf();
            String itemRegistryName = buf.readUtf();
            String itemDisplayName = buf.readUtf();
            double price = buf.readDouble();
            AdminShopEntry.ShopMode mode = buf.readEnum(AdminShopEntry.ShopMode.class);
            boolean infiniteStock = buf.readBoolean();
            int stock = buf.readInt();
            entries.add(new AdminShopEntry(entryId, itemRegistryName, itemDisplayName,
                    price, mode, infiniteStock, stock));
        }
        double balance = buf.readDouble();
        return new S2C_AdminShopDataPacket(entries, balance);
    }

    public static void handle(S2C_AdminShopDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleAdminShopData(msg)));
        ctx.get().setPacketHandled(true);
    }
}
