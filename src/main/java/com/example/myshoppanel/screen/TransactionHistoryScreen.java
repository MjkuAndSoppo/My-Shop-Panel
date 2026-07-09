package com.example.myshoppanel.screen;

import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_RequestTransactionHistoryPacket;
import com.example.myshoppanel.network.packet.S2C_TransactionHistoryPacket;
import com.example.myshoppanel.shop.ShopUtils;
import com.example.myshoppanel.shop.TransactionRecord;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TransactionHistoryScreen extends Screen {

    private static final int ITEMS_PER_PAGE = 12;
    private int page = 0;
    private List<TransactionRecord> records = List.of();
    private int scrollOffset = 0;

    public TransactionHistoryScreen() {
        super(Component.translatable("my_shop_panel.menu.history"));
    }

    @Override
    protected void init() {
        super.init();
        int btnW = 80, btnH = 20;
        int centerX = width / 2;

        // 请求数据
        NetworkHandler.sendToServer(new C2S_RequestTransactionHistoryPacket());

        // 上一页
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.history.prev_page"), b -> { if (page > 0) page--; }
        ).pos(centerX - 170, height - 28).size(btnW, btnH).build());

        // 下一页
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.history.next_page"), b -> {
                    if ((page + 1) * ITEMS_PER_PAGE < records.size()) page++;
                }
        ).pos(centerX + 90, height - 28).size(btnW, btnH).build());

        // 返回
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"), b -> onClose()
        ).pos(centerX - 40, height - 28).size(btnW, btnH).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        // 加载最新缓存
        records = S2C_TransactionHistoryPacket.TransactionHistoryCache.get();

        int centerX = width / 2;
        graphics.drawCenteredString(font, "§e§l交易记录", centerX, 12, 0xFFD700);

        if (records.isEmpty()) {
            graphics.drawCenteredString(font, "§7暂无交易记录", centerX, height / 2, 0xAAAAAA);
            return;
        }

        // 表头
        int y = 32;
        int[] colX = {20, 60, 140, 260, 310, 370};
        String[] headers = {"时间", "类型", "物品", "数量", "价格", "交易方"};
        for (int i = 0; i < headers.length; i++) {
            graphics.drawString(font, "§7" + headers[i], colX[i], y, 0xAAAAAA);
        }
        y += 14;

        // 分割线
        graphics.fill(20, y, width - 20, y + 1, 0x55FFFFFF);
        y += 4;

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, records.size());
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");

        for (int i = start; i < end; i++) {
            TransactionRecord r = records.get(i);
            String time = sdf.format(new Date(r.timestamp));
            String type = r.getTypeColor() + r.getTypeLabel();
            String item = "§f" + r.itemName;
            String qty = "§f" + r.quantity;
            String price = "§e" + ShopUtils.roundAmount(r.totalPrice);
            String party = "§f" + r.counterparty;

            graphics.drawString(font, time, colX[0], y, 0xCCCCCC);
            graphics.drawString(font, type, colX[1], y, 0xFFFFFF);
            graphics.drawString(font, item, colX[2], y, 0xFFFFFF);
            graphics.drawString(font, qty, colX[3], y, 0xFFFFFF);
            graphics.drawString(font, price, colX[4], y, 0xFFFFFF);
            graphics.drawString(font, party, colX[5], y, 0xFFFFFF);

            y += 12;
            if (y > height - 40) break;
        }

        // 页码
        int totalPages = (records.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        graphics.drawCenteredString(font, "§7第 " + (page + 1) + "/" + totalPages + " 页", centerX, height - 14, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(new MainMenuScreen());
    }
}