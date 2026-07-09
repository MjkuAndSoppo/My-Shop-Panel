package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.TransactionHistoryData;
import com.example.myshoppanel.shop.TransactionRecord;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class C2S_RequestTransactionHistoryPacket {

    public C2S_RequestTransactionHistoryPacket() {}

    public C2S_RequestTransactionHistoryPacket(FriendlyByteBuf buf) {}

    public static void encode(C2S_RequestTransactionHistoryPacket msg, FriendlyByteBuf buf) {}

    public static C2S_RequestTransactionHistoryPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestTransactionHistoryPacket();
    }

    public static void handle(C2S_RequestTransactionHistoryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            TransactionHistoryData data = TransactionHistoryData.get(player.serverLevel());
            List<TransactionRecord> records = data.getRecords(player.getUUID());
            NetworkHandler.sendToPlayer(new S2C_TransactionHistoryPacket(records), player);
        });
        ctx.get().setPacketHandled(true);
    }
}