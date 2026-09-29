package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_OpenMenuPacket {

    public enum MenuType {
        MAIN_MENU,
        PLAYER_MARKET,
        ADMIN_SHOP,
        ADMIN_SHOP_EDIT
    }

    private final MenuType menuType;
    private final double balance;

    public S2C_OpenMenuPacket(MenuType menuType, double balance) {
        this.menuType = menuType;
        this.balance = balance;
    }

    public MenuType getMenuType() { return menuType; }
    public double getBalance() { return balance; }

    public static void encode(S2C_OpenMenuPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.menuType);
        buf.writeDouble(msg.balance);
    }

    public static S2C_OpenMenuPacket decode(FriendlyByteBuf buf) {
        return new S2C_OpenMenuPacket(buf.readEnum(MenuType.class), buf.readDouble());
    }

    public static void handle(S2C_OpenMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleOpenMenu(msg)));
        ctx.get().setPacketHandled(true);
    }
}
