package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import com.example.myshoppanel.shop.WarehouseItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：冗余仓库数据。
 * 包含玩家的所有仓库页及其物品。
 */
public class S2C_WarehouseDataPacket {

    private final List<List<WarehouseItem>> pages; // 每页的物品列表
    private final List<Long> pageTimers;            // 每页的剩余刻数

    public S2C_WarehouseDataPacket(List<RedundantWarehouseSavedData.WarehousePage> pages) {
        this.pages = new ArrayList<>();
        this.pageTimers = new ArrayList<>();
        if (pages != null) {
            for (RedundantWarehouseSavedData.WarehousePage page : pages) {
                this.pages.add(new ArrayList<>(page.items));
                this.pageTimers.add(page.remainingTicks);
            }
        }
    }

    public List<List<WarehouseItem>> getPages() { return pages; }
    public List<Long> getPageTimers() { return pageTimers; }
    public int getTotalItems() {
        int count = 0;
        for (List<WarehouseItem> page : pages) count += page.size();
        return count;
    }

    public static void encode(S2C_WarehouseDataPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.pages.size());
        for (int i = 0; i < msg.pages.size(); i++) {
            List<WarehouseItem> page = msg.pages.get(i);
            buf.writeInt(page.size());
            for (WarehouseItem wi : page) {
                CompoundTag tag = wi.serializeNBT();
                buf.writeNbt(tag);
            }
            buf.writeLong(msg.pageTimers.get(i));
        }
    }

    public static S2C_WarehouseDataPacket decode(FriendlyByteBuf buf) {
        int pageCount = buf.readInt();
        List<RedundantWarehouseSavedData.WarehousePage> pages = new ArrayList<>();
        for (int p = 0; p < pageCount; p++) {
            int itemCount = buf.readInt();
            RedundantWarehouseSavedData.WarehousePage page =
                    new RedundantWarehouseSavedData.WarehousePage();
            for (int i = 0; i < itemCount; i++) {
                CompoundTag tag = buf.readNbt();
                if (tag != null) {
                    page.items.add(WarehouseItem.deserializeNBT(tag));
                }
            }
            page.remainingTicks = buf.readLong();
            pages.add(page);
        }
        return new S2C_WarehouseDataPacket(pages);
    }

    public static void handle(S2C_WarehouseDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.example.myshoppanel.screen.RedundantWarehouseScreen screen) {
                screen.updateData(msg.pages, msg.pageTimers);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
