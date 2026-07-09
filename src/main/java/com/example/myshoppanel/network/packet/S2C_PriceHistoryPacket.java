package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.shop.QuoteGroupData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_PriceHistoryPacket {

    private final String itemId;
    private final List<QuoteGroupData.PriceSnapshot> snapshots;

    public S2C_PriceHistoryPacket(String itemId, List<QuoteGroupData.PriceSnapshot> snapshots) {
        this.itemId = itemId;
        this.snapshots = snapshots;
    }

    public S2C_PriceHistoryPacket(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        this.itemId = new String(bytes, StandardCharsets.UTF_8);
        int size = buf.readVarInt();
        this.snapshots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.snapshots.add(new QuoteGroupData.PriceSnapshot(buf.readDouble(), buf.readLong()));
        }
    }

    public static void encode(S2C_PriceHistoryPacket msg, FriendlyByteBuf buf) {
        byte[] bytes = msg.itemId.getBytes(StandardCharsets.UTF_8);
        buf.writeVarInt(bytes.length);
        buf.writeBytes(bytes);
        buf.writeVarInt(msg.snapshots.size());
        for (QuoteGroupData.PriceSnapshot s : msg.snapshots) {
            buf.writeDouble(s.price);
            buf.writeLong(s.timestamp);
        }
    }

    public static S2C_PriceHistoryPacket decode(FriendlyByteBuf buf) {
        return new S2C_PriceHistoryPacket(buf);
    }

    public static void handle(S2C_PriceHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PriceCache.set(msg.itemId, msg.snapshots));
        ctx.get().setPacketHandled(true);
    }

    public static class PriceCache {
        private static String itemId = "";
        private static List<QuoteGroupData.PriceSnapshot> cached = List.of();
        public static String getItemId() { return itemId; }
        public static List<QuoteGroupData.PriceSnapshot> get() { return cached; }
        public static void set(String id, List<QuoteGroupData.PriceSnapshot> list) {
            itemId = id;
            cached = list;
        }
    }
}