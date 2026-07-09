package com.example.myshoppanel.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_PriceItemListPacket {

    private final List<String> itemIds;

    public S2C_PriceItemListPacket(List<String> itemIds) {
        this.itemIds = itemIds;
    }

    public S2C_PriceItemListPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.itemIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int len = buf.readVarInt();
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            this.itemIds.add(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    public static void encode(S2C_PriceItemListPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.itemIds.size());
        for (String id : msg.itemIds) {
            byte[] bytes = id.getBytes(StandardCharsets.UTF_8);
            buf.writeVarInt(bytes.length);
            buf.writeBytes(bytes);
        }
    }

    public static S2C_PriceItemListPacket decode(FriendlyByteBuf buf) {
        return new S2C_PriceItemListPacket(buf);
    }

    public static void handle(S2C_PriceItemListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> PriceItemListCache.set(msg.itemIds));
        ctx.get().setPacketHandled(true);
    }

    public static class PriceItemListCache {
        private static List<String> cached = List.of();
        public static List<String> get() { return cached; }
        public static void set(List<String> list) { cached = list; }
    }
}