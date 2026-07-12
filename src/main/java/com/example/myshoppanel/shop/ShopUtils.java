package com.example.myshoppanel.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
        return String.format("%,.2f MSPP", v).replace(".", " .");
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
                LOGGER.info("[MyShopPanel] ProjectE 未安装，使用内置默认价格表。");
            }
        }
        return projectELoaded;
    }

    /**
     * 获取物品的默认标价。
     * 优先级：ProjectE EMC > 内置默认价格表 > 1.0
     */
    public static double getEMCDefaultPrice(ItemStack stack) {
        if (stack.isEmpty()) return 1.0;

        // 优先级 1：ProjectE EMC（联动功能高优先）
        if (isProjectELoaded()) {
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
        }

        // 优先级 2：内置默认价格表
        long defaultPrice = DefaultPricing.getDefaultPrice(stack);
        if (defaultPrice > 0) return (double) defaultPrice;

        return 1.0;
    }

    /**
     * 将物品给予玩家：按 maxStackSize 拆分成一组组放入背包，超出部分进冗余仓库。
     * 不发送任何提示消息，由调用方自行组装消息顺序。
     * @return 放入冗余仓库的数量，0 表示全部放入背包
     */
    public static int giveItemWithOverflow(ServerPlayer player, ItemStack item) {
        return giveItemWithOverflow(player, item, true);
    }

    /**
     * @param silent 保留参数兼容，目前统一静默，由调用方自行提示
     */
    public static int giveItemWithOverflow(ServerPlayer player, ItemStack item, boolean silent) {
        if (item.isEmpty()) return 0;

        int totalCount = item.getCount();
        int perStack = Math.min(item.getMaxStackSize(), 99);
        int warehoused = 0;

        // 拆分成多个 maxStackSize 的组放入背包
        int remaining = totalCount;
        while (remaining > 0) {
            int batch = Math.min(remaining, perStack);
            ItemStack batchStack = item.copy();
            batchStack.setCount(batch);
            player.getInventory().add(batchStack);
            remaining -= batch;

            // 如果 inventory.add() 之后 batchStack 还有剩余，说明背包满了
            if (!batchStack.isEmpty()) {
                remaining += batchStack.getCount();
                break;
            }
        }

        // 剩余全部进冗余仓库
        if (remaining > 0) {
            ItemStack overflow = item.copy();
            overflow.setCount(remaining);
            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(player.serverLevel());
            warehouse.addItem(player.getUUID(), overflow);
            warehoused = remaining;
        }

        return warehoused;
    }

    /** 发送"背包已满，物品已放入冗余仓库"提示 */
    public static void sendWarehouseOverflowMsg(ServerPlayer player, int count, String itemName) {
        if (count > 0) {
            player.sendSystemMessage(Component.literal(
                    "§e[MyShopPanel] 背包已满，§6" + count + "§e个§6"
                            + itemName + "§e已放入冗余仓库。"));
        }
    }
}
