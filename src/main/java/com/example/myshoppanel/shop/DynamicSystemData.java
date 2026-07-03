package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 动态系统配置。
 * 配置文件：./config/my_shop_panel/dynamic_system.json
 *
 * 机器人 UUID 固定为 (0, 0)，MSPP 账户使用 MSPPointsSavedData，
 * 物品库存使用 RedundantWarehouseSavedData。
 */
public class DynamicSystemData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "dynamic_system.json";

    /** 机器人专用 UUID */
    public static final UUID BOT_UUID = new UUID(0, 0);
    public static final String BOT_NAME = "[动态系统]";

    // ===== 配置参数 =====
    private boolean enabled = true;
    private int tickInterval = 200;               // 巡检间隔（tick），默认10秒
    private double minRetainedFunds = 10000;       // 机器人最低留存资金，低于此不上架
    private double listingBatchSize = 5;           // 每次巡检最多上架批次数
    private int maxListings = 50;                  // 机器人挂单总数上限

    /** 机器人上架物品列表：注册名 → 标价（0=使用默认价格） */
    private Map<String, Double> listingItems = new LinkedHashMap<>();

    private transient Path configPath;

    // ===== 单例 =====
    private static DynamicSystemData instance;

    public static DynamicSystemData getInstance() { return instance; }

    public static void loadInstance(Path configDir) {
        instance = new DynamicSystemData();
        instance.configPath = configDir.resolve(FILE_NAME);
        if (Files.exists(instance.configPath)) {
            try (Reader reader = Files.newBufferedReader(instance.configPath)) {
                DynamicSystemData loaded = GSON.fromJson(reader, DynamicSystemData.class);
                if (loaded != null) {
                    instance.enabled = loaded.enabled;
                    instance.tickInterval = loaded.tickInterval;
                    instance.minRetainedFunds = loaded.minRetainedFunds;
                    instance.listingBatchSize = loaded.listingBatchSize;
                    instance.maxListings = loaded.maxListings;
                    instance.listingItems = loaded.listingItems != null ? loaded.listingItems : new LinkedHashMap<>();
                }
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 动态系统配置加载失败", e);
            }
        } else {
            instance.initDefaults();
            instance.save();
        }
        LOGGER.info("[MyShopPanel] 动态系统配置已加载: enabled={}, interval={}t, minFunds={}, items={}",
                instance.enabled, instance.tickInterval, instance.minRetainedFunds, instance.listingItems.size());
    }

    private void initDefaults() {
        listingItems.put("minecraft:iron_ingot", 0.0);
        listingItems.put("minecraft:gold_ingot", 0.0);
        listingItems.put("minecraft:diamond", 0.0);
        listingItems.put("minecraft:emerald", 0.0);
        listingItems.put("minecraft:redstone", 0.0);
        listingItems.put("minecraft:lapis_lazuli", 0.0);
        listingItems.put("minecraft:coal", 0.0);
        listingItems.put("minecraft:quartz", 0.0);
        listingItems.put("minecraft:cobblestone", 0.0);
        listingItems.put("minecraft:stone", 0.0);
        listingItems.put("minecraft:oak_log", 0.0);
        listingItems.put("minecraft:glass", 0.0);
        listingItems.put("minecraft:flint", 0.0);
        listingItems.put("minecraft:stick", 0.0);
    }

    public void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 动态系统配置保存失败", e);
        }
    }

    // ===== Getter =====
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; save(); }
    public int getTickInterval() { return tickInterval; }
    public void setTickInterval(int tickInterval) { this.tickInterval = Math.max(20, tickInterval); save(); }
    public double getMinRetainedFunds() { return minRetainedFunds; }
    public void setMinRetainedFunds(double minRetainedFunds) { this.minRetainedFunds = minRetainedFunds; save(); }
    public Map<String, Double> getListingItems() { return listingItems; }
    public int getMaxListings() { return maxListings; }
    public void setMaxListings(int maxListings) { this.maxListings = Math.max(1, maxListings); save(); }
    public double getListingBatchSize() { return listingBatchSize; }
    public void setListingBatchSize(double listingBatchSize) { this.listingBatchSize = Math.max(1, listingBatchSize); save(); }
}
