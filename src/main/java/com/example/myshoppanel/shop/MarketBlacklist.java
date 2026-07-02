package com.example.myshoppanel.shop;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 玩家市场报价黑名单。
 * 从 config/msp_blacklist.toml 加载，所有存档共享。
 * 黑名单中的物品不可在玩家市场上架。
 */
public class MarketBlacklist {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "msp_blacklist.toml";

    private static MarketBlacklist instance;
    private final Set<String> blacklist = new HashSet<>();
    private Path configPath;

    public static MarketBlacklist getInstance() {
        return instance;
    }

    public static void loadInstance() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        instance = load(configDir);
    }

    private static MarketBlacklist load(Path configDir) {
        MarketBlacklist mb = new MarketBlacklist();
        mb.configPath = configDir.resolve(FILE_NAME);

        if (Files.exists(mb.configPath)) {
            FileConfig toml = FileConfig.builder(mb.configPath, TomlFormat.instance()).build();
            try {
                toml.load();
                List<String> items = toml.get("items");
                if (items != null) {
                    mb.blacklist.addAll(items);
                }
                LOGGER.info("[MyShopPanel] 报价黑名单已加载: {} 项", mb.blacklist.size());
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 加载黑名单失败", e);
            } finally {
                toml.close();
            }
        } else {
            mb.blacklist.add("my_shop_panel:quotation_terminal");
            mb.save();
        }
        return mb;
    }

    public void save() {
        FileConfig toml = FileConfig.builder(configPath, TomlFormat.instance()).build();
        try {
            Files.createDirectories(configPath.getParent());
            toml.set("items", new ArrayList<>(blacklist));
            toml.save();
            LOGGER.info("[MyShopPanel] 报价黑名单已保存: {} 项", blacklist.size());
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 保存黑名单失败", e);
        } finally {
            toml.close();
        }
    }

    public boolean add(String itemRegName) {
        if (blacklist.add(itemRegName)) {
            save();
            return true;
        }
        return false;
    }

    public boolean remove(String itemRegName) {
        if (blacklist.remove(itemRegName)) {
            save();
            return true;
        }
        return false;
    }

    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }

    public static boolean isBlacklisted(ItemStack stack) {
        if (instance == null) return false;
        String regName = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return instance.blacklist.contains(regName);
    }
}
