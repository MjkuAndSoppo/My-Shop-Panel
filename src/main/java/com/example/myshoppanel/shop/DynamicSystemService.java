package com.example.myshoppanel.shop;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
 */
public class DynamicSystemService {

    private static final Logger LOGGER = LogUtils.getLogger();

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

        // 检查机器人已有挂单数量
        long existingBotListings = market.getAllListings().stream()
                .filter(l -> l.getSellerUUID().equals(DynamicSystemData.BOT_UUID))
                .count();
        if (existingBotListings >= cfg.getMaxListings()) return;

        int listed = 0;
        for (Map.Entry<String, Double> entry : cfg.getListingItems().entrySet()) {
            if (existingBotListings + listed >= cfg.getMaxListings()) break;

            String itemId = entry.getKey();
            double configPrice = entry.getValue();

            ItemStack stack = createItemStack(itemId);
            if (stack.isEmpty()) continue;

            double price = configPrice > 0 ? configPrice : ShopUtils.getEMCDefaultPrice(stack);
            if (price <= 0) price = 1.0;

            // 检查资金是否足够（机器人需要预留资金，这里简单检查）
            if (botBalance < cfg.getMinRetainedFunds() + price) continue;

            UUID listingId = UUID.randomUUID();
            int displayId = market.nextDisplayId();
            stack.setCount(1);
            PlayerMarketListing listing = new PlayerMarketListing(
                    listingId,
                    DynamicSystemData.BOT_UUID,
                    DynamicSystemData.BOT_NAME,
                    stack,
                    price,
                    System.currentTimeMillis(),
                    displayId
            );
            market.addListing(listing);
            listed++;
        }

        if (listed > 0) {
            LOGGER.info("[MyShopPanel] 动态系统上架了 {} 个物品 (余额: {} MSPP)", listed, ShopUtils.fmt(botBalance));
        }
    }

    /**
     * 注入机器人资金。
     * @param level 服务器世界
     * @param amount 注入金额（正数）
     * @param reason 来源说明
     */
    public static void injectBotFunds(ServerLevel level, double amount, String reason) {
        if (amount <= 0) return;
        MSPPointsSavedData points = MSPPointsSavedData.get(level);
        double newBalance = points.addPoints(DynamicSystemData.BOT_UUID, ShopUtils.roundAmount(amount));
        LOGGER.info("[MyShopPanel] 机器人资金注入 {} MSPP (原因: {})，当前余额: {} MSPP",
                ShopUtils.fmt(amount), reason, ShopUtils.fmt(newBalance));
    }

    /**
     * 扣除机器人资金（上架消耗等）。
     */
    public static boolean deductBotFunds(ServerLevel level, double amount) {
        if (amount <= 0) return true;
        MSPPointsSavedData points = MSPPointsSavedData.get(level);
        double bal = points.getPoints(DynamicSystemData.BOT_UUID);
        if (bal < amount) return false;
        points.cutPoints(DynamicSystemData.BOT_UUID, ShopUtils.roundAmount(amount));
        return true;
    }

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
