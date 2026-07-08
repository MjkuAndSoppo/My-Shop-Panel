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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 报价组 — 永久化记录行情价格。
 *
 * 每当玩家在玩家市场上架物品时，记录/更新该物品的价格（折中平均）。
 * 文件：./config/my_shop_panel/quote_group.json
 *
 * 数据结构：{ "entries": { "minecraft:stone": { "price": 5.0, "count": 2 } } }
 */
public class QuoteGroupData {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "quote_group.json";

    // ---- 内部条目 ----
    public static class QuoteEntry {
        public double price;
        public long updateCount; // 更新次数（每次上架 +1）

        public QuoteEntry(double price, long count) {
            this.price = price;
            this.updateCount = count;
        }

        public QuoteEntry() {
            this.price = 0;
            this.updateCount = 0;
        }
    }

    // ---- 存储结构 ----
    private Map<String, QuoteEntry> entries = new LinkedHashMap<>();

    private transient Path configPath;

    // ---- 单例 ----
    private static QuoteGroupData instance;

    public static QuoteGroupData getInstance() { return instance; }

    public static void loadInstance(Path configDir) {
        instance = new QuoteGroupData();
        instance.configPath = configDir.resolve(FILE_NAME);
        if (Files.exists(instance.configPath)) {
            try (Reader reader = Files.newBufferedReader(instance.configPath)) {
                var wrapper = GSON.fromJson(reader,
                        new TypeToken<Map<String, Map<String, QuoteEntry>>>() {}.getType());
                if (wrapper != null) {
                    var loaded = (Map<String, Map<String, QuoteEntry>>) wrapper;
                    var raw = loaded.get("entries");
                    if (raw != null) {
                        instance.entries = new LinkedHashMap<>(raw);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 报价组加载失败", e);
            }
        } else {
            instance.save();
        }
        LOGGER.info("[MyShopPanel] 报价组已加载: {} 条记录", instance.entries.size());
    }

    public void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("entries", entries);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(wrapper, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 报价组保存失败", e);
        }
    }

    // ---- API ----

    /**
     * 记录一次玩家上架，更新价格（折中平均）。
     * @param registryName 物品注册名
     * @param price 本次标价
     * @param quantity 上架数量（用于加权平均，越大影响越大）
     */
    public static void recordListing(String registryName, double price, int quantity) {
        if (instance == null || registryName == null || price <= 0) return;
        QuoteEntry entry = instance.entries.get(registryName);
        if (entry == null) {
            entry = new QuoteEntry(price, 1);
            instance.entries.put(registryName, entry);
        } else {
            // 加权折中平均
            long oldCount = entry.updateCount;
            // 上架数量权重（每次上架权重 = quantity，避免大批量上架过度影响）
            entry.price = (entry.price * oldCount + price * quantity) / (oldCount + quantity);
            entry.updateCount = oldCount + 1; // 上架次数 +1（不计数量）
            entry.price = Math.round(entry.price * 100.0) / 100.0; // 保留两位小数
        }
        instance.save();
        LOGGER.debug("[MyShopPanel] 报价组更新: {} → {} MSPP (第{}次)", registryName, entry.price, entry.updateCount);
    }

    /**
     * 获取某个物品的行情价格。不存在返回 -1。
     */
    public static double getPrice(String registryName) {
        if (instance == null || registryName == null) return -1;
        QuoteEntry entry = instance.entries.get(registryName);
        return entry != null ? entry.price : -1;
    }

    /** 报价组条目数 */
    public static int size() {
        return instance != null ? instance.entries.size() : 0;
    }

    /** 遍历全部条目 */
    public static Set<Map.Entry<String, QuoteEntry>> allEntries() {
        return instance != null ? instance.entries.entrySet() : Set.of();
    }

    /** 清空所有报价组条目 */
    public static void clear() {
        if (instance == null) return;
        instance.entries.clear();
        instance.save();
        LOGGER.info("[MyShopPanel] 报价组已清空");
    }
}
