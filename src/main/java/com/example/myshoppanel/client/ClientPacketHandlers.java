package com.example.myshoppanel.client;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.economy.EditModeData;
import com.example.myshoppanel.network.packet.S2C_AdminShopDataPacket;
import com.example.myshoppanel.network.packet.S2C_EditModePacket;
import com.example.myshoppanel.network.packet.S2C_MarketDataPacket;
import com.example.myshoppanel.network.packet.S2C_OpenMenuPacket;
import com.example.myshoppanel.network.packet.S2C_WarehouseDataPacket;
import com.example.myshoppanel.screen.AdminShopEditScreen;
import com.example.myshoppanel.screen.AdminShopScreen;
import com.example.myshoppanel.screen.MainMenuScreen;
import com.example.myshoppanel.screen.PlayerMarketScreen;
import com.example.myshoppanel.screen.RedundantWarehouseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 纯客户端的数据包处理逻辑。
 * 该类只在客户端被加载，服务端通过 DistExecutor 隔离，绝不触碰 net.minecraft.client.*。
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    public static void handleOpenMenu(S2C_OpenMenuPacket msg) {
        ClientBalanceData.balance = msg.getBalance();
        Minecraft mc = Minecraft.getInstance();
        switch (msg.getMenuType()) {
            case MAIN_MENU -> mc.setScreen(new MainMenuScreen());
            case PLAYER_MARKET -> mc.setScreen(new PlayerMarketScreen());
            case ADMIN_SHOP -> mc.setScreen(new AdminShopScreen());
            case ADMIN_SHOP_EDIT -> mc.setScreen(new AdminShopEditScreen());
        }
    }

    public static void handleMarketData(S2C_MarketDataPacket msg) {
        ClientBalanceData.balance = msg.getBalance();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PlayerMarketScreen screen) {
            screen.updateListings(msg.getAllListings(), msg.getMyListings());
        }
    }

    public static void handleAdminShopData(S2C_AdminShopDataPacket msg) {
        ClientBalanceData.balance = msg.getBalance();
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AdminShopScreen screen) {
            screen.updateEntries(msg.getEntries());
        } else if (mc.screen instanceof AdminShopEditScreen editScreen) {
            editScreen.updateEntries(msg.getEntries());
        }
    }

    public static void handleWarehouseData(S2C_WarehouseDataPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RedundantWarehouseScreen screen) {
            screen.updateData(msg.getPages(), msg.getPageTimers());
        }
    }

    public static void handleEditMode(S2C_EditModePacket msg) {
        EditModeData.enabled = msg.isEnabled();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Component status = Component.translatable(msg.isEnabled()
                    ? "my_shop_panel.cmd.mspedit.on"
                    : "my_shop_panel.cmd.mspedit.off");
            mc.player.displayClientMessage(
                    Component.translatable("my_shop_panel.tx.msg.edit_mode", status),
                    false
            );
        }
    }
}