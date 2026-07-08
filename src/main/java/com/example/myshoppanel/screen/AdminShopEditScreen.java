package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_AdminShopEditPacket;
import com.example.myshoppanel.network.packet.C2S_RequestAdminShopDataPacket;
import com.example.myshoppanel.shop.AdminShopEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class AdminShopEditScreen extends BaseStoreScreen {

    private static final int SIDEBAR_WIDTH = 90;
    private int dividerX; // = imageWidth - SIDEBAR_WIDTH - 10

    private List<AdminShopEntry> entries = new ArrayList<>();
    private List<AdminShopEntry> displayEntries = new ArrayList<>();
    private int page = 0;
    private boolean firstInit = true;
    private int ROWS_PER_PAGE = 6;
    private String selectedEntryId = null;

    public AdminShopEditScreen() {
        super(Component.translatable("my_shop_panel.title.admin_edit"), 350, 240);
    }

    @Override
    protected void init() {
        super.init();
        dividerX = imageWidth - SIDEBAR_WIDTH - 10;
        ROWS_PER_PAGE = computeRowsPerPage(28, 44, 35);

        int sideX = guiLeft + dividerX + 5;
        int btnW = SIDEBAR_WIDTH - 10;

        // 新增按钮（打开上架界面）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.admin.add"),
                btn -> openListingScreen()
        ).pos(sideX, guiTop + 30).size(btnW / 2 - 2, 20).build());

        // 删除按钮
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.admin.remove"),
                btn -> removeEntry()
        ).pos(sideX + btnW / 2 + 2, guiTop + 30).size(btnW / 2 - 2, 20).build());

        // +1、-1 库存按钮
        addRenderableWidget(Button.builder(
                Component.literal("§e+1"),
                btn -> adjustStock(1)
        ).pos(sideX, guiTop + 60).size(btnW / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§e-1"),
                btn -> adjustStock(-1)
        ).pos(sideX + btnW / 2 + 2, guiTop + 60).size(btnW / 2 - 2, 20).build());

        // +10、-10 库存按钮
        addRenderableWidget(Button.builder(
                Component.literal("§e+10"),
                btn -> adjustStock(10)
        ).pos(sideX, guiTop + 86).size(btnW / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§e-10"),
                btn -> adjustStock(-10)
        ).pos(sideX + btnW / 2 + 2, guiTop + 86).size(btnW / 2 - 2, 20).build());

        // 售卖切换按钮（出售/收购）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.admin.toggle_mode"),
                btn -> toggleMode()
        ).pos(sideX, guiTop + 118).size(btnW, 20).build());

        // 库存开关按钮（限购/不限购）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.admin.toggle_stock"),
                btn -> toggleInfinite()
        ).pos(sideX, guiTop + 144).size(btnW, 20).build());

        // 底栏按钮
        int bottomY = guiTop + imageHeight - 26;

        // 页码居中，翻页按钮在页码两边
        int mainWidth = dividerX - 4;
        int totalPages = Math.max(1, (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        int pageBlockWidth = 24 + font.width(PAGE_SAMPLE) + 24;
        int pageCenterX = guiLeft + mainWidth / 2;

        addRenderableWidget(Button.builder(
                Component.literal("§7◀"),
                btn -> { if (page > 0) { page--; refreshWidgets(); } }
        ).pos(pageCenterX - pageBlockWidth / 2, bottomY).size(24, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7▶"),
                btn -> {
                    int tp = getTotalPages();
                    if (page < tp - 1) { page++; refreshWidgets(); }
                }
        ).pos(pageCenterX + pageBlockWidth / 2 - 24, bottomY).size(24, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"),
                btn -> handleClose()
        ).pos(guiLeft + imageWidth - 55, bottomY).size(48, 20).build());

        if (firstInit) {
            firstInit = false;
            displayEntries = new ArrayList<>();
            NetworkHandler.sendToServer(new C2S_RequestAdminShopDataPacket());
        }
    }

    public void updateEntries(List<AdminShopEntry> allEntries) {
        this.entries = allEntries;
        this.displayEntries = new ArrayList<>(allEntries);
        if (page >= getTotalPages()) {
            page = Math.max(0, getTotalPages() - 1);
        }
        refreshWidgets();
    }

    private int getTotalPages() {
        return Math.max(1, (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    private void openListingScreen() {
        if (minecraft != null) {
            minecraft.setScreen(new AdminShopListingScreen());
        }
    }

    private void removeEntry() {
        if (selectedEntryId == null) return;
        NetworkHandler.sendToServer(new C2S_AdminShopEditPacket(
                C2S_AdminShopEditPacket.EditType.REMOVE, selectedEntryId));
        selectedEntryId = null;
    }

    private void adjustStock(int delta) {
        if (selectedEntryId == null) return;
        AdminShopEntry entry = getSelectedEntry();
        if (entry != null && entry.isInfiniteStock()) return;
        NetworkHandler.sendToServer(new C2S_AdminShopEditPacket(
                C2S_AdminShopEditPacket.EditType.STOCK_ADJUST, selectedEntryId, delta));
    }

    private void toggleInfinite() {
        if (selectedEntryId == null) return;
        NetworkHandler.sendToServer(new C2S_AdminShopEditPacket(
                C2S_AdminShopEditPacket.EditType.TOGGLE_STOCK, selectedEntryId));
    }

    private void toggleMode() {
        if (selectedEntryId == null) return;
        NetworkHandler.sendToServer(new C2S_AdminShopEditPacket(
                C2S_AdminShopEditPacket.EditType.TOGGLE_MODE, selectedEntryId));
    }

    private AdminShopEntry getSelectedEntry() {
        if (selectedEntryId == null) return null;
        return entries.stream()
                .filter(e -> e.getEntryId().equals(selectedEntryId))
                .findFirst().orElse(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawCenteredInMain(graphics, Component.translatable("my_shop_panel.title.admin_edit").getString(), guiTop + 6, 0xFFFFD700);

        int headerY = guiTop + 22;
        graphics.drawString(font, Component.translatable("my_shop_panel.label.quote_column").getString(), guiLeft + 10, headerY, 0xFF888888);

        int sepY = guiTop + 32;
        graphics.fill(guiLeft + 4, sepY, guiLeft + dividerX, sepY + 1, 0xFF_4A4A6A);

        int listTop = sepY + 5;
        int rowH = 28;
        int startIdx = page * ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayEntries.size());

        if (displayEntries.isEmpty()) {
            graphics.drawString(font, Component.translatable("my_shop_panel.empty.no_items_add").getString(), guiLeft + 8, listTop + 40, 0xFF888888);
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                AdminShopEntry entry = displayEntries.get(i);
                int rowY = listTop + (i - startIdx) * rowH;

                boolean isSelected = entry.getEntryId().equals(selectedEntryId);
                boolean hovered = mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                        && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1;

                if (isSelected) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x66_FFFFFF);
                    graphics.fill(guiLeft + 5, rowY, guiLeft + dividerX - 1, rowY + rowH - 2, 0x33_FFFFFF);
                } else if (hovered) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x33_FFFFFF);
                }

                // 物品图标
                ItemStack icon = getItemStack(entry.getItemRegistryName());
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, guiLeft + 8, rowY + 4);
                    graphics.renderItemDecorations(font, icon, guiLeft + 8, rowY + 4);
                    renderItemTooltipIfHovered(graphics, icon, guiLeft + 8, rowY + 4, mouseX, mouseY);
                }

                // 物品名称（原版颜色）
                drawComponent(graphics, icon.getHoverName(), guiLeft + 30, rowY + 2);

                // 价格
                String priceStr = ClientBalanceData.format(entry.getPrice());
                int col2 = guiLeft + dividerX - 30;
                graphics.drawString(font, "§6" + priceStr, col2 - font.width(priceStr), rowY + 2, 0xFFFFAA00);

                // 售卖模式 + 库存状态
                String modeStr = entry.getMode() == AdminShopEntry.ShopMode.SELLING
                        ? Component.translatable("my_shop_panel.mode.selling").getString()
                        : Component.translatable("my_shop_panel.mode.buying").getString();
                String stockStr;
                if (entry.isInfiniteStock()) {
                    stockStr = Component.translatable("my_shop_panel.stock.unlimited").getString();
                } else if (entry.getStock() <= 0) {
                    stockStr = Component.translatable("my_shop_panel.stock.sold_out").getString();
                } else {
                    stockStr = Component.translatable("my_shop_panel.label.stock").getString() + entry.getStock();
                }
                graphics.drawString(font, modeStr + " " + stockStr, guiLeft + 30, rowY + 14, 0xFFFFFFFF);
            }
        }

        int bottomSepY = guiTop + imageHeight - 32;
        graphics.fill(guiLeft + 4, bottomSepY, guiLeft + dividerX, bottomSepY + 1, 0xFF_4A4A6A);

        String pageStr = Component.translatable("my_shop_panel.status.page", page + 1, getTotalPages()).getString();
        drawCenteredInMain(graphics, pageStr, guiTop + imageHeight - 23, 0xFFAAAAAA);

        int sX = guiLeft + dividerX;

        drawBalanceLeft(graphics, sX + 5, guiTop + 8);

        graphics.fill(sX + 1, guiTop + 22, sX + SIDEBAR_WIDTH - 5, guiTop + 23, 0xFF_4A4A6A);

        graphics.drawString(font, Component.translatable("my_shop_panel.label.stock_adjust").getString(), sX + 5, guiTop + 47, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.admin.toggle_mode").getString(), sX + 5, guiTop + 105, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.admin.toggle_stock").getString(), sX + 5, guiTop + 131, 0xFF888888);

        graphics.fill(guiLeft + dividerX, guiTop + 4, guiLeft + dividerX + 1,
                guiTop + imageHeight - 4, 0xFF_4A4A6A);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listTop = guiTop + 37;
            int rowH = 28;
            int startIdx = page * ROWS_PER_PAGE;
            int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayEntries.size());

            for (int i = startIdx; i < endIdx; i++) {
                int rowY = listTop + (i - startIdx) * rowH;
                if (mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                        && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1) {
                    AdminShopEntry entry = displayEntries.get(i);
                    selectedEntryId = entry.getEntryId();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleClose() {
        if (minecraft != null) minecraft.setScreen(new MainMenuScreen());
    }

    private static ItemStack getItemStack(String registryName) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(registryName);
            if (rl != null) {
                var item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    return new ItemStack(item);
                }
            }
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("[MyShopPanel] 加载物品失败: {}", registryName, e);
        }
        return ItemStack.EMPTY;
    }

    private void drawCenteredInMain(GuiGraphics graphics, String text, int y, int color) {
        int mainWidth = dividerX - 4;
        int x = guiLeft + mainWidth / 2 - font.width(text) / 2;
        graphics.drawString(font, text, x, y, color);
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }
}
