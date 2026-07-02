package com.example.myshoppanel.shop;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 世界商店配置。
 * 存储路径：./config/msp_quote.toml
 * 所有存档共享同一份配置，确保内容一致。
 * 支持热重载。
 */
public class AdminShopConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILE_NAME = "msp_quote.toml";

    /** 静态单例，服务器启动时加载 */
    private static AdminShopConfig instance;

    public static AdminShopConfig getInstance() {
        return instance;
    }

    /**
     * 在服务器启动时调用，加载 config/msp_quote.toml。
     */
    public static void loadInstance() {
        Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        instance = load(configDir);
    }

    private final List<AdminShopEntry> entries = new ArrayList<>();
    private Path configPath;

    // ========== 加载 / 保存 ==========

    public static AdminShopConfig load(Path configDir) {
        AdminShopConfig config = new AdminShopConfig();
        config.configPath = configDir.resolve(FILE_NAME);

        if (Files.exists(config.configPath)) {
            FileConfig toml = FileConfig.builder(config.configPath, TomlFormat.instance()).build();
            try {
                toml.load();
                List<Config> entryConfigs = toml.get("entries");
                if (entryConfigs != null) {
                    for (Config entryCfg : entryConfigs) {
                        AdminShopEntry entry = new AdminShopEntry();
                        entry.setEntryId(entryCfg.get("entryId"));
                        entry.setItemRegistryName(entryCfg.get("itemRegistryName"));
                        entry.setItemDisplayName(entryCfg.get("itemDisplayName"));
                        entry.setPrice(((Number) entryCfg.getOrElse("price", 0)).doubleValue());
                        String modeStr = entryCfg.get("mode");
                        entry.setMode(modeStr != null ? AdminShopEntry.ShopMode.valueOf(modeStr) : AdminShopEntry.ShopMode.SELLING);
                        entry.setInfiniteStock(entryCfg.getOrElse("infiniteStock", true));
                        entry.setStock(entryCfg.getIntOrElse("stock", 0));
                        config.entries.add(entry);
                    }
                }
                LOGGER.info("[MyShopPanel] 世界商店配置已加载: {} 条目", config.entries.size());
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 加载世界商店配置失败", e);
            } finally {
                toml.close();
            }
        } else {
            // 创建默认空配置
            config.save();
        }
        return config;
    }

    public void save() {
        FileConfig toml = FileConfig.builder(configPath, TomlFormat.instance()).build();
        try {
            Files.createDirectories(configPath.getParent());
            List<Config> configList = new ArrayList<>();
            for (AdminShopEntry entry : entries) {
                Config c = Config.inMemory();
                c.set("entryId", entry.getEntryId());
                c.set("itemRegistryName", entry.getItemRegistryName());
                c.set("itemDisplayName", entry.getItemDisplayName());
                c.set("price", entry.getPrice());
                c.set("mode", entry.getMode().name());
                c.set("infiniteStock", entry.isInfiniteStock());
                c.set("stock", entry.getStock());
                configList.add(c);
            }
            toml.set("entries", configList);
            toml.save();
            LOGGER.info("[MyShopPanel] 世界商店配置已保存: {} 条目", entries.size());
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 保存世界商店配置失败", e);
        } finally {
            toml.close();
        }
    }

    // ========== 业务方法 ==========

    public void addEntry(AdminShopEntry entry) {
        entries.add(entry);
        save();
    }

    public boolean removeEntry(String entryId) {
        boolean removed = entries.removeIf(e -> e.getEntryId().equals(entryId));
        if (removed) save();
        return removed;
    }

    public AdminShopEntry getEntry(String entryId) {
        return entries.stream().filter(e -> e.getEntryId().equals(entryId)).findFirst().orElse(null);
    }

    public List<AdminShopEntry> getAllEntries() {
        return new ArrayList<>(entries);
    }
}
