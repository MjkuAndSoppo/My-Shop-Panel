package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_ConfirmTransactionPacket;
import com.example.myshoppanel.network.packet.C2S_RequestAdminShopDataPacket;
import com.example.myshoppanel.shop.AdminShopEntry;
import com.example.myshoppanel.shop.ShopUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AdminShopScreen extends BaseStoreScreen {

    private static final int SIDEBAR_WIDTH = 82;
    private int dividerX; // = imageWidth - SIDEBAR_WIDTH - 6

    private List<AdminShopEntry> entries = new ArrayList<>();
    private List<AdminShopEntry> displayEntries = new ArrayList<>();
    private boolean showBuying = true;
    private int page = 0;
    private boolean firstInit = true;
    private int ROWS_PER_PAGE = 7;
    private int selectedEntryIdx = -1;          // 选中的条目索引，用于空格快速购买

    public AdminShopScreen() {
        super(Component.translatable("my_shop_panel.title.admin_shop"), 340, 240);
    }

    @Override
    protected void init() {
        super.init();
        dividerX = imageWidth - SIDEBAR_WIDTH - 6;
        ROWS_PER_PAGE = computeRowsPerPage(24, 32, 35);

        int sideX = guiLeft + dividerX + 2;
        int btnW = SIDEBAR_WIDTH - 6;

        addRenderableWidget(Button.builder(
                Component.translatable(showBuying ? "my_shop_panel.admin.sell_tab_dim" : "my_shop_panel.admin.sell_tab"),
                btn -> {
                    showBuying = false;
                    filterEntries();
                    page = 0;
                    refreshWidgets();
                }
        ).pos(sideX + 3, guiTop + 44).size(btnW, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable(showBuying ? "my_shop_panel.admin.buy_tab" : "my_shop_panel.admin.buy_tab_dim"),
                btn -> {
                    showBuying = true;
                    filterEntries();
                    page = 0;
                    refreshWidgets();
                }
        ).pos(sideX + 3, guiTop + 70).size(btnW, 20).build());

        int bottomY = guiTop + imageHeight - 26;

        // 页码居中，翻页按钮在页码两边
        int mainWidth = dividerX - 4;
        int totalPages = Math.max(1, (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        int pageBlockWidth = 20 + font.width(PAGE_SAMPLE) + 20;
        int pageCenterX = guiLeft + mainWidth / 2;

        addRenderableWidget(Button.builder(
                Component.literal("§7◀"),
                btn -> { if (page > 0) { page--; refreshWidgets(); } }
        ).pos(pageCenterX - pageBlockWidth / 2, bottomY).size(20, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7▶"),
                btn -> {
                    int tp = (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
                    if (page < tp - 1) { page++; refreshWidgets(); }
                }
        ).pos(pageCenterX + pageBlockWidth / 2 - 20, bottomY).size(20, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"),
                btn -> handleClose()
        ).pos(guiLeft + imageWidth - 48, bottomY).size(44, 20).build());

        if (firstInit) {
            firstInit = false;
            NetworkHandler.sendToServer(new C2S_RequestAdminShopDataPacket());
        }
    }

    public void updateEntries(List<AdminShopEntry> allEntries) {
        this.entries = allEntries;
        filterEntries();
        if (page >= getTotalPages()) {
            page = Math.max(0, getTotalPages() - 1);
        }
        refreshWidgets();
    }

    private int getTotalPages() {
        return Math.max(1, (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private void filterEntries() {
        displayEntries = new ArrayList<>();
        for (AdminShopEntry e : entries) {
            if (showBuying && e.getMode() == AdminShopEntry.ShopMode.SELLING) {
                displayEntries.add(e);
            } else if (!showBuying && e.getMode() == AdminShopEntry.ShopMode.BUYING) {
                displayEntries.add(e);
            }
        }
    }

    private void refreshWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawCenteredInMain(graphics, Component.translatable("my_shop_panel.title.admin_shop").getString(), guiTop + 6, 0xFFFFAA00);

        int headerY = guiTop + 18;
        int col1 = guiLeft + 28;
        int col2 = guiLeft + dividerX - 30;
        String priceLabel = Component.translatable("my_shop_panel.label.price").getString();
        graphics.drawString(font, Component.translatable("my_shop_panel.label.quoted_items").getString(), col1, headerY, 0xFF888888);
        graphics.drawString(font, priceLabel, col2 - font.width(priceLabel), headerY, 0xFF888888);

        int sepY = guiTop + 28;
        graphics.fill(guiLeft + 4, sepY, guiLeft + dividerX, sepY + 1, 0xFF_4A4A6A);

        int listTop = sepY + 3;
        int rowH = 24;
        int startIdx = page * ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayEntries.size());

        if (displayEntries.isEmpty()) {
            String emptyMsg = showBuying
                    ? Component.translatable("my_shop_panel.empty.no_buy").getString()
                    : Component.translatable("my_shop_panel.empty.no_sell").getString();
            graphics.drawString(font, emptyMsg, guiLeft + 8, listTop + 40, 0xFF888888);
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                AdminShopEntry entry = displayEntries.get(i);
                int rowY = listTop + (i - startIdx) * rowH;

                boolean isSoldOut = !entry.isInfiniteStock() && entry.getStock() <= 0;
                boolean hovered = mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                        && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1;
                boolean isSelected = selectedEntryIdx == i;
                if (isSelected && !isSoldOut) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x44_FFFF00);
                } else if (hovered && !isSoldOut) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x33_FFFFFF);
                }

                // 物品图标（售空时半透明）
                ItemStack icon = getItemStack(entry.getItemRegistryName());
                if (!icon.isEmpty()) {
                    graphics.renderItem(icon, guiLeft + 8, rowY);
                    graphics.renderItemDecorations(font, icon, guiLeft + 8, rowY);
                    renderItemTooltipIfHovered(graphics, icon, guiLeft + 8, rowY, mouseX, mouseY);
                }

                // 物品名称（原版颜色，售空时灰色）
                drawComponent(graphics, icon.getHoverName(), guiLeft + 30, rowY + 3, isSoldOut);

                // 价格
                String priceStr = ClientBalanceData.format(entry.getPrice());
                Object priceColor = isSoldOut ? "§8" : "§6";
                graphics.drawString(font, (String) priceColor + priceStr, col2 - font.width(priceStr), rowY + 3, 0xFFFFAA00);

                // 库存状态
                String stockStr;
                if (entry.isInfiniteStock()) {
                    stockStr = "";
                } else if (entry.getStock() <= 0) {
                    stockStr = Component.translatable("my_shop_panel.stock.sold_out").getString();
                } else {
                    stockStr = Component.translatable("my_shop_panel.stock.x").getString() + entry.getStock();
                }
                if (!stockStr.isEmpty()) {
                    graphics.drawString(font, stockStr, col2 - font.width(priceStr) - font.width(stockStr) - 4, rowY + 3, 0xFFFFFFFF);
                }
            }
        }

        int bottomSepY = guiTop + imageHeight - 32;
        graphics.fill(guiLeft + 4, bottomSepY, guiLeft + dividerX, bottomSepY + 1, 0xFF_4A4A6A);

        int totalPages = Math.max(1, (displayEntries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        String pageStr = "§7" + (page + 1) + "/" + totalPages;
        drawCenteredInMain(graphics, pageStr, guiTop + imageHeight - 23, 0xFFAAAAAA);

        int sX = guiLeft + dividerX;

        drawBalanceLeft(graphics, sX + 4, guiTop + 8);

        graphics.fill(sX + 1, guiTop + 22, sX + SIDEBAR_WIDTH - 4, guiTop + 23, 0xFF_4A4A6A);

        graphics.drawString(font, Component.translatable("my_shop_panel.label.sell").getString(), sX + 4, guiTop + 33, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.label.buy").getString(), sX + 4, guiTop + 59, 0xFF888888);
        graphics.drawString(font, showBuying
                ? Component.translatable("my_shop_panel.hint.left_click_buy").getString()
                : Component.translatable("my_shop_panel.hint.left_click_sell").getString(),
                sX + 4, guiTop + 85, 0xFF888888);

        graphics.fill(guiLeft + dividerX, guiTop + 4, guiLeft + dividerX + 1,
                guiTop + imageHeight - 4, 0xFF_4A4A6A);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) return super.mouseClicked(mouseX, mouseY, button);

        int listTop = guiTop + 31;
        int rowH = 24;
        int startIdx = page * ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayEntries.size());

        for (int i = startIdx; i < endIdx; i++) {
            int rowY = listTop + (i - startIdx) * rowH;
            if (mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                    && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1) {
                AdminShopEntry entry = displayEntries.get(i);
                selectedEntryIdx = i; // 记录选中条目

                // 购买模式：左键打开购买弹窗
                if (showBuying && button == 0) {
                    if (!entry.isInfiniteStock() && entry.getStock() <= 0) {
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.displayClientMessage(
                                    Component.translatable("my_shop_panel.admin.msg.sold_out"), false);
                        }
                        return true;
                    }
                    if (minecraft != null) {
                        minecraft.setScreen(new AdminTransactionDialog(
                                entry, getItemStack(entry.getItemRegistryName()), true,
                                () -> minecraft.setScreen(this)));
                    }
                    return true;
                }

                // 出售模式：左键打开卖出弹窗
                if (!showBuying && button == 0) {
                    if (minecraft != null) {
                        minecraft.setScreen(new AdminTransactionDialog(
                                entry, getItemStack(entry.getItemRegistryName()), false,
                                () -> minecraft.setScreen(this)));
                    }
                    return true;
                }

                // 出售模式：右键买回 → 1.3倍价格
                if (!showBuying && button == 1) {
                    if (minecraft != null && minecraft.player != null) {
                        double buybackPrice = Math.ceil(entry.getPrice() * 1.3 * 100) / 100;
                        NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                                C2S_ConfirmTransactionPacket.TransactionType.ADMIN_BUYBACK,
                                null, buybackPrice, 1, entry.getEntryId()));
                    }
                    return true;
                }

                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE && selectedEntryIdx >= 0
                && selectedEntryIdx < displayEntries.size()) {
            AdminShopEntry entry = displayEntries.get(selectedEntryIdx);
            if (showBuying) {
                // 购买模式：快速购买1个
                handleSpaceQuickBuy(entry);
            } else {
                // 出售模式：快速买回1个（1.3倍价格）
                handleSpaceQuickBuyback(entry);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 空格购买模式 — 快速购买1个，无弹窗 */
    private void handleSpaceQuickBuy(AdminShopEntry entry) {
        if (minecraft == null || minecraft.player == null) return;
        if (!entry.isInfiniteStock() && entry.getStock() <= 0) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.admin.msg.sold_out"), false);
            return;
        }
        double cost = entry.getPrice();
        if (ClientBalanceData.balance < cost) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.admin.msg.insufficient", ShopUtils.fmt(cost), ClientBalanceData.format()), false);
            return;
        }
        NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                C2S_ConfirmTransactionPacket.TransactionType.ADMIN_BUY,
                null, entry.getPrice(), 1, entry.getEntryId()));
    }

    /** 空格出售模式 — 快速买回1个（1.3倍），无弹窗 */
    private void handleSpaceQuickBuyback(AdminShopEntry entry) {
        if (minecraft == null || minecraft.player == null) return;
        double buybackPrice = Math.ceil(entry.getPrice() * 1.3 * 100) / 100;
        if (ClientBalanceData.balance < buybackPrice) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.admin.msg.insufficient", ShopUtils.fmt(buybackPrice), ClientBalanceData.format()), false);
            return;
        }
        NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                C2S_ConfirmTransactionPacket.TransactionType.ADMIN_BUYBACK,
                null, buybackPrice, 1, entry.getEntryId()));
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
            com.mojang.logging.LogUtils.getLogger().warn("[MyShopPanel] 世界商店物品加载失败: {}", registryName, e);
        }
        return ItemStack.EMPTY;
    }

    private void drawCenteredInMain(GuiGraphics graphics, String text, int y, int color) {
        int mainWidth = dividerX - 4;
        int x = guiLeft + mainWidth / 2 - font.width(text) / 2;
        graphics.drawString(font, text, x, y, color);
    }
}
