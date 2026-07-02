package com.example.myshoppanel.item;

import com.example.myshoppanel.economy.MSPPointsSavedData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.S2C_OpenMenuPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QuotationTerminalItem extends Item {

    public QuotationTerminalItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer sp = (ServerPlayer) player;
        double balance = MSPPointsSavedData.get(sp.serverLevel()).getPoints(sp.getUUID());
        NetworkHandler.sendToPlayer(
                new S2C_OpenMenuPacket(S2C_OpenMenuPacket.MenuType.MAIN_MENU, balance), sp);
        return InteractionResultHolder.success(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("my_shop_panel.hint.right_click_menu"));
        tooltip.add(Component.translatable("my_shop_panel.hint.right_click_transfer"));
        String keyName = TerminalKeyMapping.OPEN_TERMINAL.getTranslatedKeyMessage().getString();
        tooltip.add(Component.translatable("my_shop_panel.hint.press_key_open", keyName));
    }
}
