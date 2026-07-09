package com.example.myshoppanel.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主菜单按钮可见性配置。持久化到 msp_main_menu.json。
 * 键为按钮 ID，值为 true(显示)/false(隐藏)。
 */
public class MainMenuConfig {

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("my_shop_panel").resolve("msp_main_menu.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 默认全部显示
    private static final Map<String, Boolean> DEFAULT = new LinkedHashMap<>();
    static {
        DEFAULT.put("player_market", true);
        DEFAULT.put("admin_shop", true);
        DEFAULT.put("warehouse", true);
        DEFAULT.put("history", true);
        DEFAULT.put("price_trend", true);
    }

    private static Map<String, Boolean> config = new LinkedHashMap<>(DEFAULT);

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    Map<String, Boolean> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Boolean>>(){}.getType());
                    if (loaded != null) {
                        config.clear();
                        config.putAll(DEFAULT); // 确保新按钮有默认值
                        config.putAll(loaded);
                    }
                }
            } else {
                save();
            }
        } catch (Exception e) {
            config = new LinkedHashMap<>(DEFAULT);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception ignored) {}
    }

    public static boolean isEnabled(String buttonId) {
        return config.getOrDefault(buttonId, true);
    }

    public static void setEnabled(String buttonId, boolean enabled) {
        config.put(buttonId, enabled);
        save();
    }

    public static Map<String, Boolean> getAll() {
        return new LinkedHashMap<>(config);
    }
}