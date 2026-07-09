package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_ConfirmTransactionPacket;
import com.example.myshoppanel.network.packet.C2S_DelistItemPacket;
import com.example.myshoppanel.network.packet.C2S_RequestMarketDataPacket;
import com.example.myshoppanel.shop.DynamicSystemData;
import com.example.myshoppanel.shop.PlayerMarketListing;
import com.example.myshoppanel.shop.ShopUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.lwjgl.glfw.GLFW;

/**
 * 玩家市场界面。
 * 布局：左表（用户|物品|价格） + 右边栏（余额|购买/下架|我的报价|上架） + 底栏
 */
public class PlayerMarketScreen extends BaseStoreScreen {

    private static final int SIDEBAR_WIDTH = 82;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_WIDTH = 24;

    private int dividerX; // = imageWidth - SIDEBAR_WIDTH - 6
    private List<PlayerMarketListing> allListings = new ArrayList<>();
    private List<PlayerMarketListing> myListings = new ArrayList<>();
    private boolean showMyListings = false;
    private int page = 0;
    private boolean firstInit = true;
    private UUID selectedListingId = null;              // 全部报价模式单选
    private Set<UUID> multiSelectedIds = new HashSet<>(); // 我的报价模式多选
    private int ROWS_PER_PAGE = 7; // 由 init() 动态计算

    // 分类标签栏
    private final List<TabEntry> categoryTabs = new ArrayList<>();
    /** 当前有挂单的标签在 categoryTabs 中的索引（动态计算） */
    private final List<Integer> visibleTabIndices = new ArrayList<>();
    private int selectedTabIndex = 0; // 0 = 全部
    private int tabScrollOffset = 0;
    private static class TabEntry {
        final String tabId;
        final CreativeModeTab tab;
        final ItemStack icon;
        TabEntry(String tabId, CreativeModeTab tab, ItemStack icon) {
            this.tabId = tabId;
            this.tab = tab;
            this.icon = icon;
        }
    }

    public PlayerMarketScreen() {
        super(Component.translatable("my_shop_panel.title.player_market"), 340, 240);
    }

