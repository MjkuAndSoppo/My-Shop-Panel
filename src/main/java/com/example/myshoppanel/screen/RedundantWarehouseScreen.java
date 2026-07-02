package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_RequestWarehouseDataPacket;
import com.example.myshoppanel.network.packet.C2S_RetrieveWarehouseItemsPacket;
import com.example.myshoppanel.shop.RedundantWarehouseSavedData;
import com.example.myshoppanel.shop.WarehouseItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 冗余仓库界面。
 * 6×9 网格展示玩家离线时被退回的物品，支持多选取回。
 * 不可放入物品，仅可取出。
 */
public class RedundantWarehouseScreen extends BaseStoreScreen {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 1;
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 6;
    private static final int ITEMS_PER_PAGE = GRID_COLS * GRID_ROWS;

    private List<List<WarehouseItem>> pages = new ArrayList<>();
    private List<Long> pageTimers = new ArrayList<>();
    private int currentPage = 0;
    private boolean firstInit = true;
    private Set<Long> selectedSlots = new HashSet<>(); // 使用 (pageIndex * ITEMS_PER_PAGE + itemIndex) 作为slot key
    private boolean showConfirmAll = false; // 一键取回二次确认状态
    private int rightBtnW = 80; // 右侧按钮宽度，render中需要引用

    public RedundantWarehouseScreen() {
        super(Component.translatable("my_shop_panel.title.warehouse"), 340, 270);
    }

