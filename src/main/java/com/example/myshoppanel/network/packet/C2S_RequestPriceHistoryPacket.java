package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.QuoteGroupData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2S_RequestPriceHistoryPacket {

    private final String itemId;

    public C2S_RequestPriceHistoryPacket(String itemId) {
        this.itemId = itemId;
    }

    public C2S_RequestPriceHistoryPacket(FriendlyByteBuf buf) {
        int len = buf.readVarInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        this.itemId = new String(bytes, StandardCharsets.UTF_8);
    }

    public static void encode(C2S_RequestPriceHistoryPacket msg, FriendlyByteBuf buf) {
        byte[] bytes = msg.itemId.getBytes(StandardCharsets.UTF_8);
        buf.writeVarInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static C2S_RequestPriceHistoryPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestPriceHistoryPacket(buf);
    }

    public static void handle(C2S_RequestPriceHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            List<QuoteGroupData.PriceSnapshot> history = QuoteGroupData.getHistory(msg.itemId);
            NetworkHandler.sendToPlayer(new S2C_PriceHistoryPacket(msg.itemId, history), player);
        });
        ctx.get().setPacketHandled(true);
    }
}