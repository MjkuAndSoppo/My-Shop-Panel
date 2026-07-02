package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.AdminShopConfig;
import com.example.myshoppanel.shop.AdminShopEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求世界商店报价数据。
 */
public class C2S_RequestAdminShopDataPacket {

    public C2S_RequestAdminShopDataPacket() {}

    public static void encode(C2S_RequestAdminShopDataPacket msg, FriendlyByteBuf buf) {}

    public static C2S_RequestAdminShopDataPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestAdminShopDataPacket();
    }

    public static void handle(C2S_RequestAdminShopDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            AdminShopConfig config = AdminShopConfig.getInstance();
            if (config == null) return;

            List<AdminShopEntry> entries = config.getAllEntries();
            NetworkHandler.sendToPlayer(new S2C_AdminShopDataPacket(entries,
                    MSPPointsSavedData.get(player.serverLevel()).getPoints(player.getUUID())), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
