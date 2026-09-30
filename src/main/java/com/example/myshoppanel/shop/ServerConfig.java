package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务端通用配置。
 * 配置文件：./config/my_shop_panel/server.json
 */
public class ServerConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "server.json";

    /** I 键快捷打开商店时，是否需要玩家拥有报价终端 */
    private static boolean requireTerminalForHotkey = true;

    private static Path configPath = null;

    public static void load(Path configDir) {
        configPath = configDir.resolve(FILE_NAME);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                Map<String, Object> cfg = GSON.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
                if (cfg != null && cfg.get("requireTerminalForHotkey") instanceof Boolean) {
                    requireTerminalForHotkey = (Boolean) cfg.get("requireTerminalForHotkey");
                }
                LOGGER.info("[MyShopPanel] 服务端配置已加载: requireTerminalForHotkey={}", requireTerminalForHotkey);
            } catch (Exception e) {
                LOGGER.error("[MyShopPanel] 服务端配置加载失败，使用默认值", e);
            }
        } else {
            save();
        }
    }

    public static void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            Map<String, Object> cfg = new LinkedHashMap<>();
            cfg.put("requireTerminalForHotkey", requireTerminalForHotkey);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(cfg, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MyShopPanel] 服务端配置保存失败", e);
        }
    }

    public static boolean isRequireTerminalForHotkey() {
        return requireTerminalForHotkey;
    }

    public static void setRequireTerminalForHotkey(boolean value) {
        requireTerminalForHotkey = value;
    }
}