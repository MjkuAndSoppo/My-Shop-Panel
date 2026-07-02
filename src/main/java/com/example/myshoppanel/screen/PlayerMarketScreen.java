package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_ConfirmTransactionPacket;
import com.example.myshoppanel.network.packet.C2S_DelistItemPacket;
import com.example.myshoppanel.network.packet.C2S_RequestMarketDataPacket;
import com.example.myshoppanel.shop.PlayerMarketListing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家市场界面。
 * 布局：左表（用户|物品|价格） + 右边栏（余额|购买/下架|我的报价|上架） + 底栏
 */
public class PlayerMarketScreen extends BaseStoreScreen {

    private static final int SIDEBAR_WIDTH = 82;
    private static final int DIVIDER_X_OFFSET = 254;

    private List<PlayerMarketListing> allListings = new ArrayList<>();
    private List<PlayerMarketListing> myListings = new ArrayList<>();
    private boolean showMyListings = false;
    private int page = 0;
    private boolean firstInit = true;
    private UUID selectedListingId = null;              // 全部报价模式单选
    private Set<UUID> multiSelectedIds = new HashSet<>(); // 我的报价模式多选
    private int ROWS_PER_PAGE = 7; // 由 init() 动态计算

    public PlayerMarketScreen() {
        super(Component.translatable("my_shop_panel.title.player_market"), 340, 240);
    }

    @Override
    protected void init() {
        super.init();
        ROWS_PER_PAGE = computeRowsPerPage(24, 32, 35);

        int sideX = guiLeft + DIVIDER_X_OFFSET + 2;
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
        int mainWidth = DIVIDER_X_OFFSET - 4;
        List<PlayerMarketListing> totalList = showMyListings ? myListings : allListings;
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
        List<PlayerMarketListing> displayList = showMyListings ? myListings : allListings;
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

        // 表头
        int headerY = guiTop + 18;
        int col1 = guiLeft + 8;
        int col2 = guiLeft + 62;
        int col3 = guiLeft + DIVIDER_X_OFFSET - 30;
        graphics.drawString(font, Component.translatable("my_shop_panel.label.user").getString(), col1, headerY, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.label.quoted_items").getString(), col2, headerY, 0xFF888888);
        graphics.drawString(font, Component.translatable("my_shop_panel.label.price").getString(), col3 - font.width(Component.translatable("my_shop_panel.label.price").getString()), headerY, 0xFF888888);

        int sepY = guiTop + 28;
        graphics.fill(guiLeft + 4, sepY, guiLeft + DIVIDER_X_OFFSET, sepY + 1, 0xFF_4A4A6A);

        // 列表
        List<PlayerMarketListing> displayList = showMyListings ? myListings : allListings;
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
                boolean hovered = mouseX >= guiLeft + 4 && mouseX <= guiLeft + DIVIDER_X_OFFSET
                        && mouseY >= rowY - 1 && mouseY <= rowY + rowH - 1;

                if (isSelected) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + DIVIDER_X_OFFSET, rowY + rowH - 1, 0x44_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + DIVIDER_X_OFFSET, rowY, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY + rowH - 1, guiLeft + DIVIDER_X_OFFSET, rowY + rowH, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + 4, rowY, guiLeft + 5, rowY + rowH - 1, 0xFF_FFFFFF);
                    graphics.fill(guiLeft + DIVIDER_X_OFFSET - 1, rowY, guiLeft + DIVIDER_X_OFFSET, rowY + rowH - 1, 0xFF_FFFFFF);
                } else if (hovered) {
                    graphics.fill(guiLeft + 4, rowY - 1, guiLeft + DIVIDER_X_OFFSET, rowY + rowH - 1, 0x33_FFFFFF);
                }

                // 用户列: #displayId 卖家名
                String seller = "#" + listing.getDisplayId() + " " + listing.getSellerName();
                graphics.drawString(font, "§f" + truncate(seller, 10), col1, rowY + 3, 0xFFFFFFFF);

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
        graphics.fill(guiLeft + 4, bottomSepY, guiLeft + DIVIDER_X_OFFSET, bottomSepY + 1, 0xFF_4A4A6A);

        int totalPages = Math.max(1, (displayList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        String pageStr = Component.translatable("my_shop_panel.status.page", page + 1, totalPages).getString();
        // 页码居中于翻页按钮之间
        int mainWidth = DIVIDER_X_OFFSET - 4;
        drawCenteredInMain(graphics, pageStr, guiTop + imageHeight - 23, 0xFFAAAAAA);

        // 右边栏
        int sX = guiLeft + DIVIDER_X_OFFSET;

        String balanceText = Component.translatable("my_shop_panel.label.balance").getString() + ClientBalanceData.format();
        graphics.drawString(font, balanceText, sX + 4, guiTop + 8, 0xFFFFFFFF);

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

        graphics.fill(guiLeft + DIVIDER_X_OFFSET, guiTop + 4, guiLeft + DIVIDER_X_OFFSET + 1,
                guiTop + imageHeight - 4, 0xFF_4A4A6A);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 只拦截左表列表区域的点击，底栏和右边栏由 super 处理
        int bottomBarY = guiTop + imageHeight - 32;
        boolean isLeftClick = button == 0;
        boolean isRightClick = button == 1;

        if ((isLeftClick || (showMyListings && isRightClick))
                && mouseX >= guiLeft + 4 && mouseX <= guiLeft + DIVIDER_X_OFFSET
                && mouseY < bottomBarY) {
            List<PlayerMarketListing> displayList = showMyListings ? myListings : allListings;
            int listTop = guiTop + 31;
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

    private void handleBuy() {
        if (selectedListingId == null || minecraft == null || minecraft.player == null) return;
        PlayerMarketListing selected = findSelected();
        if (selected == null) {
            minecraft.player.displayClientMessage(
                    Component.literal("§c[MyShopPanel] 该报价已失效，请重新选择。"), false);
            return;
        }

        // 余额为负数时禁止购买
        if (ClientBalanceData.balance < 0) {
            minecraft.player.displayClientMessage(
                    Component.literal("§c[MyShopPanel] 余额不足，当前余额: §6"
                            + ClientBalanceData.format() + "§c，无法购买。"), false);
            return;
        }

        final UUID listingIdToBuy = selected.getListingId();

        minecraft.setScreen(new ConfirmDialog(
                "购买 §6" + selected.getItem().getDisplayName().getString()
                        + " x" + selected.getItem().getCount()
                        + "\n\n卖家: §6" + selected.getSellerName()
                        + "\n价格: §6" + ClientBalanceData.format(selected.getPrice()),
                false,
                () -> {
                    NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(listingIdToBuy));
                    selectedListingId = null;
                },
                () -> {}
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
            List<PlayerMarketListing> displayList = showMyListings ? myListings : allListings;
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
        List<PlayerMarketListing> list = showMyListings ? myListings : allListings;
        for (PlayerMarketListing l : list) {
            if (l.getListingId().equals(selectedListingId)) return l;
        }
        return null;
    }

    /** 选中的挂单是否是自己上架的 */
    private boolean isOwnListing(PlayerMarketListing listing) {
        if (minecraft == null || minecraft.player == null) return false;
        return listing.getSellerUUID().equals(minecraft.player.getUUID());
    }

    private void drawCenteredInMain(GuiGraphics graphics, String text, int y, int color) {
        int mainWidth = DIVIDER_X_OFFSET - 4;
        int x = guiLeft + mainWidth / 2 - font.width(text) / 2;
        graphics.drawString(font, text, x, y, color);
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 1) + "…";
    }
}
