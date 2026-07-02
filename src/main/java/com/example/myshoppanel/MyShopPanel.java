package com.example.myshoppanel;

import com.example.myshoppanel.command.MSPBlacklistCommands;
import com.example.myshoppanel.command.MSPBCommands;
import com.example.myshoppanel.command.MSPEditCommands;
import com.example.myshoppanel.command.MSPPCommands;
import com.example.myshoppanel.command.MSPTestCommands;
import com.example.myshoppanel.item.QuotationTerminalItem;
import com.example.myshoppanel.item.TerminalEventHandler;
import com.example.myshoppanel.item.TerminalKeyHandler;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.shop.AdminShopConfig;
import com.example.myshoppanel.shop.MarketBlacklist;
import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(MyShopPanel.MODID)
public class MyShopPanel
{
    public static final String MODID = "my_shop_panel";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 物品注册
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> QUOTATION_TERMINAL = ITEMS.register("quotation_terminal",
            QuotationTerminalItem::new);

    // 创造模式标签页
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> MY_SHOP_PANEL_TAB = CREATIVE_MODE_TABS.register("tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> QUOTATION_TERMINAL.get().getDefaultInstance())
                    .title(Component.literal("My Shop Panel"))
                    .displayItems((params, output) -> {
                        output.accept(QUOTATION_TERMINAL.get());
                    })
                    .build());

    /** 记录已发送仓库提醒的玩家（登录后1分钟） */
    private final Map<UUID, Integer> warehouseDelayTicks = new ConcurrentHashMap<>();

    public MyShopPanel(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        NetworkHandler.register();
        LOGGER.info("[MyShopPanel] Network registered.");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        AdminShopConfig.loadInstance();
        MarketBlacklist.loadInstance();
        LOGGER.info("[MyShopPanel] Server starting - My Shop Panel is ready!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MSPPCommands.register(event.getDispatcher());
        MSPBCommands.register(event.getDispatcher());
        MSPEditCommands.register(event.getDispatcher());
        MSPBlacklistCommands.register(event.getDispatcher());
        MSPTestCommands.register(event.getDispatcher());
        LOGGER.info("[MyShopPanel] MSPP, MSPB, MSPEdit, MSPBlacklist & MSPTest commands registered.");
    }

    /**
     * 服务端每tick：冗余仓库清理计时器 + 登录提醒延迟。
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null) return;

        int tick = server.getTickCount();

        // 每20tick（1秒）对所有已加载世界执行一次仓库清理
        if (tick % 20 == 0) {
            for (ServerLevel level : server.getAllLevels()) {
                RedundantWarehouseSavedData.get(level).tick();
            }
        }

        // 登录后1分钟(1200tick)提醒
        if (!warehouseDelayTicks.isEmpty()) {
            var iter = warehouseDelayTicks.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    iter.remove();
                    var sp = server.getPlayerList().getPlayer(entry.getKey());
                    if (sp != null) {
                        ServerLevel overworld = server.overworld();
                        if (overworld != null) {
                            RedundantWarehouseSavedData wh = RedundantWarehouseSavedData.get(overworld);
                            if (wh.hasItems(entry.getKey())) {
                                int count = wh.getItemCount(entry.getKey());
                                sp.sendSystemMessage(Component.literal(
                                        "§c§l⚠ [MyShopPanel] 你的冗余仓库中有 §6" + count
                                                + " §c§l件物品等待取回！请打开报价终端 → 冗余仓库 查看。"));
                            }
                        }
                    }
                } else {
                    entry.setValue(remaining);
                }
            }
        }
    }

    /**
     * 玩家登录：延迟1分钟后检查冗余仓库。
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp)) return;
        warehouseDelayTicks.put(sp.getUUID(), 1200);
    }
}
