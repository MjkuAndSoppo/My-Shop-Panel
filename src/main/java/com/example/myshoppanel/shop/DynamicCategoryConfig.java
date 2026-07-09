package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 动态系统分类配置 — 按创造标签栏/物品类型配置不同的上架概率权重。
 * 配置文件：./config/my_shop_panel/msp_dynamic_categories.json
 */
public class DynamicCategoryConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "msp_dynamic_categories.json";

    public static class CategoryConfig {
        @Expose public String creativeTabId;
        @Expose public double weight = 1.0;
        @Expose public int restockCost = 500;
        @Expose public int listingCost = 10000;
        @Expose public boolean enabled = true;

        public CategoryConfig() {}

        public CategoryConfig(String creativeTabId, double weight, int restockCost, int listingCost, boolean enabled) {
            this.creativeTabId = creativeTabId;
            this.weight = weight;
            this.restockCost = restockCost;
            this.listingCost = listingCost;
            this.enabled = enabled;
        }
    }

    @Expose private List<CategoryConfig> categories = new ArrayList<>();
    private transient Path configPath;

    // ---- 单例 ----
    private static DynamicCategoryConfig instance;

    public static DynamicCategoryConfig getInstance() { return instance; }

    public static void loadInstance(Path configDir) {
        instance = new DynamicCategoryConfig();
        instance.configPath = configDir.resolve(FILE_NAME);
        if (Files.exists(instance.configPath)) {
            try (Reader reader = Files.newBufferedReader(instance.configPath)) {
                DynamicCategoryConfig loaded = GSON.fromJson(reader, DynamicCategoryConfig.class);
                if (loaded != null && loaded.categories != null) {
                    instance.categories = loaded.categories;
                }
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 动态分类配置加载失败", e);
            }
        } else {
            instance.save();
        }
        LOGGER.info("[MyShopPanel] 动态分类配置已加载: {} 个分类", instance.categories.size());
    }

    public void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 动态分类配置保存失败", e);
        }
    }

    /**
     * 获取指定标签栏的配置，不存在则返回默认配置。
     */
    public CategoryConfig getOrCreate(String tabId) {
        for (CategoryConfig c : categories) {
            if (c.creativeTabId.equals(tabId)) return c;
        }
        CategoryConfig cfg = new CategoryConfig(tabId, 1.0, 500, 10000, true);
        categories.add(cfg);
        save();
        return cfg;
    }

    /**
     * 获取指定标签栏的配置（不创建）。
     */
    public CategoryConfig get(String tabId) {
        for (CategoryConfig c : categories) {
            if (c.creativeTabId.equals(tabId)) return c;
        }
        return null;
    }

    public List<CategoryConfig> getAll() {
        return Collections.unmodifiableList(categories);
    }

    public void setWeight(String tabId, double weight) {
        getOrCreate(tabId).weight = weight;
        save();
    }

    public void setCost(String tabId, int restockCost, int listingCost) {
        CategoryConfig c = getOrCreate(tabId);
        c.restockCost = restockCost;
        c.listingCost = listingCost;
        save();
    }

    public void setEnabled(String tabId, boolean enabled) {
        getOrCreate(tabId).enabled = enabled;
        save();
    }
}