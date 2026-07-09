package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.shop.TransactionRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_TransactionHistoryPacket {

    private final List<TransactionRecord> records;

    public S2C_TransactionHistoryPacket(List<TransactionRecord> records) {
        this.records = records;
    }

    public S2C_TransactionHistoryPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.records = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            TransactionRecord r = new TransactionRecord();
            r.type = TransactionRecord.Type.values()[buf.readVarInt()];
            r.itemName = readString(buf);
            r.itemId = readString(buf);
            r.quantity = buf.readVarInt();
            r.totalPrice = buf.readDouble();
            r.unitPrice = buf.readDouble();
            r.counterparty = readString(buf);
            r.timestamp = buf.readLong();
            this.records.add(r);
        }
    }

    public static void encode(S2C_TransactionHistoryPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.records.size());
        for (TransactionRecord r : msg.records) {
            buf.writeVarInt(r.type.ordinal());
            writeString(buf, r.itemName);
            writeString(buf, r.itemId);
            buf.writeVarInt(r.quantity);
            buf.writeDouble(r.totalPrice);
            buf.writeDouble(r.unitPrice);
            writeString(buf, r.counterparty);
            buf.writeLong(r.timestamp);
        }
    }

    public static S2C_TransactionHistoryPacket decode(FriendlyByteBuf buf) {
        return new S2C_TransactionHistoryPacket(buf);
    }

    public static void handle(S2C_TransactionHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> TransactionHistoryCache.set(msg.records));
        ctx.get().setPacketHandled(true);
    }

    private static void writeString(FriendlyByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        buf.writeVarInt(bytes.length);
        buf.writeBytes(bytes);
    }

    private static String readString(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** 客户端缓存，UI 读取 */
    public static class TransactionHistoryCache {
        private static List<TransactionRecord> cached = new ArrayList<>();
        public static List<TransactionRecord> get() { return cached; }
        public static void set(List<TransactionRecord> list) { cached = list; }
    }
}