    @Override
    protected void init() {
        super.init();
        imageHeight = Math.max(270, Math.min(this.height - 40, 480));
        guiLeft = (this.width - imageWidth) / 2;
        guiTop = (this.height - imageHeight) / 2;

        int btnW = 80;
        int bottomY = guiTop + imageHeight - 26;

        // 页码居中
        int totalPages = Math.max(1, pages.size());
        int pageBlockWidth = 20 + font.width(PAGE_SAMPLE) + 20;
        int pageCenterX = guiLeft + imageWidth / 2;

        addRenderableWidget(Button.builder(
                Component.literal("§7◀"),
                btn -> { if (currentPage > 0) { currentPage--; showConfirmAll = false; refreshWidgets(); } }
        ).pos(pageCenterX - pageBlockWidth / 2, bottomY).size(20, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7▶"),
                btn -> { if (currentPage < totalPages - 1) { currentPage++; showConfirmAll = false; refreshWidgets(); } }
        ).pos(pageCenterX + pageBlockWidth / 2 - 20, bottomY).size(20, 20).build());

        if (showConfirmAll) {
            // 二次确认界面
            int confirmW = 50;
            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.wh.confirm_clear"),
                    btn -> handleRetrieveAll()
            ).pos(guiLeft + imageWidth - btnW - 10, bottomY).size(btnW, 20).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.btn.cancel"),
                    btn -> { showConfirmAll = false; refreshWidgets(); }
            ).pos(guiLeft + imageWidth - btnW - 10, bottomY - 24).size(btnW, 20).build());
        } else {
            // 一键取回按钮（仓库有物品时显示，取回上方）
            int totalCount = 0;
            for (List<WarehouseItem> p : pages) totalCount += p.size();
            boolean hasItems = totalCount > 0;
            addRenderableWidget(Button.builder(
                    Component.translatable(hasItems ? "my_shop_panel.wh.retrieve_all" : "my_shop_panel.wh.retrieve_all_dim"),
                    btn -> { if (hasItems) { showConfirmAll = true; refreshWidgets(); } }
            ).pos(guiLeft + imageWidth - btnW - 10, bottomY - 24).size(btnW, 20).build());

            // 取回按钮（有选中时亮起）
            boolean hasSelection = !selectedSlots.isEmpty();
            addRenderableWidget(Button.builder(
                    Component.translatable(hasSelection ? "my_shop_panel.wh.retrieve" : "my_shop_panel.wh.retrieve_dim"),
                    btn -> { if (hasSelection) handleRetrieve(); }
            ).pos(guiLeft + imageWidth - btnW - 10, bottomY).size(btnW, 20).build());
        }

        // 返回按钮
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"),
                btn -> handleClose()
        ).pos(guiLeft + 10, bottomY).size(50, 20).build());

        if (firstInit) {
            firstInit = false;
            NetworkHandler.sendToServer(new C2S_RequestWarehouseDataPacket());
        }
    }

    public void updateData(List<List<WarehouseItem>> newPages, List<Long> newTimers) {
        this.pages = newPages != null ? newPages : new ArrayList<>();
        this.pageTimers = newTimers != null ? newTimers : new ArrayList<>();
        if (currentPage >= Math.max(1, pages.size())) {
            currentPage = Math.max(0, pages.size() - 1);
        }
        selectedSlots.clear();
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawCenteredString(graphics, Component.translatable("my_shop_panel.title.warehouse").getString(), guiTop + 6, 0xFFFF5555);

        // 余额
        String balanceText = Component.translatable("my_shop_panel.label.balance").getString() + ClientBalanceData.format();
        graphics.drawString(font, balanceText, guiLeft + imageWidth - font.width(balanceText) - 8,
                guiTop + 6, 0xFFFFFFFF);

        // 分隔线
        int sepY = guiTop + 22;
        graphics.fill(guiLeft + 4, sepY, guiLeft + imageWidth - 4, sepY + 1, 0xFF_4A4A6A);

        // 6x9 网格
        int gridWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = guiLeft + (imageWidth - gridWidth) / 2;
        int startY = guiTop + 28;

        if (pages.isEmpty() || currentPage >= pages.size()) {
            graphics.drawString(font, Component.translatable("my_shop_panel.empty.warehouse").getString(), guiLeft + 8, startY + 60, 0xFF888888);
        } else {
            List<WarehouseItem> currentItems = pages.get(currentPage);

            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int idx = row * GRID_COLS + col;
                    int x = startX + col * (SLOT_SIZE + SLOT_GAP);
                    int y = startY + row * (SLOT_SIZE + SLOT_GAP);

                    boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE
                            && mouseY >= y && mouseY < y + SLOT_SIZE;
                    long slotKey = (long) currentPage * ITEMS_PER_PAGE + idx;
                    boolean selected = selectedSlots.contains(slotKey);
                    boolean hasItem = idx < currentItems.size();

                    // 背景
                    int bgColor;
                    if (selected) {
                        bgColor = 0xFF_5A8A4A;
                    } else if (hovered && hasItem) {
                        bgColor = 0xFF_5A5A7A;
                    } else {
                        bgColor = 0xFF_2A2A3E;
                    }
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);

                    // 边框
                    int b = 0xFF_4A4A6A;
                    graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, b);
                    graphics.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, b);
                    graphics.fill(x - 1, y, x, y + SLOT_SIZE, b);
                    graphics.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, b);

                    if (hasItem) {
                        WarehouseItem wi = currentItems.get(idx);
                        ItemStack stack = wi.getItem();
                        graphics.renderItem(stack, x + 2, y + 2);
                        graphics.renderItemDecorations(font, stack, x + 2, y + 2);
                        renderItemTooltipIfHovered(graphics, stack, x + 2, y + 2, mouseX, mouseY);
                    }
                }
            }

            // 倒计时信息
            if (currentPage < pageTimers.size()) {
                long remaining = pageTimers.get(currentPage);
                long hours = remaining / 72000;
                long minutes = (remaining % 72000) / 1200;
                String timerStr = Component.translatable("my_shop_panel.status.warehouse_timer", hours + "h" + minutes + "m").getString();
                graphics.drawString(font, timerStr, guiLeft + 10, guiTop + imageHeight - 48, 0xFFFFFFFF);
            }
        }

        // 页码
        int totalPages = Math.max(1, pages.size());
        String pageStr = Component.translatable("my_shop_panel.status.page", currentPage + 1, totalPages).getString();
        drawCenteredString(graphics, pageStr, guiTop + imageHeight - 23, 0xFFAAAAAA);

        // 总物品数提示
        int totalCount = 0;
        for (List<WarehouseItem> p : pages) totalCount += p.size();
        String countStr = Component.translatable("my_shop_panel.status.total_items", totalCount).getString();
        graphics.drawString(font, countStr, guiLeft + 65, guiTop + imageHeight - 36, 0xFF888888);

        // 二次确认警告闪烁
        if (showConfirmAll && totalCount > 0) {
            int warnY = guiTop + imageHeight - 56;
            // 闪烁红色背景
            long tick = System.currentTimeMillis() / 200;
            int alpha = (tick % 2 == 0) ? 0xAA : 0x55;
            graphics.fill(guiLeft + 10, warnY - 2, guiLeft + imageWidth - rightBtnW - 20, warnY + 14,
                    (alpha << 24) | 0xFF0000);
            drawCenteredString(graphics, Component.translatable("my_shop_panel.error.retrieve_warn", totalCount).getString(),
                    warnY, 0xFFFF5555);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
            int startX = guiLeft + (imageWidth - gridWidth) / 2;
            int startY = guiTop + 28;

            if (currentPage < pages.size()) {
                List<WarehouseItem> currentItems = pages.get(currentPage);

                for (int row = 0; row < GRID_ROWS; row++) {
                    for (int col = 0; col < GRID_COLS; col++) {
                        int idx = row * GRID_COLS + col;
                        if (idx >= currentItems.size()) continue;

                        int x = startX + col * (SLOT_SIZE + SLOT_GAP);
                        int y = startY + row * (SLOT_SIZE + SLOT_GAP);

                        if (mouseX >= x && mouseX < x + SLOT_SIZE
                                && mouseY >= y && mouseY < y + SLOT_SIZE) {
                            long slotKey = (long) currentPage * ITEMS_PER_PAGE + idx;
                            if (selectedSlots.contains(slotKey)) {
                                selectedSlots.remove(slotKey);
                            } else {
                                selectedSlots.add(slotKey);
                            }
                            refreshWidgets();
                            return true;
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleRetrieve() {
        if (selectedSlots.isEmpty()) return;

        // 构建并排序：按(pageIdx, itemIdx)降序排列，避免列表移除后索引偏移
        List<long[]> pairs = new ArrayList<>();
        for (long slotKey : selectedSlots) {
            pairs.add(new long[]{slotKey / ITEMS_PER_PAGE, slotKey % ITEMS_PER_PAGE});
        }
        pairs.sort((a, b) -> {
            int pc = Long.compare(b[0], a[0]);
            return pc != 0 ? pc : Long.compare(b[1], a[1]);
        });

        int[] pageArr = new int[pairs.size()];
        int[] itemArr = new int[pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            pageArr[i] = (int) pairs.get(i)[0];
            itemArr[i] = (int) pairs.get(i)[1];
        }

        NetworkHandler.sendToServer(new C2S_RetrieveWarehouseItemsPacket(pageArr, itemArr));
        selectedSlots.clear();
    }

    /** 一键取回全部物品（遍历所有页所有槽位） */
    private void handleRetrieveAll() {
        List<Integer> piList = new ArrayList<>();
        List<Integer> iiList = new ArrayList<>();
        for (int p = 0; p < pages.size(); p++) {
            List<WarehouseItem> page = pages.get(p);
            for (int i = 0; i < page.size(); i++) {
                piList.add(p);
                iiList.add(i);
            }
        }

        if (piList.isEmpty()) return;

        int[] pageArr = piList.stream().mapToInt(Integer::intValue).toArray();
        int[] itemArr = iiList.stream().mapToInt(Integer::intValue).toArray();

        NetworkHandler.sendToServer(new C2S_RetrieveWarehouseItemsPacket(pageArr, itemArr));
        showConfirmAll = false;
        selectedSlots.clear();
    }

    private void handleClose() {
        if (minecraft != null) minecraft.setScreen(new MainMenuScreen());
    }
}
