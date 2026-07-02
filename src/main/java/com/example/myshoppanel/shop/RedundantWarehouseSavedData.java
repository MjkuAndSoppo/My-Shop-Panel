package com.example.myshoppanel.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 冗余仓库持久化存储。
 * 存储路径：&lt;世界文件夹&gt;/data/redundant_warehouse.dat
 *
 * 结构：每个玩家一个List&lt;WarehousePage&gt;，每页最多54个物品(6×9)，
 * 每页有独立的倒计时（360000刻=5小时），归零时整页清除。
 * 玩家取回物品时该页倒计时重置。
 */
public class RedundantWarehouseSavedData extends SavedData {

    private static final String DATA_NAME = "redundant_warehouse";
    public static final int ITEMS_PER_PAGE = 54;    // 6×9
    public static final long PAGE_CLEAR_TICKS = 360000L; // 5小时

    public static class WarehousePage {
        public final List<WarehouseItem> items = new ArrayList<>();
        public long remainingTicks = PAGE_CLEAR_TICKS;

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("RemainingTicks", remainingTicks);
            ListTag itemsTag = new ListTag();
            for (WarehouseItem wi : items) {
                itemsTag.add(wi.serializeNBT());
            }
            tag.put("Items", itemsTag);
            return tag;
        }

        public static WarehousePage deserializeNBT(CompoundTag tag) {
            WarehousePage page = new WarehousePage();
            page.remainingTicks = tag.getLong("RemainingTicks");
            ListTag itemsTag = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < itemsTag.size(); i++) {
                page.items.add(WarehouseItem.deserializeNBT(itemsTag.getCompound(i)));
            }
            return page;
        }
    }

    /** key: player UUID, value: 按时间排序的仓库物品页列表 */
    private final Map<UUID, List<WarehousePage>> playerWarehouses = new HashMap<>();

    // ========== 工厂方法 ==========

    public static RedundantWarehouseSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RedundantWarehouseSavedData::load,
                RedundantWarehouseSavedData::new,
                DATA_NAME
        );
    }

    // ========== 读写 ==========

    public static RedundantWarehouseSavedData load(CompoundTag tag) {
        RedundantWarehouseSavedData data = new RedundantWarehouseSavedData();
        ListTag playersTag = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playersTag.size(); i++) {
            CompoundTag playerTag = playersTag.getCompound(i);
            UUID uuid = playerTag.getUUID("UUID");
            List<WarehousePage> pages = new ArrayList<>();
            ListTag pagesTag = playerTag.getList("Pages", Tag.TAG_COMPOUND);
            for (int j = 0; j < pagesTag.size(); j++) {
                pages.add(WarehousePage.deserializeNBT(pagesTag.getCompound(j)));
            }
            if (!pages.isEmpty()) {
                data.playerWarehouses.put(uuid, pages);
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag playersTag = new ListTag();
        for (Map.Entry<UUID, List<WarehousePage>> entry : playerWarehouses.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("UUID", entry.getKey());
            ListTag pagesTag = new ListTag();
            for (WarehousePage page : entry.getValue()) {
                pagesTag.add(page.serializeNBT());
            }
            playerTag.put("Pages", pagesTag);
            playersTag.add(playerTag);
        }
        tag.put("Players", playersTag);
        return tag;
    }

    // ========== 业务方法 ==========

    /** 向玩家仓库添加物品（自动分页） */
    public void addItem(UUID playerUUID, ItemStack item) {
        if (item.isEmpty()) return;
        List<WarehousePage> pages = playerWarehouses.computeIfAbsent(playerUUID, k -> new ArrayList<>());

        // 找最后一个未满的页，没有则新建
        WarehousePage targetPage = null;
        if (!pages.isEmpty()) {
            WarehousePage last = pages.get(pages.size() - 1);
            if (last.items.size() < ITEMS_PER_PAGE) {
                targetPage = last;
            }
        }
        if (targetPage == null) {
            targetPage = new WarehousePage();
            pages.add(targetPage);
        }

        targetPage.items.add(new WarehouseItem(item, System.currentTimeMillis()));
        setDirty();
    }

    /** 从仓库中移除指定页中的物品（按索引），并重置该页倒计时 */
    public ItemStack removeItem(UUID playerUUID, int pageIndex, int itemIndex) {
        List<WarehousePage> pages = playerWarehouses.get(playerUUID);
        if (pages == null || pageIndex < 0 || pageIndex >= pages.size()) return ItemStack.EMPTY;

        WarehousePage page = pages.get(pageIndex);
        if (itemIndex < 0 || itemIndex >= page.items.size()) return ItemStack.EMPTY;

        WarehouseItem removed = page.items.remove(itemIndex);
        // 取回物品时重置该页倒计时
        page.remainingTicks = PAGE_CLEAR_TICKS;

        // 若页为空则移除该页
        if (page.items.isEmpty()) {
            pages.remove(pageIndex);
            if (pages.isEmpty()) {
                playerWarehouses.remove(playerUUID);
            }
        }

        setDirty();
        return removed.getItem();
    }

    /** 导出指定玩家的所有物品页面（客户端展示用） */
    public List<WarehousePage> getPlayerPages(UUID playerUUID) {
        List<WarehousePage> pages = playerWarehouses.get(playerUUID);
        if (pages == null) return Collections.emptyList();
        // 深拷贝以避免并发问题
        List<WarehousePage> copy = new ArrayList<>();
        for (WarehousePage page : pages) {
            WarehousePage pc = new WarehousePage();
            pc.remainingTicks = page.remainingTicks;
            for (WarehouseItem wi : page.items) {
                pc.items.add(new WarehouseItem(wi.getItem(), wi.getAddedTimestamp()));
            }
            copy.add(pc);
        }
        return copy;
    }

    /** 检查玩家仓库是否有物品 */
    public boolean hasItems(UUID playerUUID) {
        List<WarehousePage> pages = playerWarehouses.get(playerUUID);
        return pages != null && !pages.isEmpty();
    }

    /** 获取仓库物品总数 */
    public int getItemCount(UUID playerUUID) {
        List<WarehousePage> pages = playerWarehouses.get(playerUUID);
        if (pages == null) return 0;
        int count = 0;
        for (WarehousePage page : pages) {
            count += page.items.size();
        }
        return count;
    }

    /** 服务端每tick调用：递减所有页面的倒计时，清除到期页面 */
    public void tick() {
        boolean changed = false;
        Iterator<Map.Entry<UUID, List<WarehousePage>>> playerIt = playerWarehouses.entrySet().iterator();
        while (playerIt.hasNext()) {
            Map.Entry<UUID, List<WarehousePage>> entry = playerIt.next();
            List<WarehousePage> pages = entry.getValue();
            Iterator<WarehousePage> pageIt = pages.iterator();
            while (pageIt.hasNext()) {
                WarehousePage page = pageIt.next();
                page.remainingTicks--;
                if (page.remainingTicks <= 0) {
                    pageIt.remove();
                    changed = true;
                }
            }
            if (pages.isEmpty()) {
                playerIt.remove();
            }
        }
        if (changed) setDirty();
    }
}
