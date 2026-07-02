package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.EditModeData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_EditModePacket {
    private final boolean enabled;

    public S2C_EditModePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(S2C_EditModePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static S2C_EditModePacket decode(FriendlyByteBuf buf) {
        return new S2C_EditModePacket(buf.readBoolean());
    }

    public static void handle(S2C_EditModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            EditModeData.enabled = msg.enabled;
            if (Minecraft.getInstance().player != null) {
                String status = msg.enabled ? "§a已开启" : "§c已关闭";
                Minecraft.getInstance().player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§e[MyShopPanel] 编辑模式" + status),
                        false
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
