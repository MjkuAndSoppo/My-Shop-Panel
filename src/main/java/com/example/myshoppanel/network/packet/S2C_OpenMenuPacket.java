package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.screen.AdminShopEditScreen;
import com.example.myshoppanel.screen.AdminShopScreen;
import com.example.myshoppanel.screen.MainMenuScreen;
import com.example.myshoppanel.screen.PlayerMarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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

    public static void encode(S2C_OpenMenuPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.menuType);
        buf.writeDouble(msg.balance);
    }

    public static S2C_OpenMenuPacket decode(FriendlyByteBuf buf) {
        return new S2C_OpenMenuPacket(buf.readEnum(MenuType.class), buf.readDouble());
    }

    public static void handle(S2C_OpenMenuPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBalanceData.balance = msg.balance;
            Minecraft mc = Minecraft.getInstance();
            switch (msg.menuType) {
                case MAIN_MENU -> mc.setScreen(new MainMenuScreen());
                case PLAYER_MARKET -> mc.setScreen(new PlayerMarketScreen());
                case ADMIN_SHOP -> mc.setScreen(new AdminShopScreen());
                case ADMIN_SHOP_EDIT -> mc.setScreen(new AdminShopEditScreen());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
