package com.example.myshoppanel.shop;

import com.google.gson.annotations.Expose;

/**
 * 单条交易记录。
 */
public class TransactionRecord {

    public enum Type {
        BUY,    // 从市场买入
        SELL,   // 通过市场卖出
        LIST,   // 上架（付手续费）
        DELIST, // 下架（退回物品）
        SYSTEM_BUY,  // 被动态系统买入
        SYSTEM_SELL, // 从动态系统买入
        ADMIN_BUY,   // 从世界商店购买
        ADMIN_SELL,  // 向世界商店出售
        ADMIN_BUYBACK // 从世界商店买回
    }

    @Expose public Type type;
    @Expose public String itemName;
    @Expose public String itemId;
    @Expose public int quantity;
    @Expose public double totalPrice;
    @Expose public double unitPrice;
    @Expose public String counterparty;
    @Expose public long timestamp;

    public TransactionRecord() {}

    public TransactionRecord(Type type, String itemName, String itemId, int quantity,
                             double totalPrice, double unitPrice, String counterparty, long timestamp) {
        this.type = type;
        this.itemName = itemName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.unitPrice = unitPrice;
        this.counterparty = counterparty;
        this.timestamp = timestamp;
    }

    public String getTypeLabel() {
        return switch (type) {
            case BUY -> "买入";
            case SELL -> "卖出";
            case LIST -> "上架";
            case DELIST -> "下架";
            case SYSTEM_BUY -> "系统买入";
            case SYSTEM_SELL -> "系统卖出";
            case ADMIN_BUY -> "商店购买";
            case ADMIN_SELL -> "商店出售";
            case ADMIN_BUYBACK -> "商店买回";
        };
    }

    public String getTypeColor() {
        return switch (type) {
            case BUY, SELL, SYSTEM_BUY, SYSTEM_SELL -> "§6";
            case LIST -> "§a";
            case DELIST -> "§c";
            case ADMIN_BUY, ADMIN_SELL, ADMIN_BUYBACK -> "§b";
        };
    }
}