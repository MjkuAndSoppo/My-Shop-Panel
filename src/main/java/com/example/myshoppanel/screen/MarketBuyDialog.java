package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_ConfirmTransactionPacket;
import com.example.myshoppanel.shop.PlayerMarketListing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 市场购买弹窗。支持控制购买数量，剩余数量保留在市场。
 */
public class MarketBuyDialog extends BaseStoreScreen {

    private final PlayerMarketListing listing;
    private final Runnable onClose;

    private EditBox qtyInput;
    private int quantity = 1;
    private int maxQty;
    private double unitPrice;
    private String errorMsg = "";

    public MarketBuyDialog(PlayerMarketListing listing, Runnable onClose) {
        super(Component.translatable("my_shop_panel.title.market_buy"), 260, 220);
        this.listing = listing;
        this.onClose = onClose;

        int stackCount = listing.getItem().getCount();
        this.maxQty = Math.min(stackCount, 999);
        this.unitPrice = listing.getPrice() / stackCount;
    }

    @Override
    protected void init() {
        super.init();

        int cx = guiLeft + imageWidth / 2;

        // 数量输入框（居中）
        qtyInput = new EditBox(font, cx - 30, guiTop + 80, 60, 20, Component.translatable("my_shop_panel.qty_input"));
        qtyInput.setMaxLength(3);
        qtyInput.setFilter(s -> s.matches("\\d*"));
        qtyInput.setValue("1");
        qtyInput.setResponder(s -> {
            try {
                int v = s.isEmpty() ? 0 : Integer.parseInt(s);
                quantity = Math.max(1, Math.min(v, maxQty));
            } catch (NumberFormatException ignored) {
                quantity = 1;
            }
        });
        addRenderableWidget(qtyInput);
        setInitialFocus(qtyInput);

        // -1 按钮
        addRenderableWidget(Button.builder(
                Component.literal("§e-1"),
                btn -> adjustQty(-1)
        ).pos(cx - 52, guiTop + 80).size(20, 20).build());

        // +1 按钮
        addRenderableWidget(Button.builder(
                Component.literal("§e+1"),
                btn -> adjustQty(1)
        ).pos(cx + 32, guiTop + 80).size(20, 20).build());

        int btnW = 100;
        int btnY = guiTop + imageHeight - 35;

        // 确认按钮
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.confirm"),
                btn -> handleConfirm()
        ).pos(guiLeft + 10, btnY).size(btnW, 24).build());

        // 取消按钮
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.cancel"),
                btn -> { onClose.run(); }
        ).pos(guiLeft + imageWidth - btnW - 10, btnY).size(btnW, 24).build());
    }

    private void adjustQty(int delta) {
        int newQty = quantity + delta;
        if (newQty < 1) newQty = maxQty;   // 0 按 -1 → 最大值
        if (newQty > maxQty) newQty = 1;   // 最大值按 +1 → 1
        quantity = newQty;
        qtyInput.setValue(String.valueOf(quantity));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            handleConfirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleConfirm() {
        if (quantity <= 0) {
            errorMsg = Component.translatable("my_shop_panel.error.qty_positive").getString();
            return;
        }
        if (quantity > maxQty) {
            errorMsg = "§c最多可购买 " + maxQty + " 个";
            return;
        }

        double totalCost = unitPrice * quantity;
        if (ClientBalanceData.balance < totalCost) {
            errorMsg = Component.translatable("my_shop_panel.error.balance_low").getString();
            return;
        }

        NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                listing.getListingId(), quantity));
        onClose.run();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        drawCenteredString(graphics, Component.translatable("my_shop_panel.title.market_buy").getString(), guiTop + 10, 0xFFFFD700);

        // 余额
        drawBalance(graphics, guiLeft + imageWidth - 8, guiTop + 8);

        // 分隔线
        graphics.fill(guiLeft + 10, guiTop + 26, guiLeft + imageWidth - 10, guiTop + 27, 0xFF_4A4A6A);

        // 物品预览
        ItemStack icon = listing.getItem();
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, guiLeft + 15, guiTop + 35);
            graphics.renderItemDecorations(font, icon, guiLeft + 15, guiTop + 35);
            graphics.drawString(font, "§f" + icon.getHoverName().getString(), guiLeft + 35, guiTop + 40, 0xFFFFFFFF);
        }

        // 卖家 & 库存
        String sellerText = "§7" + Component.translatable("my_shop_panel.label.seller").getString()
                + "§f " + listing.getSellerName()
                + "  §7" + Component.translatable("my_shop_panel.label.total_stock").getString()
                + "§f" + listing.getItem().getCount();
        graphics.drawString(font, sellerText, guiLeft + 15, guiTop + 56, 0xFFFFFFFF);

        // 数量标签
        graphics.drawString(font, Component.translatable("my_shop_panel.hint.qty_label").getString(), guiLeft + 15, guiTop + 82, 0xFFAAAAAA);

        // 花费与剩余
        double totalCost = unitPrice * quantity;
        double remaining = ClientBalanceData.balance - totalCost;
        String costLine = Component.translatable("my_shop_panel.label.cost").getString() + ClientBalanceData.format(totalCost);
        String remainLine = Component.translatable("my_shop_panel.label.remaining").getString() + ClientBalanceData.format(remaining);
        graphics.drawString(font, costLine, guiLeft + 15, guiTop + 108, 0xFFFFFFFF);
        graphics.drawString(font, remainLine, guiLeft + 15, guiTop + 124, 0xFFFFFFFF);

        // 透支警告
        if (remaining < 0) {
            int warnY = guiTop + 148;
            graphics.fill(guiLeft + 10, warnY - 2, guiLeft + imageWidth - 10, warnY + 14, 0x55_FF0000);
            drawCenteredString(graphics, Component.translatable("my_shop_panel.error.overdraft_warn").getString(), warnY, 0xFFFF5555);
        }

        // 错误信息
        if (!errorMsg.isEmpty()) {
            drawCenteredString(graphics, "§c" + errorMsg, guiTop + imageHeight - 48, 0xFFFF5555);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xAA_000000);
        renderFrostedBackground(graphics);
    }
}
