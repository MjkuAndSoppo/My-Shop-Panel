package com.example.myshoppanel.network;

import com.example.myshoppanel.MyShopPanel;
import com.example.myshoppanel.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通信层。
 * 使用 Forge SimpleChannel，协议版本 1.0。
 * 服务端全权计算，客户端仅负责 UI 渲染与发包。
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1.1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MyShopPanel.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.registerMessage(packetId++, S2C_OpenMenuPacket.class,
                S2C_OpenMenuPacket::encode, S2C_OpenMenuPacket::decode, S2C_OpenMenuPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_RequestMarketDataPacket.class,
                C2S_RequestMarketDataPacket::encode, C2S_RequestMarketDataPacket::decode, C2S_RequestMarketDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_MarketDataPacket.class,
                S2C_MarketDataPacket::encode, S2C_MarketDataPacket::decode, S2C_MarketDataPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_ConfirmTransactionPacket.class,
                C2S_ConfirmTransactionPacket::encode, C2S_ConfirmTransactionPacket::decode, C2S_ConfirmTransactionPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_ListMarketItemPacket.class,
                C2S_ListMarketItemPacket::encode, C2S_ListMarketItemPacket::decode, C2S_ListMarketItemPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_RequestAdminShopDataPacket.class,
                C2S_RequestAdminShopDataPacket::encode, C2S_RequestAdminShopDataPacket::decode, C2S_RequestAdminShopDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_AdminShopDataPacket.class,
                S2C_AdminShopDataPacket::encode, S2C_AdminShopDataPacket::decode, S2C_AdminShopDataPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_DelistItemPacket.class,
                C2S_DelistItemPacket::encode, C2S_DelistItemPacket::decode, C2S_DelistItemPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_EditModePacket.class,
                S2C_EditModePacket::encode, S2C_EditModePacket::decode, S2C_EditModePacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_AdminShopEditPacket.class,
                C2S_AdminShopEditPacket::encode, C2S_AdminShopEditPacket::decode, C2S_AdminShopEditPacket::handle);
        // 冗余仓库包
        CHANNEL.registerMessage(packetId++, C2S_RequestWarehouseDataPacket.class,
                C2S_RequestWarehouseDataPacket::encode, C2S_RequestWarehouseDataPacket::decode, C2S_RequestWarehouseDataPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_WarehouseDataPacket.class,
                S2C_WarehouseDataPacket::encode, S2C_WarehouseDataPacket::decode, S2C_WarehouseDataPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_RetrieveWarehouseItemsPacket.class,
                C2S_RetrieveWarehouseItemsPacket::encode, C2S_RetrieveWarehouseItemsPacket::decode, C2S_RetrieveWarehouseItemsPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_OpenTerminalPacket.class,
                C2S_OpenTerminalPacket::encode, C2S_OpenTerminalPacket::decode, C2S_OpenTerminalPacket::handle);
        // 交易记录
        CHANNEL.registerMessage(packetId++, C2S_RequestTransactionHistoryPacket.class,
                C2S_RequestTransactionHistoryPacket::encode, C2S_RequestTransactionHistoryPacket::decode, C2S_RequestTransactionHistoryPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_TransactionHistoryPacket.class,
                S2C_TransactionHistoryPacket::encode, S2C_TransactionHistoryPacket::decode, S2C_TransactionHistoryPacket::handle);
        // 价格走势
        CHANNEL.registerMessage(packetId++, C2S_RequestPriceItemListPacket.class,
                C2S_RequestPriceItemListPacket::encode, C2S_RequestPriceItemListPacket::decode, C2S_RequestPriceItemListPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_PriceItemListPacket.class,
                S2C_PriceItemListPacket::encode, S2C_PriceItemListPacket::decode, S2C_PriceItemListPacket::handle);
        CHANNEL.registerMessage(packetId++, C2S_RequestPriceHistoryPacket.class,
                C2S_RequestPriceHistoryPacket::encode, C2S_RequestPriceHistoryPacket::decode, C2S_RequestPriceHistoryPacket::handle);
        CHANNEL.registerMessage(packetId++, S2C_PriceHistoryPacket.class,
                S2C_PriceHistoryPacket::encode, S2C_PriceHistoryPacket::decode, S2C_PriceHistoryPacket::handle);
    }

    /** 向指定玩家发送数据包 */
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** 向服务端发送数据包 */
    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
