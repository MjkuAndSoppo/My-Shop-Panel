package com.example.myshoppanel.shop;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

/**
 * 动态系统核心服务。
 * 负责定时巡检：检查机器人资金并自动在玩家市场上架物品。
 *
 * 资金来源（由交易包注入）：
 * - 玩家购买世界商店物品 → 100% 盈利注入
 * - 玩家买回世界商店收购物品 → 25% 差价注入
 * - 玩家上架手续费 → 100% 注入
 *
 * 上架流程（每轮巡检）：
 * 1. 余额 > minRetainedFunds 则触发
 * 2. 随机 0~maxListingItemCount 决定本轮尝试次数
 * 3. 逐个掷骰子(1~10)：
 *    - >5 → 补货：随机选已有机器人挂单，加 random(1,restockMaxCount) 个，余额-500
 *    - <5 → 新上架：从报价组加权随机选物品（可重复），重复物品叠加到已有报价，余额-minRetainedFunds
 *    - ==5 → 结束本轮，余额-100
 */
public class DynamicSystemService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RNG = new Random();

    // 成本常量（默认值，实际从 DynamicCategoryConfig 读取）
    private static final double DEFAULT_COST_RESTOCK = 500;
    private static final double DEFAULT_COST_LISTING = 10000;
    private static final double COST_END_ROUND = 100;

    // 缓存：物品注册名 → 创造标签栏ID
    private static final Map<String, String> itemTabCache = new HashMap<>();

    /**
     * 主巡检 tick。由 MyShopPanel.onServerTick 调用。
     */
    public static void onServerTick(ServerLevel overworld, int tickCount) {
        DynamicSystemData cfg = DynamicSystemData.getInstance();
        if (cfg == null || !cfg.isEnabled()) return;
        if (tickCount % cfg.getTickInterval() != 0) return;

        MSPPointsSavedData points = MSPPointsSavedData.get(overworld);
        PlayerMarketSavedData market = PlayerMarketSavedData.get(overworld);

        double botBalance = points.getPoints(DynamicSystemData.BOT_UUID);
        if (botBalance < cfg.getMinRetainedFunds()) return;

        // 检查报价组是否有数据
        if (QuoteGroupData.size() == 0) return;

        // 确保分类配置已加载
        DynamicCategoryConfig catCfg = DynamicCategoryConfig.getInstance();

        // --- 步骤1：随机 0 ~ maxListingItemCount ---
        int maxItems = cfg.getMaxListingItemCount();
        int totalToProcess = RNG.nextInt(maxItems + 1); // 0 ~ maxListingItemCount

        int newListed = 0;
        int restocked = 0;
        int ends = 0;

        for (int i = 0; i < totalToProcess; i++) {
            botBalance = points.getPoints(DynamicSystemData.BOT_UUID);

            // 挂单上限保护
            long currentListings = market.getAllListings().stream()
                    .filter(l -> l.getSellerUUID().equals(DynamicSystemData.BOT_UUID))
                    .count();
            if (currentListings >= cfg.getMaxListings()) break;

            // 检查钱包余额是否足够任何操作
            double minRetained = cfg.getMinRetainedFunds();
            if (botBalance < minRetained + DEFAULT_COST_RESTOCK
                    && botBalance < minRetained + DEFAULT_COST_LISTING) break;

            // --- 步骤2：掷骰子 ---
            int dice = RNG.nextInt(10) + 1; // 1~10

            if (dice == 5) {
                // 结束本轮
                deductBotFunds(overworld, COST_END_ROUND);
                ends++;
                break;
            } else if (dice > 5) {
                // 补货
                double restockCost = DEFAULT_COST_RESTOCK;
                if (botBalance >= cfg.getMinRetainedFunds() + restockCost) {
                    if (restockExisting(botBalance, cfg, market, points, overworld, catCfg)) {
                        restocked++;
                    }
                }
            } else {
                // 新上架
                double listingCost = DEFAULT_COST_LISTING;
                if (botBalance >= minRetained + listingCost) {
                    if (listNewItem(botBalance, cfg, market, points, overworld, catCfg)) {
                        newListed++;
                    }
                }
            }
        }

        if (newListed > 0 || restocked > 0 || ends > 0) {
            botBalance = points.getPoints(DynamicSystemData.BOT_UUID);
            LOGGER.info("[MyShopPanel] 动态系统本轮: 新上架 {} 个, 补货 {} 个, 结束 {} 次 (余额: {} MSPP)",
                    newListed, restocked, ends, ShopUtils.fmt(botBalance));
        }
    }

    /**
     * 补货：随机找一个机器人现有挂单，随机增加数量（上限可配置），扣除对应分类的 restockCost。
     */
    private static boolean restockExisting(double botBalance, DynamicSystemData cfg,
                                           PlayerMarketSavedData market, MSPPointsSavedData points,
                                           ServerLevel level, DynamicCategoryConfig catCfg) {
        List<PlayerMarketListing> botListings = market.getAllListings().stream()
                .filter(l -> l.getSellerUUID().equals(DynamicSystemData.BOT_UUID))
                .collect(java.util.stream.Collectors.toList());

        if (botListings.isEmpty()) return false;

        PlayerMarketListing target = botListings.get(RNG.nextInt(botListings.size()));
        int oldCount = target.getItem().getCount();
        int maxStack = target.getItem().getMaxStackSize();
        int addAmount = RNG.nextInt(cfg.getRestockMaxCount()) + 1;
        int newCount = Math.min(oldCount + addAmount, maxStack * 16);

        if (newCount <= oldCount) return false;

        double price = ShopUtils.roundAmount(target.getPrice() * ((double) newCount / oldCount));

        // 使用分类配置的 restockCost
        double restockCost = getCategoryRestockCost(target.getItem(), catCfg);
        if (botBalance < cfg.getMinRetainedFunds() + restockCost) return false;

        // 扣除成本
        deductBotFunds(level, restockCost);

        market.removeListing(target.getListingId());
        ItemStack newStack = target.getItem().copy();
        newStack.setCount(newCount);
        int displayId = market.nextDisplayId();
        PlayerMarketListing listing = new PlayerMarketListing(
                UUID.randomUUID(), DynamicSystemData.BOT_UUID, DynamicSystemData.BOT_NAME,
                newStack, price, System.currentTimeMillis(), displayId);
        market.addListing(listing);
        return true;
    }

    /**
     * 新上架：从报价组加权随机选物品（可重复），价值越低概率越高。
     * 同时考虑分类权重。若已有同物品的机器人挂单，则叠加到已有报价上。
     * 成本 = 对应分类的 listingCost。
     */
    private static boolean listNewItem(double botBalance, DynamicSystemData cfg,
                                       PlayerMarketSavedData market, MSPPointsSavedData points,
                                       ServerLevel level, DynamicCategoryConfig catCfg) {
        var allEntries = QuoteGroupData.allEntries();
        if (allEntries.isEmpty()) return false;

        double minRetained = cfg.getMinRetainedFunds();

        // 构建加权列表（考虑分类权重）
        List<String> items = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;

        for (var entry : allEntries) {
            String id = entry.getKey();
            double price = entry.getValue().price;
            if (price <= 0) price = 1.0;
            double w = 1.0 / Math.sqrt(Math.max(price, 1));
            // 乘以分类权重
            double categoryWeight = getCategoryWeight(id, catCfg);
            w *= categoryWeight;
            if (w <= 0) continue; // 跳过禁用的分类
            items.add(id);
            weights.add(w);
            totalWeight += w;
        }

        if (totalWeight <= 0 || items.isEmpty()) return false;

        // 加权随机选择
        double roll = RNG.nextDouble() * totalWeight;
        double cumulative = 0;
        String chosen = items.get(0);
        for (int i = 0; i < items.size(); i++) {
            cumulative += weights.get(i);
            if (roll <= cumulative) {
                chosen = items.get(i);
                break;
            }
        }

        ItemStack stack = createItemStack(chosen);
        // 禁止上架空气
        if (stack.isEmpty()) return false;

        // 禁止上架黑名单物品
        if (MarketBlacklist.isBlacklisted(stack)) {
            LOGGER.info("[MyShopPanel] 动态系统跳过黑名单物品: {}", chosen);
            return false;
        }

        // 使用分类配置的 listingCost
        double listingCost = getCategoryListingCost(stack, catCfg);

        // 检查余额
        if (botBalance < minRetained + listingCost) return false;

        // 使用报价组中的行情价格
        double quotePrice = QuoteGroupData.getPrice(chosen);
        if (quotePrice <= 0) quotePrice = ShopUtils.getEMCDefaultPrice(stack);
        if (quotePrice <= 0) quotePrice = 1.0;

        int stackCount = Math.min(stack.getMaxStackSize(), 64);
        stack.setCount(stackCount);

        // 检查是否已有同物品的机器人挂单，若有则叠加
        List<PlayerMarketListing> botListings = market.getAllListings().stream()
                .filter(l -> l.getSellerUUID().equals(DynamicSystemData.BOT_UUID))
                .collect(java.util.stream.Collectors.toList());
        for (PlayerMarketListing existing : botListings) {
            if (ItemStack.isSameItemSameTags(existing.getItem(), stack)) {
                int oldCount = existing.getItem().getCount();
                int maxStack = existing.getItem().getMaxStackSize();
                int newCount = Math.min(oldCount + stackCount, maxStack * 16);
                if (newCount <= oldCount) return false;

                // 扣除成本
                deductBotFunds(level, listingCost);

                double newPrice = ShopUtils.roundAmount(quotePrice * ((double) newCount / stackCount));
                market.removeListing(existing.getListingId());
                ItemStack merged = existing.getItem().copy();
                merged.setCount(newCount);
                PlayerMarketListing mergedListing = new PlayerMarketListing(
                        UUID.randomUUID(), DynamicSystemData.BOT_UUID, DynamicSystemData.BOT_NAME,
                        merged, newPrice, System.currentTimeMillis(), market.nextDisplayId());
                market.addListing(mergedListing);
                return true;
            }
        }

        // 无重复，正常新上架
        deductBotFunds(level, listingCost);

        int displayId = market.nextDisplayId();
        PlayerMarketListing listing = new PlayerMarketListing(
                UUID.randomUUID(), DynamicSystemData.BOT_UUID, DynamicSystemData.BOT_NAME,
                stack, quotePrice, System.currentTimeMillis(), displayId);
        market.addListing(listing);
        return true;
    }

    // ===== 资金操作 =====

    /**
     * 注入机器人资金。
     */
    public static void injectBotFunds(ServerLevel level, double amount, String reason) {
        if (amount <= 0) return;
        MSPPointsSavedData points = MSPPointsSavedData.get(level);
        double newBalance = points.addPoints(DynamicSystemData.BOT_UUID, ShopUtils.roundAmount(amount));
        LOGGER.info("[MyShopPanel] 机器人资金注入 {} MSPP (原因: {})，当前余额: {} MSPP",
                ShopUtils.fmt(amount), reason, ShopUtils.fmt(newBalance));
    }

    /**
     * 扣除机器人资金（上架成本等）。
     */
    public static boolean deductBotFunds(ServerLevel level, double amount) {
        if (amount <= 0) return true;
        MSPPointsSavedData points = MSPPointsSavedData.get(level);
        double bal = points.getPoints(DynamicSystemData.BOT_UUID);
        if (bal < amount) return false;
        points.cutPoints(DynamicSystemData.BOT_UUID, ShopUtils.roundAmount(amount));
        return true;
    }

    // ===== 分类配置辅助方法 =====

    /**
     * 获取物品所属的创造标签栏ID（带缓存）。
     */
    private static String getTabForItem(ItemStack stack) {
        String regName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        if (itemTabCache.containsKey(regName)) {
            return itemTabCache.get(regName);
        }
        try {
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab == null) continue;
                if (tab.contains(stack)) {
                    String tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
                    itemTabCache.put(regName, tabId);
                    return tabId;
                }
            }
        } catch (Exception ignored) {}
        itemTabCache.put(regName, "unknown");
        return "unknown";
    }

    private static double getCategoryWeight(String itemRegName, DynamicCategoryConfig catCfg) {
        ItemStack stack = createItemStack(itemRegName);
        if (stack.isEmpty()) return 1.0;
        return getCategoryWeight(stack, catCfg);
    }

    private static double getCategoryWeight(ItemStack stack, DynamicCategoryConfig catCfg) {
        if (catCfg == null) return 1.0;
        String tabId = getTabForItem(stack);
        DynamicCategoryConfig.CategoryConfig cc = catCfg.get(tabId);
        if (cc == null) return 1.0;
        if (!cc.enabled) return 0.0;
        return cc.weight;
    }

    private static double getCategoryListingCost(ItemStack stack, DynamicCategoryConfig catCfg) {
        if (catCfg == null) return DEFAULT_COST_LISTING;
        String tabId = getTabForItem(stack);
        DynamicCategoryConfig.CategoryConfig cc = catCfg.get(tabId);
        return cc != null ? cc.listingCost : DEFAULT_COST_LISTING;
    }

    private static double getCategoryRestockCost(ItemStack stack, DynamicCategoryConfig catCfg) {
        if (catCfg == null) return DEFAULT_COST_RESTOCK;
        String tabId = getTabForItem(stack);
        DynamicCategoryConfig.CategoryConfig cc = catCfg.get(tabId);
        return cc != null ? cc.restockCost : DEFAULT_COST_RESTOCK;
    }

    // ===== 工具方法 =====

    private static ItemStack createItemStack(String registryName) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(registryName);
            if (rl != null) {
                var item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) return new ItemStack(item);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}
