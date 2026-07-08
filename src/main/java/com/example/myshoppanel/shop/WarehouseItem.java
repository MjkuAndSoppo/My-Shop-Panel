package com.example.myshoppanel.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 冗余仓库中的单个物品条目。
 */
public class WarehouseItem {

    private ItemStack item;
    private final long addedTimestamp;

    public WarehouseItem(ItemStack item, long addedTimestamp) {
        this.item = item.copy();
        this.addedTimestamp = addedTimestamp;
    }

    public ItemStack getItem() { return item.copy(); }
    public long getAddedTimestamp() { return addedTimestamp; }
    public void setItem(ItemStack item) { this.item = item.copy(); }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        // 保存物品时把 Count 写为 int，避免 byte 溢出（>127 会变负数）
        CompoundTag itemTag = item.save(new CompoundTag());
        itemTag.putInt("Count", item.getCount());
        tag.put("Item", itemTag);
        tag.putLong("AddedAt", addedTimestamp);
        return tag;
    }

    public static WarehouseItem deserializeNBT(CompoundTag tag) {
        CompoundTag itemTag = tag.getCompound("Item");
        ItemStack item = ItemStack.of(itemTag);
        // 用 int 读取 Count 覆盖 byte 反序列化结果
        if (itemTag.contains("Count", 3)) { // 3 = TAG_Int
            item.setCount(itemTag.getInt("Count"));
        }
        long timestamp = tag.getLong("AddedAt");
        return new WarehouseItem(item, timestamp);
    }
}
