package com.example.myshoppanel.network.packet;

import com.example.myshoppanel.client.ClientPacketHandlers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2C_EditModePacket {
    private final boolean enabled;

    public S2C_EditModePacket(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() { return enabled; }

    public static void encode(S2C_EditModePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static S2C_EditModePacket decode(FriendlyByteBuf buf) {
        return new S2C_EditModePacket(buf.readBoolean());
    }

    public static void handle(S2C_EditModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandlers.handleEditMode(msg)));
        ctx.get().setPacketHandled(true);
    }
}
