package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求冗余仓库数据。
 */
public class C2S_RequestWarehouseDataPacket {

    public C2S_RequestWarehouseDataPacket() {}

    public static void encode(C2S_RequestWarehouseDataPacket msg, FriendlyByteBuf buf) {}

    public static C2S_RequestWarehouseDataPacket decode(FriendlyByteBuf buf) {
        return new C2S_RequestWarehouseDataPacket();
    }

    public static void handle(C2S_RequestWarehouseDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(player.serverLevel());
            var pages = warehouse.getPlayerPages(player.getUUID());
            NetworkHandler.sendToPlayer(new S2C_WarehouseDataPacket(pages), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
