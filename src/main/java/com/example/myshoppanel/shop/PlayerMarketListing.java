package com.example.myshoppanel.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class PlayerMarketListing {

    private final UUID listingId;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long timestamp;
    private final int displayId;

    public PlayerMarketListing(UUID listingId, UUID sellerUUID, String sellerName,
                               ItemStack item, double price, long timestamp, int displayId) {
        this.listingId = listingId;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.item = item.copy();
        this.price = price;
        this.timestamp = timestamp;
        this.displayId = displayId;
    }

    public UUID getListingId() { return listingId; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.copy(); }
    public double getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
    public int getDisplayId() { return displayId; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("ListingId", listingId);
        tag.putUUID("SellerUUID", sellerUUID);
        tag.putString("SellerName", sellerName);
        tag.put("Item", item.save(new CompoundTag()));
        tag.putDouble("Price", price);
        tag.putLong("Timestamp", timestamp);
        tag.putInt("DisplayId", displayId);
        return tag;
    }

    public static PlayerMarketListing deserializeNBT(CompoundTag tag) {
        UUID listingId = tag.getUUID("ListingId");
        UUID sellerUUID = tag.getUUID("SellerUUID");
        String sellerName = tag.getString("SellerName");
        ItemStack item = ItemStack.of(tag.getCompound("Item"));
        double price = tag.getDouble("Price");
        long timestamp = tag.getLong("Timestamp");
        int displayId = tag.getInt("DisplayId");
        return new PlayerMarketListing(listingId, sellerUUID, sellerName, item, price, timestamp, displayId);
    }
}
