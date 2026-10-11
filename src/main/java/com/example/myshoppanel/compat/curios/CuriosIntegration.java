package com.example.myshoppanel.compat.curios;

import com.example.myshoppanel.item.QuotationTerminalItem;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios API 软依赖集成层。
 *
 * <p>本类只在 Curios 已加载时被调用（调用方用 {@code ModList.get().isLoaded("curios")} 守卫），
 * 未安装 Curios 时本类不会被 JVM 加载，因此可以直接引用 Curios 的 API 类型而不会触发
 * NoClassDefFoundError / 专服 dist 问题。
 */
public final class CuriosIntegration {

    private CuriosIntegration() {}

    /** 玩家任意一个饰品槽中是否插着报价终端 */
    public static boolean hasTerminal(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> {
                    // handler.getCurios() = Map<String, ICurioStackHandler>（槽位 id -> 子库存）
                    for (var slotHandler : handler.getCurios().values()) {
                        // IDynamicStackHandler 不是 Iterable，按槽位索引读取
                        var stacks = slotHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            if (stacks.getStackInSlot(i).getItem() instanceof QuotationTerminalItem) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .orElse(false);
    }
}
