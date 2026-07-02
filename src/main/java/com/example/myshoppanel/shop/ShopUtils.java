package com.example.myshoppanel.shop;

import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 商店系统通用工具方法。
 */
public class ShopUtils {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Boolean projectELoaded = null;

    /** 将double格式化为MSPP显示字符串，整数不显示小数位 */
    public static String fmt(double v) {
        if (Double.isNaN(v)) return "0 MSPP";
        if (Double.isInfinite(v)) return v > 0 ? "∞ MSPP" : "-∞ MSPP";
        // 使用round避免浮点判断误差
        long rounded = Math.round(v);
        if (Math.abs(v - rounded) < 1e-9) {
            return String.format("%,d MSPP", rounded);
        }
        return String.format("%,.2f MSPP", v);
    }

    /** 将金额四舍五入到合理精度，避免浮点误差累积 */
    public static double roundAmount(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ==================== ProjectE 联动 ====================

    /** 检查 ProjectE 是否已加载（缓存结果） */
    public static boolean isProjectELoaded() {
        if (projectELoaded == null) {
            try {
                Class.forName("moze_intel.projecte.api.ProjectEAPI");
                projectELoaded = true;
                LOGGER.info("[MyShopPanel] ProjectE 已检测到，将使用 EMC 作为默认定价。");
            } catch (ClassNotFoundException e) {
                projectELoaded = false;
                LOGGER.info("[MyShopPanel] ProjectE 未安装，使用默认价格 1。");
            }
        }
        return projectELoaded;
    }

    /**
     * 获取物品的 ProjectE EMC 值作为默认标价。
     * 未安装 ProjectE 或物品无 EMC 时返回 1.0。
     */
    public static double getEMCDefaultPrice(ItemStack stack) {
        if (!isProjectELoaded() || stack.isEmpty()) return 1.0;
        try {
            Class<?> api = Class.forName("moze_intel.projecte.api.ProjectEAPI");
            Object proxy = api.getMethod("getEMCProxy").invoke(null);
            long emc = (long) proxy.getClass()
                    .getMethod("getValue", ItemStack.class)
                    .invoke(proxy, stack);
            if (emc > 0) {
                return (double) emc;
            }
        } catch (Exception e) {
            LOGGER.debug("[MyShopPanel] EMC 查询失败: {}", e.toString());
        }
        return 1.0;
    }
}
