package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.screen.PlayerMarketScreen;
import com.example.myshoppanel.shop.PlayerMarketListing;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2C_MarketDataPacket {

    private final List<PlayerMarketListing> allListings;
    private final List<PlayerMarketListing> myListings;
    private final double balance;

    public S2C_MarketDataPacket(List<PlayerMarketListing> allListings, List<PlayerMarketListing> myListings, double balance) {
        this.allListings = allListings != null ? allListings : new ArrayList<>();
        this.myListings = myListings != null ? myListings : new ArrayList<>();
        this.balance = balance;
    }

    public static void encode(S2C_MarketDataPacket msg, FriendlyByteBuf buf) {
        ListTag allTag = new ListTag();
        for (PlayerMarketListing listing : msg.allListings) {
            allTag.add(listing.serializeNBT());
        }
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("all", allTag);

        ListTag myTag = new ListTag();
        for (PlayerMarketListing listing : msg.myListings) {
            myTag.add(listing.serializeNBT());
        }
        wrapper.put("my", myTag);

        buf.writeNbt(wrapper);
        buf.writeDouble(msg.balance);
    }

    public static S2C_MarketDataPacket decode(FriendlyByteBuf buf) {
        CompoundTag wrapper = buf.readNbt();
        List<PlayerMarketListing> all = new ArrayList<>();
        List<PlayerMarketListing> my = new ArrayList<>();

        if (wrapper != null) {
            ListTag allTag = wrapper.getList("all", Tag.TAG_COMPOUND);
            for (Tag tag : allTag) {
                if (tag instanceof CompoundTag ct) {
                    all.add(PlayerMarketListing.deserializeNBT(ct));
                }
            }
            ListTag myTag = wrapper.getList("my", Tag.TAG_COMPOUND);
            for (Tag tag : myTag) {
                if (tag instanceof CompoundTag ct) {
                    my.add(PlayerMarketListing.deserializeNBT(ct));
                }
            }
        }
        double balance = buf.readDouble();
        return new S2C_MarketDataPacket(all, my, balance);
    }

    public static void handle(S2C_MarketDataPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientBalanceData.balance = msg.balance;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PlayerMarketScreen screen) {
                screen.updateListings(msg.allListings, msg.myListings);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
