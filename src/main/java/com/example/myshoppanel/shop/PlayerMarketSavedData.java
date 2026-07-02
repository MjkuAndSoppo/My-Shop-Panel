package com.example.myshoppanel.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 玩家市场挂单持久化存储。
 * 存储路径：<世界文件夹>/data/player_market_data.dat
 */
public class PlayerMarketSavedData extends SavedData {

    private static final String DATA_NAME = "player_market_data";
    private final List<PlayerMarketListing> listings = new ArrayList<>();
    private int nextDisplayId = 100;

    // ========== 工厂方法 ==========

    public static PlayerMarketSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlayerMarketSavedData::load,
                PlayerMarketSavedData::new,
                DATA_NAME
        );
    }

    // ========== 读写 ==========

    public static PlayerMarketSavedData load(CompoundTag tag) {
        PlayerMarketSavedData data = new PlayerMarketSavedData();
        data.nextDisplayId = tag.getInt("NextDisplayId");
        if (data.nextDisplayId < 100) data.nextDisplayId = 100;
        ListTag list = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.listings.add(PlayerMarketListing.deserializeNBT(list.getCompound(i)));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        tag.putInt("NextDisplayId", nextDisplayId);
        ListTag list = new ListTag();
        for (PlayerMarketListing listing : listings) {
            list.add(listing.serializeNBT());
        }
        tag.put("Listings", list);
        return tag;
    }

    // ========== 业务方法 ==========

    /** 获取下一个展示ID并自增 */
    public int nextDisplayId() {
        return nextDisplayId++;
    }

    /** 添加挂单（自动分配displayId） */
    public void addListing(PlayerMarketListing listing) {
        listings.add(listing);
        setDirty();
    }

    /** 移除挂单（成交或撤回） */
    public boolean removeListing(UUID listingId) {
        boolean removed = listings.removeIf(l -> l.getListingId().equals(listingId));
        if (removed) setDirty();
        return removed;
    }

    /** 按展示ID移除挂单，返回被移除的挂单以便退回物品 */
    public Optional<PlayerMarketListing> removeListingByDisplayId(int displayId) {
        Optional<PlayerMarketListing> found = listings.stream()
                .filter(l -> l.getDisplayId() == displayId).findFirst();
        found.ifPresent(l -> {
            listings.remove(l);
            setDirty();
        });
        return found;
    }

    /** 获取指定挂单 */
    public Optional<PlayerMarketListing> getListing(UUID listingId) {
        return listings.stream().filter(l -> l.getListingId().equals(listingId)).findFirst();
    }

    /** 按展示ID获取挂单 */
    public Optional<PlayerMarketListing> getListingByDisplayId(int displayId) {
        return listings.stream().filter(l -> l.getDisplayId() == displayId).findFirst();
    }

    /** 获取所有挂单（按时间排序，用于展示） */
    public List<PlayerMarketListing> getAllListings() {
        return new ArrayList<>(listings);
    }

    /** 获取某玩家的所有挂单 */
    public List<PlayerMarketListing> getPlayerListings(UUID playerUUID) {
        return listings.stream()
                .filter(l -> l.getSellerUUID().equals(playerUUID))
                .collect(Collectors.toList());
    }
}