    @Override
    protected void init() {
        super.init();
        dividerX = imageWidth - SIDEBAR_WIDTH - 6;
        ROWS_PER_PAGE = computeRowsPerPage(24, 32, 35);

        // 初始化分类标签
        if (categoryTabs.isEmpty()) {
            initTabs();
        }

        int sideX = guiLeft + dividerX + 2;
        int btnW = SIDEBAR_WIDTH - 6;

        // === 动态：购买/下架按钮 ===
        PlayerMarketListing selected = findSelected();
        boolean isOwn = selected != null && isOwnListing(selected);

        if (showMyListings) {
            // 我的报价模式：有选中就显示下架按钮
            if (!multiSelectedIds.isEmpty()) {
                addRenderableWidget(Button.builder(
                        Component.translatable("my_shop_panel.mkt.delist_btn"),
                        btn -> handleDelist()
                ).pos(sideX + 3, guiTop + 44).size(btnW, 20).build());
            } else {
                addRenderableWidget(Button.builder(
                        Component.translatable("my_shop_panel.mkt.delist_btn_dim"),
                        btn -> {}
                ).pos(sideX + 3, guiTop + 44).size(btnW, 20).build());
            }
        } else {
            if (selected != null) {
                addRenderableWidget(Button.builder(
                        Component.translatable(isOwn ? "my_shop_panel.mkt.delist_btn" : "my_shop_panel.mkt.buy_btn"),
                        btn -> {
                            if (isOwn) {
                                handleDelist();
                            } else {
                                handleBuy();
                            }
                        }
                ).pos(sideX + 3, guiTop + 44).size(btnW, 20).build());
            } else {
                addRenderableWidget(Button.builder(
                        Component.translatable("my_shop_panel.mkt.buy_btn_dim"),
                        btn -> {}
                ).pos(sideX + 3, guiTop + 44).size(btnW, 20).build());
            }
        }

        // 我的报价/全部报价 切换
        addRenderableWidget(Button.builder(
                Component.translatable(showMyListings ? "my_shop_panel.mkt.all_listings" : "my_shop_panel.mkt.my_listings"),
                btn -> {
                    showMyListings = !showMyListings;
                    selectedListingId = null;
                    multiSelectedIds.clear();
                    page = 0;
                    refreshWidgets();
                }
        ).pos(sideX + 3, guiTop + 70).size(btnW, 20).build());

        // 上架按钮
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.mkt.list_btn"),
                btn -> handleList()
        ).pos(sideX + 3, guiTop + 96).size(btnW, 20).build());

        // === 底栏按钮 ===
        int bottomY = guiTop + imageHeight - 26;

        // 页码居中，翻页按钮在页码两边
        int mainWidth = dividerX - 4;
        List<PlayerMarketListing> totalList = showMyListings ? myListings : getFilteredListings();
        int totalPages = Math.max(1, (totalList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);

        int pageCenterX = guiLeft + mainWidth / 2;
        int pageBlockWidth = 20 + font.width(PAGE_SAMPLE) + 20;
        addRenderableWidget(Button.builder(
                Component.literal("§7◀"),
                btn -> { if (page > 0) { page--; refreshWidgets(); } }
        ).pos(pageCenterX - pageBlockWidth / 2, bottomY).size(20, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("§7▶"),
                btn -> {
                    int tp = (showMyListings ? myListings : allListings).size();
                    int tp2 = (tp + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
                    if (page < tp2 - 1) { page++; refreshWidgets(); }
                }
        ).pos(pageCenterX + pageBlockWidth / 2 - 20, bottomY).size(20, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.back"),
                btn -> handleClose()
        ).pos(guiLeft + imageWidth - 48, bottomY).size(44, 20).build());

        if (firstInit) {
            firstInit = false;
            NetworkHandler.sendToServer(new C2S_RequestMarketDataPacket());
        }
    }

    public void updateListings(List<PlayerMarketListing> all, List<PlayerMarketListing> my) {
        this.allListings = all;
        this.myListings = my;
        rebuildVisibleTabs();
        List<PlayerMarketListing> displayList = showMyListings ? myListings : getFilteredListings();
        int totalPages = Math.max(1, (displayList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        if (page >= totalPages) {
            page = Math.max(0, totalPages - 1);
        }
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

        drawCenteredInMain(graphics, Component.translatable("my_shop_panel.title.player_market").getString(), guiTop + 6, 0xFFFFD700);

        // 分类标签栏
        renderCategoryTabs(graphics, mouseX, mouseY);

        // 表头
        int headerY = guiTop + 44;
        int col1 = guiLeft + 8;
        int col2 = guiLeft + 104;
        int col3 = guiLeft + dividerX - 30;
        graphics.drawString(font, Component.translatable("my_shop_panel.label.user").getString(), col1, headerY, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.label.quoted_items").getString(), col2, headerY, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.label.price").getString(), col3 - font.width(Component.translatable("my_shop_panel.label.price").getString()), headerY, 0xFF888888);

        int sepY = guiTop + 54;
        graphics.fill(guiLeft + 4, sepY, guiLeft + dividerX, sepY + 1, 0xFF_4A4A6A);

        // 列表
        List<PlayerMarketListing> displayList = showMyListings ? myListings : getFilteredListings();
        int listTop = sepY + 3;
        int rowH = 24;
        int startIdx = page * ROWS_PER_PAGE;
        int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayList.size());

        if (displayList.isEmpty()) {
            graphics.drawString(font, Component.translatable("my_shop_panel.empty.no_listings").getString(), guiLeft + 8, listTop + 40, 0xFF888888);
        } else {
            for (int i = startIdx; i < endIdx; i++) {
                PlayerMarketListing listing = displayList.get(i);
                int rowY = listTop + (i - startIdx) * rowH;

                boolean isSelected = (selectedListingId != null
                        && selectedListingId.equals(listing.getListingId()))
                        || (showMyListings && multiSelectedIds.contains(listing.getListingId()));
                boolean hovered = mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                        && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1;

                if (isSelected) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x44_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY + rowH - 1, guiLeft + dividerX, rowY + rowH, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY, guiLeft + 5, rowY + rowH - 1, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + dividerX - 1, rowY, guiLeft + dividerX, rowY + rowH - 1, 0xFF_FFFFFF);
                } else if (hovered) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + dividerX, rowY + rowH - 1, 0x33_FFFFFF);
                }

                // 用户列: #displayId 卖家名
                String seller = "#" + listing.getDisplayId() + " " + listing.getSellerName();
                boolean isBot = listing.getSellerUUID().equals(DynamicSystemData.BOT_UUID);
                String sellerText = isBot ? "§9" + truncate(seller, 16) : "§f" + truncate(seller, 16);
                graphics.drawString(font, sellerText, col1, rowY + 3, 0xFFFFFFFF);

                // 物品图标 + 名称 x数量
                graphics.renderItem(listing.getItem(), col2, rowY);
                graphics.renderItemDecorations(font, listing.getItem(), col2, rowY);
                renderItemTooltipIfHovered(graphics, listing.getItem(), col2, rowY, mouseX, mouseY);

                drawComponent(graphics, listing.getItem().getHoverName(), col2 + 20, rowY + 3);
                String countStr = "§7x" + listing.getItem().getCount();
                graphics.drawString(font, countStr, col2 + 20 + font.width(listing.getItem().getHoverName()) + 2, rowY + 3, 0xFFFFFFFF);

                String priceStr = ClientBalanceData.format(listing.getPrice());
                graphics.drawString(font, "§6" + priceStr, col3 - font.width(priceStr), rowY + 3, 0xFFFFAA00);
            }
        }

        int bottomSepY = guiTop + imageHeight - 32;
        graphics.fill(guiLeft + 4, bottomSepY, guiLeft + dividerX, bottomSepY + 1, 0xFF_4A4A6A);

        int totalPages = Math.max(1, (displayList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        String pageStr = Component.translatable("my_shop_panel.status.page", page + 1, totalPages).getString();
        // 页码居中于翻页按钮之间
        int mainWidth = dividerX - 4;
        drawCenteredInMain(graphics, pageStr, guiTop + imageHeight - 23, 0xFFAAAAAA);

        // 右边栏
        int sX = guiLeft + dividerX;

        drawBalanceLeft(graphics, sX + 4, guiTop + 8);

        graphics.fill(sX + 1, guiTop + 22, sX + SIDEBAR_WIDTH - 4, guiTop + 23, 0xFF_4A4A6A);

        if (!showMyListings) {
            PlayerMarketListing sel = findSelected();
            String label = (sel != null && isOwnListing(sel))
                    ? Component.translatable("my_shop_panel.mkt.delist_btn_dim").getString()
                    : Component.translatable("my_shop_panel.mkt.buy_btn_dim").getString();
            graphics.drawString(font, label, sX + 4, guiTop + 33, 0xFF888888);
        } else {
            graphics.drawString(font, Component.translatable("my_shop_panel.mkt.delist_btn_dim").getString(), sX + 4, guiTop + 33, 0xFF888888);
        }
        graphics.drawString(font, Component.translatable("my_shop_panel.mkt.my_listings").getString(), sX + 4, guiTop + 59, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.mkt.list_btn").getString(), sX + 4, guiTop + 85, 0xFF888888);

        graphics.fill(guiLeft + dividerX, guiTop + 4, guiLeft + dividerX + 1,
                guiTop + imageHeight - 4, 0xFF_4A4A6A);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 优先处理标签栏点击
        if (button == 0 && handleTabClick(mouseX, mouseY)) {
            return true;
        }

        // 只拦截左表列表区域的点击，底栏和右边栏由 super 处理
        int bottomBarY = guiTop + imageHeight - 32;
        boolean isLeftClick = button == 0;
        boolean isRightClick = button == 1;

        if ((isLeftClick || (showMyListings && isRightClick))
                && mouseX >= guiLeft + 4 && mouseX <= guiLeft + dividerX
                && mouseY < bottomBarY) {
            List<PlayerMarketListing> displayList = showMyListings ? myListings : getFilteredListings();
            int listTop = guiTop + 57;
            int rowH = 24;
            int startIdx = page * ROWS_PER_PAGE;
            int endIdx = Math.min(startIdx + ROWS_PER_PAGE, displayList.size());

            for (int i = startIdx; i < endIdx; i++) {
                int rowY = listTop + (i - startIdx) * rowH;
                if (mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1) {
                    PlayerMarketListing listing = displayList.get(i);

                    if (showMyListings) {
                        if (isLeftClick) {
                            // 左键多选切换
                            if (multiSelectedIds.contains(listing.getListingId())) {
                                multiSelectedIds.remove(listing.getListingId());
                            } else {
                                multiSelectedIds.add(listing.getListingId());
                            }
                        } else {
                            // 右键取消其他仅保留当前
                            multiSelectedIds.clear();
                            multiSelectedIds.add(listing.getListingId());
                        }
                    } else {
                        selectedListingId = listing.getListingId();
                    }
                    refreshWidgets();
                    return true;
                }
            }

            // 在列表区内但没点到行 → 取消选中
            if (showMyListings) {
                if (!multiSelectedIds.isEmpty()) {
                    multiSelectedIds.clear();
                    refreshWidgets();
                }
            } else {
                if (selectedListingId != null) {
                    selectedListingId = null;
                    refreshWidgets();
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (showMyListings || selectedListingId == null) {
                return false; // 让文本框等消费
            }
            // 快速购买全部，无确认弹窗
            handleQuickBuyAll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** 空格快速购买全部 */
    private void handleQuickBuyAll() {
        if (selectedListingId == null || minecraft == null || minecraft.player == null) return;
        PlayerMarketListing selected = findSelected();
        if (selected == null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.expired"), false);
            return;
        }
        int qty = selected.getItem().getCount();
        if (qty < 1) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.sold_out"), false);
            return;
        }
        double cost = selected.getPrice() * qty;
        if (ClientBalanceData.balance < 0) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.insufficient", ClientBalanceData.format()), false);
            return;
        }
        if (ClientBalanceData.balance < cost) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.insufficient_need", ShopUtils.fmt(cost), ClientBalanceData.format()), false);
            return;
        }
        NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                selectedListingId, qty));
        selectedListingId = null;
        NetworkHandler.sendToServer(new C2S_RequestMarketDataPacket());
    }

    private void handleBuy() {
        if (selectedListingId == null || minecraft == null || minecraft.player == null) return;
        PlayerMarketListing selected = findSelected();
        if (selected == null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.expired"), false);
            return;
        }

        // 余额为负数时禁止购买
        if (ClientBalanceData.balance < 0) {
            minecraft.player.displayClientMessage(
                    Component.translatable("my_shop_panel.market.msg.insufficient", ClientBalanceData.format()), false);
            return;
        }

        minecraft.setScreen(new MarketBuyDialog(selected,
                () -> {
                    if (minecraft != null) {
                        selectedListingId = null;
                        minecraft.setScreen(new PlayerMarketScreen());
                    }
                }
        ));
    }

    private void handleDelist() {
        if (minecraft == null || minecraft.player == null) return;

        if (showMyListings) {
            // 多选批量下架
            for (UUID id : new HashSet<>(multiSelectedIds)) {
                PlayerMarketListing found = myListings.stream()
                        .filter(l -> l.getListingId().equals(id)).findFirst().orElse(null);
                if (found != null) {
                    NetworkHandler.sendToServer(new C2S_DelistItemPacket(found.getDisplayId()));
                    allListings.removeIf(l -> l.getListingId().equals(id));
                    myListings.removeIf(l -> l.getListingId().equals(id));
                }
            }
            multiSelectedIds.clear();
            List<PlayerMarketListing> displayList = showMyListings ? myListings : getFilteredListings();
            int totalPages = Math.max(1, (displayList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
            if (page >= totalPages) {
                page = Math.max(0, totalPages - 1);
            }
            refreshWidgets();
            return;
        }

        // 全部报价模式单选下架
        if (selectedListingId == null) return;
        PlayerMarketListing selected = findSelected();
        if (selected == null) return;

        final int displayId = selected.getDisplayId();
        NetworkHandler.sendToServer(new C2S_DelistItemPacket(displayId));

        allListings.removeIf(l -> l.getListingId().equals(selectedListingId));
        myListings.removeIf(l -> l.getListingId().equals(selectedListingId));
        selectedListingId = null;
        refreshWidgets();
    }

    private void handleList() {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.setScreen(new ListingSetupScreen());
    }

    private void handleClose() {
        if (minecraft != null) minecraft.setScreen(new MainMenuScreen());
    }

    /** 从当前显示列表中查找选中的挂单 */
    private PlayerMarketListing findSelected() {
        if (selectedListingId == null) return null;
        List<PlayerMarketListing> list = showMyListings ? myListings : getFilteredListings();
        for (PlayerMarketListing l : list) {
            if (l.getListingId().equals(selectedListingId)) return l;
        }
        return null;
    }

    // ===== 分类标签栏 =====

    private void initTabs() {
        categoryTabs.clear();
        // "全部" 标签
        categoryTabs.add(new TabEntry("__all__", null, ItemStack.EMPTY));
        try {
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                if (tab == null) continue;
                String tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).toString();
                ItemStack icon = tab.getIconItem();
                if (icon.isEmpty()) {
                    var displayItems = new ArrayList<ItemStack>();
                    tab.getDisplayItems().forEach(displayItems::add);
                    if (!displayItems.isEmpty()) {
                        icon = displayItems.get(0);
                    }
                }
                categoryTabs.add(new TabEntry(tabId, tab, icon));
            }
        } catch (Exception e) {
            // 忽略加载错误
        }
        rebuildVisibleTabs();
    }

    /** 重建可见标签列表：只保留有挂单的标签（+"全部"） */
    private void rebuildVisibleTabs() {
        visibleTabIndices.clear();
        visibleTabIndices.add(0); // "全部" 始终显示
        for (int i = 1; i < categoryTabs.size(); i++) {
            CreativeModeTab tab = categoryTabs.get(i).tab;
            if (tab == null) continue;
            for (PlayerMarketListing listing : allListings) {
                try {
                    if (tab.contains(listing.getItem())) {
                        visibleTabIndices.add(i);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
        // 修正选中索引
        if (selectedTabIndex >= visibleTabIndices.size()) {
            selectedTabIndex = 0;
        }
    }

    /**
     * 获取过滤后的挂单列表（按当前选中的分类标签过滤）。
     */
    private List<PlayerMarketListing> getFilteredListings() {
        if (selectedTabIndex == 0) return allListings;
        if (selectedTabIndex < 1 || selectedTabIndex >= visibleTabIndices.size()) return allListings;
        int origIdx = visibleTabIndices.get(selectedTabIndex);
        if (origIdx < 1 || origIdx >= categoryTabs.size()) return allListings;
        CreativeModeTab targetTab = categoryTabs.get(origIdx).tab;
        if (targetTab == null) return allListings;
        List<PlayerMarketListing> filtered = new ArrayList<>();
        for (PlayerMarketListing listing : allListings) {
            try {
                if (targetTab.contains(listing.getItem())) {
                    filtered.add(listing);
                }
            } catch (Exception ignored) {}
        }
        return filtered;
    }

    /** 绘制分类标签栏 */
    private void renderCategoryTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        int mainWidth = dividerX - 4;
        int tabAreaLeft = guiLeft + 4;
        int tabAreaRight = guiLeft + dividerX;
        int tabAreaY = guiTop + 18;
        int visibleCount = visibleTabIndices.size();
        int visibleTabs = Math.min(visibleCount, (mainWidth - 20) / (TAB_WIDTH + 2));

        // 左箭头
        if (tabScrollOffset > 0) {
            int arrowX = tabAreaLeft;
            graphics.fill(arrowX, tabAreaY, arrowX + 10, tabAreaY + TAB_HEIGHT, 0x44_FFFFFF);
            graphics.drawString(font, "§7◀", arrowX + 1, tabAreaY + 5, 0xFFAAAAAA);
            if (mouseX >= arrowX && mouseX < arrowX + 10 && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT) {
                graphics.fill(arrowX, tabAreaY, arrowX + 10, tabAreaY + TAB_HEIGHT, 0x66_FFFFFF);
            }
        }

        // 右箭头
        boolean hasMore = tabScrollOffset + visibleTabs < visibleCount;
        if (hasMore) {
            int arrowX = tabAreaRight - 12;
            graphics.fill(arrowX, tabAreaY, arrowX + 10, tabAreaY + TAB_HEIGHT, 0x44_FFFFFF);
            graphics.drawString(font, "§7▶", arrowX + 1, tabAreaY + 5, 0xFFAAAAAA);
            if (mouseX >= arrowX && mouseX < arrowX + 10 && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT) {
                graphics.fill(arrowX, tabAreaY, arrowX + 10, tabAreaY + TAB_HEIGHT, 0x66_FFFFFF);
            }
        }

        int startX = tabAreaLeft + (tabScrollOffset > 0 ? 12 : 2);
        int endIdx = Math.min(tabScrollOffset + visibleTabs, visibleCount);
        for (int vi = tabScrollOffset; vi < endIdx; vi++) {
            int displayIdx = vi - tabScrollOffset;
            int x = startX + displayIdx * (TAB_WIDTH + 2);
            boolean isSelected = (vi == selectedTabIndex);
            boolean hovered = mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT;

            int bgColor = isSelected ? 0xAA_FFFFFF : (hovered ? 0x66_FFFFFF : 0x33_FFFFFF);
            graphics.fill(x, tabAreaY, x + TAB_WIDTH, tabAreaY + TAB_HEIGHT, bgColor);

            if (vi == 0) {
                drawCenteredInTab(graphics, "§7★", x, tabAreaY, TAB_WIDTH);
            } else {
                int origIdx = visibleTabIndices.get(vi);
                TabEntry entry = categoryTabs.get(origIdx);
                if (!entry.icon.isEmpty()) {
                    graphics.renderItem(entry.icon, x + 4, tabAreaY + 2);
                }
            }
        }
    }

    private void drawCenteredInTab(GuiGraphics graphics, String text, int x, int y, int w) {
        int tx = x + w / 2 - font.width(text) / 2;
        graphics.drawString(font, text, tx, y + 5, 0xFFFFFFFF);
    }

    /** 处理标签栏点击 */
    private boolean handleTabClick(double mouseX, double mouseY) {
        int mainWidth = dividerX - 4;
        int tabAreaLeft = guiLeft + 4;
        int tabAreaRight = guiLeft + dividerX;
        int tabAreaY = guiTop + 18;
        int visibleCount = visibleTabIndices.size();
        int visibleTabs = Math.min(visibleCount, (mainWidth - 20) / (TAB_WIDTH + 2));

        // 左箭头
        if (tabScrollOffset > 0 && mouseX >= tabAreaLeft && mouseX < tabAreaLeft + 10
                && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT) {
            tabScrollOffset = Math.max(0, tabScrollOffset - 1);
            return true;
        }

        // 右箭头
        boolean hasMore = tabScrollOffset + visibleTabs < visibleCount;
        if (hasMore && mouseX >= tabAreaRight - 12 && mouseX < tabAreaRight - 2
                && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT) {
            tabScrollOffset = Math.min(visibleCount - visibleTabs, tabScrollOffset + 1);
            return true;
        }

        int startX = tabAreaLeft + (tabScrollOffset > 0 ? 12 : 2);
        int endIdx = Math.min(tabScrollOffset + visibleTabs, visibleCount);
        for (int vi = tabScrollOffset; vi < endIdx; vi++) {
            int displayIdx = vi - tabScrollOffset;
            int x = startX + displayIdx * (TAB_WIDTH + 2);
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= tabAreaY && mouseY < tabAreaY + TAB_HEIGHT) {
                if (selectedTabIndex != vi) {
                    selectedTabIndex = vi;
                    selectedListingId = null;
                    multiSelectedIds.clear();
                    page = 0;
                    refreshWidgets();
                }
                return true;
            }
        }
        return false;
    }

    /** 选中的挂单是否是自己上架的 */
    private boolean isOwnListing(PlayerMarketListing listing) {
        if (minecraft == null || minecraft.player == null) return false;
        return listing.getSellerUUID().equals(minecraft.player.getUUID());
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
