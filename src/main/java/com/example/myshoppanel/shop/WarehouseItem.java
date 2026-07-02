package com.example.myshoppanel.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 冗余仓库中的单个物品条目。
 */
public class WarehouseItem {

    private final ItemStack item;
    private final long addedTimestamp;

    public WarehouseItem(ItemStack item, long addedTimestamp) {
        this.item = item.copy();
        this.addedTimestamp = addedTimestamp;
    }

    public ItemStack getItem() { return item.copy(); }
    public long getAddedTimestamp() { return addedTimestamp; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", item.save(new CompoundTag()));
        tag.putLong("AddedAt", addedTimestamp);
        return tag;
    }

    public static WarehouseItem deserializeNBT(CompoundTag tag) {
        ItemStack item = ItemStack.of(tag.getCompound("Item"));
        long timestamp = tag.getLong("AddedAt");
        return new WarehouseItem(item, timestamp);
    }
}
