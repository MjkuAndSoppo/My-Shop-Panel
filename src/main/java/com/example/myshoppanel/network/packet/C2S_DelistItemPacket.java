package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.shop.PlayerMarketSavedData;
import com.example.myshoppanel.shop.TransactionService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：下架物品（撤回挂单）。
 */
public class C2S_DelistItemPacket {

    private final int displayId;

    public C2S_DelistItemPacket(int displayId) {
        this.displayId = displayId;
    }

    public static void encode(C2S_DelistItemPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.displayId);
    }

    public static C2S_DelistItemPacket decode(FriendlyByteBuf buf) {
        return new C2S_DelistItemPacket(buf.readInt());
    }

    public static void handle(C2S_DelistItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (msg.displayId < 100) {
                player.sendSystemMessage(Component.literal(
                        "§c[MyShopPanel] 无效的报价单ID: #" + msg.displayId));
                return;
            }

            PlayerMarketSavedData marketData = PlayerMarketSavedData.get(player.serverLevel());
            boolean ok = TransactionService.commitDelist(player, msg.displayId, marketData);

            if (ok) {
                player.sendSystemMessage(Component.literal(
                        "§a[MyShopPanel] 报价单 #" + msg.displayId + " 已下架，物品已退回。"));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§c[MyShopPanel] 下架失败：报价单 #" + msg.displayId + " 不存在。"));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
