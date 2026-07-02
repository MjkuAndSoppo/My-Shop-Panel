package com.example.myshoppanel.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 统一本地化工具。所有界面文字通过 Component.translatable 获取。
 */
public class ShopLang {

    public static MutableComponent t(String key, Object... args) {
        return Component.translatable("my_shop_panel." + key, args);
    }

    // ---- 常用按钮 ----
    public static MutableComponent btnBack() { return t("btn.back"); }
    public static MutableComponent btnConfirm() { return t("btn.confirm"); }
    public static MutableComponent btnCancel() { return t("btn.cancel"); }

    // ---- 常用标签 ----
    public static MutableComponent labelBalance() { return t("label.balance"); }
    public static MutableComponent labelPrice() { return t("label.price"); }
    public static MutableComponent labelQuantity() { return t("label.quantity"); }
}
