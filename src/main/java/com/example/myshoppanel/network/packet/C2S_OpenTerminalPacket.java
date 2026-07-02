package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：键盘快捷键请求打开终端主菜单。
 * 服务端查询余额后返回 S2C_OpenMenuPacket。
 */
public class C2S_OpenTerminalPacket {

    public C2S_OpenTerminalPacket() {}

    public static void encode(C2S_OpenTerminalPacket msg, FriendlyByteBuf buf) {}

    public static C2S_OpenTerminalPacket decode(FriendlyByteBuf buf) {
        return new C2S_OpenTerminalPacket();
    }

    public static void handle(C2S_OpenTerminalPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            double balance = MSPPointsSavedData.get(player.serverLevel())
                    .getPoints(player.getUUID());
            NetworkHandler.sendToPlayer(
                    new S2C_OpenMenuPacket(S2C_OpenMenuPacket.MenuType.MAIN_MENU, balance),
                    player);
        });
        ctx.get().setPacketHandled(true);
    }
}
