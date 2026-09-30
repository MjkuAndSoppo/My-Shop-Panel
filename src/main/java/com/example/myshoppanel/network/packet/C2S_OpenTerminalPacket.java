package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.item.QuotationTerminalItem;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.ServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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

            // 服务端配置决定 I 键打开是否需要拥有报价终端
            if (ServerConfig.isRequireTerminalForHotkey() && !hasTerminal(player)) {
                player.sendSystemMessage(Component.translatable("my_shop_panel.hint.need_terminal"));
                return;
            }

            double balance = MSPPointsSavedData.get(player.serverLevel())
                    .getPoints(player.getUUID());
            NetworkHandler.sendToPlayer(
                    new S2C_OpenMenuPacket(S2C_OpenMenuPacket.MenuType.MAIN_MENU, balance),
                    player);
        });
        ctx.get().setPacketHandled(true);
    }

    /** 玩家身上（背包 / 主手 / 副手）是否持有报价终端 */
    private static boolean hasTerminal(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof QuotationTerminalItem) return true;
        if (player.getOffhandItem().getItem() instanceof QuotationTerminalItem) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof QuotationTerminalItem) return true;
        }
        return false;
    }
}
