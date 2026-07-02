package com.example.myshoppanel.shop;

public class AdminShopEntry {

    public enum ShopMode {
        BUYING,
        SELLING
    }

    private String entryId;
    private String itemRegistryName;
    private String itemDisplayName;
    private double price;
    private ShopMode mode;
    private boolean infiniteStock;
    private int stock;

    public AdminShopEntry() {}

    public AdminShopEntry(String entryId, String itemRegistryName, String itemDisplayName,
                          double price, ShopMode mode, boolean infiniteStock, int stock) {
        this.entryId = entryId;
        this.itemRegistryName = itemRegistryName;
        this.itemDisplayName = itemDisplayName;
        this.price = price;
        this.mode = mode;
        this.infiniteStock = infiniteStock;
        this.stock = stock;
    }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getItemRegistryName() { return itemRegistryName; }
    public void setItemRegistryName(String itemRegistryName) { this.itemRegistryName = itemRegistryName; }

    public String getItemDisplayName() { return itemDisplayName; }
    public void setItemDisplayName(String itemDisplayName) { this.itemDisplayName = itemDisplayName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public ShopMode getMode() { return mode; }
    public void setMode(ShopMode mode) { this.mode = mode; }

    public boolean isInfiniteStock() { return infiniteStock; }
    public void setInfiniteStock(boolean infiniteStock) { this.infiniteStock = infiniteStock; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }
}
