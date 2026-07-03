package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 手续费计算引擎。
 * 公式：最终手续费 = 基础手续费 × 涨价惩罚系数 × 批量折扣系数
 *
 * 基础手续费 = 标价 × 数量 × feeRate
 * 涨价惩罚系数 = min(标价 / 默认价格, maxMarkupPenalty)，标价≤默认价格则为 1.0
 * 批量折扣系数 = max(minBulkDiscount, 1.0 - (数量-1) × bulkDiscountPerItem)
 *
 * 配置文件：./config/my_shop_panel/listing_fee.json
 */
public class ListingFeeCalculator {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "listing_fee.json";

    // ===== 可配置参数 =====
    private static double feeRate = 0.1;              // 基础手续费率
    private static double maxMarkupPenalty = 2.0;     // 涨价惩罚上限倍数
    private static double minBulkDiscount = 0.5;       // 批量折扣下限倍数
    private static double bulkDiscountPerItem = 0.02;  // 每多一个数量减少的折扣率

    private static Path configPath = null;
    private static boolean loaded = false;

    public static void load(Path configDir) {
        configPath = configDir.resolve(FILE_NAME);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                Map<String, Double> cfg = GSON.fromJson(reader, new TypeToken<Map<String, Double>>() {}.getType());
                if (cfg != null) {
                    if (cfg.containsKey("feeRate")) feeRate = cfg.get("feeRate");
                    if (cfg.containsKey("maxMarkupPenalty")) maxMarkupPenalty = cfg.get("maxMarkupPenalty");
                    if (cfg.containsKey("minBulkDiscount")) minBulkDiscount = cfg.get("minBulkDiscount");
                    if (cfg.containsKey("bulkDiscountPerItem")) bulkDiscountPerItem = cfg.get("bulkDiscountPerItem");
                }
                LOGGER.info("[MyShopPanel] 手续费配置已加载: rate={}, markupPenalty={}, bulkDiscount={}, perItem={}",
                        feeRate, maxMarkupPenalty, minBulkDiscount, bulkDiscountPerItem);
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 手续费配置加载失败，使用默认值", e);
            }
        } else {
            save();
        }
        loaded = true;
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Map<String, Double> cfg = new LinkedHashMap<>();
            cfg.put("feeRate", feeRate);
            cfg.put("maxMarkupPenalty", maxMarkupPenalty);
            cfg.put("minBulkDiscount", minBulkDiscount);
            cfg.put("bulkDiscountPerItem", bulkDiscountPerItem);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(cfg, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 手续费配置保存失败", e);
        }
    }

    /** 确保已加载（客户端侧延迟加载） */
    private static void ensureLoaded() {
        if (!loaded) {
            load(Paths.get("config", "my_shop_panel"));
        }
    }

    // ===== 计算 =====

    /**
     * 计算最终手续费。
     * @param price 玩家标价
     * @param quantity 上架数量
     * @param item 上架物品（用于查询默认价格）
     * @return 最终手续费（四舍五入到2位小数）
     */
    public static double calculateFee(double price, int quantity, ItemStack item) {
        ensureLoaded();
        if (quantity <= 0) return 0;

        double baseFee = price * quantity * feeRate;

        // 涨价惩罚系数
        double defaultPrice = ShopUtils.getEMCDefaultPrice(item);
        double markupRatio = 1.0;
        if (price > defaultPrice && defaultPrice > 0) {
            markupRatio = Math.min(price / defaultPrice, maxMarkupPenalty);
        }

        // 批量折扣系数
        double bulkRatio = Math.max(minBulkDiscount, 1.0 - (quantity - 1) * bulkDiscountPerItem);

        return ShopUtils.roundAmount(baseFee * markupRatio * bulkRatio);
    }

    /**
     * 计算手续费占标价的百分比（用于UI显示）。
     */
    public static double getFeePercentage(double price, int quantity, ItemStack item) {
        double fee = calculateFee(price, quantity, item);
        double totalPrice = price * quantity;
        if (totalPrice <= 0) return 0;
        return Math.min(100.0, ShopUtils.roundAmount(fee / totalPrice * 100));
    }

    // ===== Getter（供指令查询） =====

    public static double getFeeRate() { ensureLoaded(); return feeRate; }
    public static double getMaxMarkupPenalty() { ensureLoaded(); return maxMarkupPenalty; }
    public static double getMinBulkDiscount() { ensureLoaded(); return minBulkDiscount; }
    public static double getBulkDiscountPerItem() { ensureLoaded(); return bulkDiscountPerItem; }

    public static void setFeeRate(double v) { feeRate = v; }
    public static void setMaxMarkupPenalty(double v) { maxMarkupPenalty = v; }
    public static void setMinBulkDiscount(double v) { minBulkDiscount = v; }
    public static void setBulkDiscountPerItem(double v) { bulkDiscountPerItem = v; }
}
