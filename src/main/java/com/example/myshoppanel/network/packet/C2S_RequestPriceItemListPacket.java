package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.QuoteGroupData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class C2S_RequestPriceItemListPacket {

    public C2S_RequestPriceItemListPacket() {}

    public C2S_RequestPriceItemListPacket(FriendlyByteBuf buf) {}

    public static void encode(C2S_RequestPriceItemListPacket msg, FriendlyByteBuf buf) {}

    public static C2S_RequestPriceItemListPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestPriceItemListPacket();
    }

    public static void handle(C2S_RequestPriceItemListPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Set<String> ids = QuoteGroupData.getHistoryItemIds();
            NetworkHandler.sendToPlayer(new S2C_PriceItemListPacket(new ArrayList<>(ids)), player);
        });
        ctx.get().setPacketHandled(true);
    }
}