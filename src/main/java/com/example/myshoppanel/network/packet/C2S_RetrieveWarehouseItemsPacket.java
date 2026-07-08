package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：从冗余仓库取回物品。
 * 发送 pageIndex 和该页中被选中的 itemIndices 列表。
 */
public class C2S_RetrieveWarehouseItemsPacket {

    /** pageIndex 和 itemIndex 的配对 */
    private final int[] pageIndices;
    private final int[] itemIndices;

    public C2S_RetrieveWarehouseItemsPacket(int[] pageIndices, int[] itemIndices) {
        this.pageIndices = pageIndices;
        this.itemIndices = itemIndices;
    }

    public static void encode(C2S_RetrieveWarehouseItemsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.pageIndices.length);
        for (int i = 0; i < msg.pageIndices.length; i++) {
            buf.writeInt(msg.pageIndices[i]);
            buf.writeInt(msg.itemIndices[i]);
        }
    }

    public static C2S_RetrieveWarehouseItemsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        int[] pages = new int[count];
        int[] items = new int[count];
        for (int i = 0; i < count; i++) {
            pages[i] = buf.readInt();
            items[i] = buf.readInt();
        }
        return new C2S_RetrieveWarehouseItemsPacket(pages, items);
    }

    public static void handle(C2S_RetrieveWarehouseItemsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            RedundantWarehouseSavedData warehouse =
                    RedundantWarehouseSavedData.get(player.serverLevel());

            int retrieved = 0;
            // 倒序移除避免索引偏移
            for (int i = msg.pageIndices.length - 1; i >= 0; i--) {
                int pi = msg.pageIndices[i];
                int ii = msg.itemIndices[i];

                // 先获取物品信息以确定 maxStackSize
                var pages = warehouse.getPlayerPages(player.getUUID());
                ItemStack preview = null;
                if (pi < pages.size() && ii < pages.get(pi).items.size()) {
                    preview = pages.get(pi).items.get(ii).getItem();
                }
                int takeCount = (preview != null && !preview.isEmpty()) ? preview.getMaxStackSize() : 64;

                // 按组取回：每次只取 maxStackSize 个，剩余留在仓库
                ItemStack item = warehouse.removePartialItem(player.getUUID(), pi, ii, takeCount);
                if (!item.isEmpty()) {
                    if (!player.getInventory().add(item)) {
                        player.drop(item, false);
                    }
                    retrieved++;
                }
            }

            if (retrieved > 0) {
                player.sendSystemMessage(Component.literal(
                        "§a[MyShopPanel] 已从冗余仓库取回 §6" + retrieved + " §a组物品！"));
            } else {
                player.sendSystemMessage(Component.literal(
                        "§c[MyShopPanel] 取回失败：物品可能已过期被清除。"));
            }

            // 刷新仓库数据
            var pages = warehouse.getPlayerPages(player.getUUID());
            NetworkHandler.sendToPlayer(new S2C_WarehouseDataPacket(pages), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
