package com.example.myshoppanel.item;

import com.example.myshoppanel.MyShopPanel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 处理报价终端的右键实体事件（右键玩家 → 转账提示）。
 */
@Mod.EventBusSubscriber(modid = MyShopPanel.MODID)
public class TerminalEventHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        if (!(stack.getItem() instanceof QuotationTerminalItem)) return;
        if (player.level().isClientSide) return;

        if (event.getTarget() instanceof Player targetPlayer) {
            event.setCanceled(true);

            player.sendSystemMessage(
                    Component.literal("§e[MyShopPanel] §a向 §6" + targetPlayer.getName().getString() + " §a转账：")
                            .append(Component.literal("§b/msp pay " + targetPlayer.getName().getString() + " <金额>"))
            );
        }
    }
}