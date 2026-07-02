package com.example.myshoppanel.economy;

import com.example.myshoppanel.shop.ShopUtils;

/**
 * 客户端余额缓存。
 */
public class ClientBalanceData {
    public static double balance = 0;

    public static String format() {
        return ShopUtils.fmt(balance);
    }

    public static String format(double v) {
        return ShopUtils.fmt(v);
    }
}
