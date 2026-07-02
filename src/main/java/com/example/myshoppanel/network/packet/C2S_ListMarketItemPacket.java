package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.shop.MarketBlacklist;
import com.example.myshoppanel.shop.PlayerMarketSavedData;
import com.example.myshoppanel.shop.ShopUtils;
import com.example.myshoppanel.shop.TransactionService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_ListMarketItemPacket {

    private final ItemStack item;
    private final double price;
    private final int quantity;

    public C2S_ListMarketItemPacket(ItemStack item, double price, int quantity) {
        this.item = item.copy();
        this.item.setCount(quantity);
        this.price = price;
        this.quantity = quantity;
    }

    public static void encode(C2S_ListMarketItemPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.item.save(new CompoundTag()));
        buf.writeDouble(msg.price);
        buf.writeInt(msg.quantity);
    }

    public static C2S_ListMarketItemPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        ItemStack item = tag != null ? ItemStack.of(tag) : ItemStack.EMPTY;
        double price = buf.readDouble();
        int quantity = buf.readInt();
        return new C2S_ListMarketItemPacket(item, price, quantity);
    }

    public static void handle(C2S_ListMarketItemPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 空物品检查
            if (msg.item.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 无效的物品。"));
                return;
            }
            if (msg.price <= 0) {
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 价格必须大于0。"));
                return;
            }
            if (msg.quantity <= 0) {
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 数量无效。"));
                return;
            }
            // 不可堆叠物品强制数量为1，同时校验数量不超过maxStackSize
            int maxStack = msg.item.getMaxStackSize();
            int actualQty;
            if (maxStack <= 1) {
                actualQty = 1;
            } else {
                actualQty = Math.min(msg.quantity, maxStack);
            }
            if (MarketBlacklist.isBlacklisted(msg.item)) {
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 该物品不可上架。"));
                return;
            }
            int found = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItemSameTags(stack, msg.item)) {
                    found += stack.getCount();
                    if (found >= actualQty) break;
                }
            }
            if (found < actualQty) {
                player.sendSystemMessage(Component.literal("§c[MyShopPanel] 背包中物品不足，请刷新重试。"));
                return;
            }
            ItemStack toList = msg.item.copy();
            toList.setCount(actualQty);
            PlayerMarketSavedData marketData = PlayerMarketSavedData.get(player.serverLevel());
            TransactionService.commitMarketList(player, toList, msg.price, marketData);
            player.sendSystemMessage(Component.literal(
                    "§a[MyShopPanel] 上架成功！§f" + toList.getDisplayName().getString()
                            + " x" + toList.getCount()
                            + " §f标价 §6" + ShopUtils.fmt(msg.price)));
        });
        ctx.get().setPacketHandled(true);
    }
}
