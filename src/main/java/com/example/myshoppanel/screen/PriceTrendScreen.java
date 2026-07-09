package com.example.myshoppanel.screen;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.*;
import com.example.myshoppanel.shop.QuoteGroupData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;

public class PriceTrendScreen extends Screen {

    private static final int LEFT_PANEL = 150;
    private static final int CHART_LEFT = LEFT_PANEL + 20;
    private static final int CHART_TOP = 40;
    private static final int CHART_WIDTH = 250;
    private static final int CHART_HEIGHT = 150;

    /** 玩家物品栏中独一无二的物品 ID 列表 */
    private final List<String> inventoryItems = new ArrayList<>();
    private String selectedItem = "";
    private List<QuoteGroupData.PriceSnapshot> snapshots = List.of();
    private int scrollIndex = 0;

    public PriceTrendScreen() {
        super(Component.translatable("my_shop_panel.menu.price_trend"));
    }

    @Override
    protected void init() {
        super.init();

        // 从玩家物品栏收集唯一物品
        buildInventoryItemList();

        int centerX = width / 2;
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"), b -> onClose()
        ).pos(centerX - 40, height - 28).size(80, 20).build());
    }

    /** 从玩家物品栏构建唯一物品列表 */
    private void buildInventoryItemList() {
        inventoryItems.clear();
        if (minecraft == null || minecraft.player == null) return;
        Set<String> seen = new LinkedHashSet<>();
        // 遍历主物品栏（36格：9快捷栏 + 27背包）
        for (int i = 0; i < 36; i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = stack.getItem().builtInRegistryHolder().key().location().toString();
            if (!seen.contains(id)) {
                seen.add(id);
                inventoryItems.add(id);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = width / 2;
        graphics.drawCenteredString(font, Component.translatable("my_shop_panel.trend.title").getString(), centerX, 10, 0xFFD700);

        // 加载最新数据
        String cachedId = S2C_PriceHistoryPacket.PriceCache.getItemId();
        if (!cachedId.isEmpty() && cachedId.equals(selectedItem)) {
            snapshots = S2C_PriceHistoryPacket.PriceCache.get();
        }

        // 左侧物品列表（玩家物品栏）
        drawLeftPanel(graphics, mouseX, mouseY);

        // 右侧图表
        drawChart(graphics);
    }

    private void drawLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(5, 30, LEFT_PANEL, height - 35, 0x22000000);
        graphics.drawString(font, Component.translatable("my_shop_panel.trend.inventory").getString(), 10, 32, 0xAAAAAA);

        int y = 48;
        int visible = (height - 90) / 12;
        int maxScroll = Math.max(0, inventoryItems.size() - visible);

        for (int i = scrollIndex; i < Math.min(inventoryItems.size(), scrollIndex + visible); i++) {
            String id = inventoryItems.get(i);
            boolean hovered = mouseX >= 5 && mouseX <= LEFT_PANEL && mouseY >= y && mouseY <= y + 11;
            boolean selected = id.equals(selectedItem);

            int bgColor = selected ? 0x44FFD700 : (hovered ? 0x33FFFFFF : 0);
            if (bgColor != 0) graphics.fill(5, y, LEFT_PANEL, y + 11, bgColor);

            String display = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            if (font.width(display) > LEFT_PANEL - 20) {
                display = font.plainSubstrByWidth(display, LEFT_PANEL - 25) + "..";
            }
            graphics.drawString(font, display, 12, y + 1, 0xFFFFFF);

            y += 12;
        }

        if (scrollIndex > 0) {
            graphics.drawString(font, "§e▲", LEFT_PANEL / 2 - 4, height - 40, 0xFFFF00);
        }
        if (scrollIndex < maxScroll) {
            graphics.drawString(font, "§e▼", LEFT_PANEL / 2 - 4, height - 28, 0xFFFF00);
        }
    }

    private void drawChart(GuiGraphics graphics) {
        if (snapshots.isEmpty()) {
            graphics.drawString(font, Component.translatable("my_shop_panel.trend.select_hint").getString(), CHART_LEFT + 30, CHART_TOP + 60, 0xAAAAAA);
            return;
        }

        graphics.fill(CHART_LEFT - 2, CHART_TOP - 2, CHART_LEFT + CHART_WIDTH + 2, CHART_TOP + CHART_HEIGHT + 2, 0xFF333333);
        graphics.fill(CHART_LEFT, CHART_TOP, CHART_LEFT + CHART_WIDTH, CHART_TOP + CHART_HEIGHT, 0xFF111111);

        double minPrice = Double.MAX_VALUE, maxPrice = Double.MIN_VALUE;
        for (var s : snapshots) {
            if (s.price < minPrice) minPrice = s.price;
            if (s.price > maxPrice) maxPrice = s.price;
        }
        if (minPrice == maxPrice) { maxPrice = minPrice + 1; minPrice = minPrice - 1; }
        double range = maxPrice - minPrice;

        for (int i = 0; i <= 4; i++) {
            int gy = CHART_TOP + CHART_HEIGHT - (i * CHART_HEIGHT / 4);
            graphics.fill(CHART_LEFT, gy, CHART_LEFT + CHART_WIDTH, gy + 1, 0x33FFFFFF);
            double val = minPrice + (range * i / 4);
            String label = String.format("%.1f", val);
            graphics.drawString(font, "§7" + label, CHART_LEFT - font.width(label) - 3, gy - 4, 0xAAAAAA);
        }

        int n = snapshots.size();
        if (n >= 2) {
            for (int i = 0; i < n - 1; i++) {
                int x1 = CHART_LEFT + (i * CHART_WIDTH / (n - 1));
                int y1 = CHART_TOP + CHART_HEIGHT - (int)((snapshots.get(i).price - minPrice) / range * CHART_HEIGHT);
                int x2 = CHART_LEFT + ((i + 1) * CHART_WIDTH / (n - 1));
                int y2 = CHART_TOP + CHART_HEIGHT - (int)((snapshots.get(i + 1).price - minPrice) / range * CHART_HEIGHT);
                drawLine(graphics, x1, y1, x2, y2, 0xFF00FF00);
            }
        }

        for (int i = 0; i < n; i++) {
            int px = CHART_LEFT + (i * CHART_WIDTH / Math.max(n - 1, 1));
            int py = CHART_TOP + CHART_HEIGHT - (int)((snapshots.get(i).price - minPrice) / range * CHART_HEIGHT);
            graphics.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFD700);
        }

        String itemName = selectedItem.contains(":") ? selectedItem.substring(selectedItem.indexOf(':') + 1) : selectedItem;
        graphics.drawString(font, "§e" + itemName, CHART_LEFT + 5, CHART_TOP - 14, 0xFFFF00);

        double curPrice = snapshots.get(snapshots.size() - 1).price;
        graphics.drawString(font, Component.translatable("my_shop_panel.trend.current", String.format("%.1f", curPrice)).getString(),
                CHART_LEFT + 5, CHART_TOP + CHART_HEIGHT + 4, 0xFFFFFF);
    }

    private void drawLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x1 += sx; }
            if (e2 <= dx) { err += dx; y1 += sy; }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 滚动
            int visible = (height - 90) / 12;
            int maxScroll = Math.max(0, inventoryItems.size() - visible);
            if (mouseX >= 5 && mouseX <= LEFT_PANEL && mouseY >= height - 42 && mouseY <= height - 30) {
                if (scrollIndex > 0) scrollIndex--;
                return true;
            }
            if (mouseX >= 5 && mouseX <= LEFT_PANEL && mouseY >= height - 30 && mouseY <= height - 18) {
                if (scrollIndex < maxScroll) scrollIndex++;
                return true;
            }
            // 点击物品
            int y = 48;
            for (int i = scrollIndex; i < Math.min(inventoryItems.size(), scrollIndex + visible); i++) {
                if (mouseX >= 5 && mouseX <= LEFT_PANEL && mouseY >= y && mouseY <= y + 11) {
                    selectedItem = inventoryItems.get(i);
                    NetworkHandler.sendToServer(new C2S_RequestPriceHistoryPacket(selectedItem));
                    return true;
                }
                y += 12;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(new MainMenuScreen());
    }
}