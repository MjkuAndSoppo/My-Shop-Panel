package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.PlayerMarketListing;
import com.example.myshoppanel.shop.PlayerMarketSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求玩家市场挂单数据。
 * 服务端查询后通过 S2C_MarketDataPacket 回复。
 */
public class C2S_RequestMarketDataPacket {

    public C2S_RequestMarketDataPacket() {}

    public static void encode(C2S_RequestMarketDataPacket msg, FriendlyByteBuf buf) {
        // 空包
    }

    public static C2S_RequestMarketDataPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestMarketDataPacket();
    }

    public static void handle(C2S_RequestMarketDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerMarketSavedData marketData = PlayerMarketSavedData.get(player.serverLevel());
            List<PlayerMarketListing> allListings = marketData.getAllListings();
            List<PlayerMarketListing> myListings = marketData.getPlayerListings(player.getUUID());

            // 回传数据（含余额）
            NetworkHandler.sendToPlayer(new S2C_MarketDataPacket(allListings, myListings,
                    MSPPointsSavedData.get(player.serverLevel()).getPoints(player.getUUID())), player);
        });
        ctx.get().setPacketHandled(true);
    }
}