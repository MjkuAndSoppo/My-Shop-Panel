package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_ConfirmTransactionPacket;
import com.example.myshoppanel.shop.AdminShopEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 世界商店交易弹窗。
 * 展示物品预览、余额、数量输入(-1/+1)、花费与剩余余额、确认/取消。
 */
public class AdminTransactionDialog extends BaseStoreScreen {

    private final AdminShopEntry entry;
    private final ItemStack icon;
    private final boolean isBuyMode; // true=购买, false=卖出
    private final Runnable onClose;  // 关闭后返回 AdminShopScreen

    private EditBox qtyInput;
    private int quantity = 1;
    private String errorMsg = "";

    public AdminTransactionDialog(AdminShopEntry entry, ItemStack icon, boolean isBuyMode, Runnable onClose) {
        super(Component.translatable(isBuyMode ? "my_shop_panel.title.admin_tx_buy" : "my_shop_panel.title.admin_tx_sell"), 260, 220);
        this.entry = entry;
        this.icon = icon != null ? icon : ItemStack.EMPTY;
        this.isBuyMode = isBuyMode;
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();

        int cx = guiLeft + imageWidth / 2;

        // 数量输入框（居中）
        qtyInput = new EditBox(font, cx - 30, guiTop + 82, 60, 20, Component.translatable("my_shop_panel.qty_input"));
        qtyInput.setMaxLength(3);
        qtyInput.setFilter(s -> s.matches("\\d*"));
        qtyInput.setValue("1");
        qtyInput.setResponder(s -> {
            try {
                int v = s.isEmpty() ? 0 : Integer.parseInt(s);
                quantity = Math.max(1, v);
            } catch (NumberFormatException ignored) {
                quantity = 1;
            }
        });
        addRenderableWidget(qtyInput);
        setInitialFocus(qtyInput);

        // -1 按钮（输入框左边）
        addRenderableWidget(Button.builder(
                Component.literal("§e-1"),
                btn -> adjustQty(-1)
        ).pos(cx - 52, guiTop + 82).size(20, 20).build());

        // +1 按钮（输入框右边）
        addRenderableWidget(Button.builder(
                Component.literal("§e+1"),
                btn -> adjustQty(1)
        ).pos(cx + 32, guiTop + 82).size(20, 20).build());

        int btnW = 100;
        int btnY = guiTop + imageHeight - 35;

        // 确认按钮（左侧）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.confirm"),
                btn -> handleConfirm()
        ).pos(guiLeft + 10, btnY).size(btnW, 24).build());

        // 取消按钮（右侧）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.cancel"),
                btn -> { onClose.run(); }
        ).pos(guiLeft + imageWidth - btnW - 10, btnY).size(btnW, 24).build());
    }

    private void adjustQty(int delta) {
        int max = isBuyMode ? getMaxBuyable() : getMaxSellable();
        int newQty = quantity + delta;
        if (newQty < 1) newQty = 1;
        if (newQty > max) newQty = max;
        quantity = newQty;
        qtyInput.setValue(String.valueOf(quantity));
    }

    private int getMaxBuyable() {
        int maxStock;
        if (entry.isInfiniteStock()) {
            maxStock = 999;
        } else {
            maxStock = entry.getStock();
        }
        if (maxStock <= 0) return 0;
        // 根据余额限制
        double maxAfford = ClientBalanceData.balance / Math.max(0.01, entry.getPrice());
        int byMoney = (int) maxAfford;
        int maxQty = Math.max(1, Math.min(maxStock, byMoney));
        return Math.min(maxQty, 999);
    }

    private int getMaxSellable() {
        if (minecraft == null || minecraft.player == null) return 1;
        int found = 0;
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (ItemStack.isSameItemSameTags(stack, icon)) {
                found += stack.getCount();
            }
        }
        return Math.min(found, 999);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose.run();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
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

        if (isBuyMode) {
            int max = getMaxBuyable();
            if (quantity > max) {
                errorMsg = Component.translatable("my_shop_panel.error.qty_exceed", max).getString();
                return;
            }
            if (!entry.isInfiniteStock() && entry.getStock() < quantity) {
                errorMsg = Component.translatable("my_shop_panel.error.stock_low").getString();
                return;
            }
            double total = entry.getPrice() * quantity;
            if (ClientBalanceData.balance < total) {
                errorMsg = Component.translatable("my_shop_panel.error.balance_low").getString();
                return;
            }
            NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                    C2S_ConfirmTransactionPacket.TransactionType.ADMIN_BUY,
                    null, entry.getPrice(), quantity, entry.getEntryId()));
        } else {
            int max = getMaxSellable();
            if (quantity > max) {
                errorMsg = Component.translatable("my_shop_panel.error.backpack_low", max).getString();
                return;
            }
            NetworkHandler.sendToServer(new C2S_ConfirmTransactionPacket(
                    C2S_ConfirmTransactionPacket.TransactionType.ADMIN_SELL,
                    null, entry.getPrice(), quantity, entry.getEntryId()));
        }

        onClose.run();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        String title = Component.translatable(isBuyMode ? "my_shop_panel.title.admin_tx_buy" : "my_shop_panel.title.admin_tx_sell").getString();
        drawCenteredString(graphics, title, guiTop + 10, 0xFFFFD700);

        // 余额
        String balanceText = Component.translatable("my_shop_panel.label.balance").getString() + ClientBalanceData.format();
        graphics.drawString(font, balanceText, guiLeft + imageWidth - font.width(balanceText) - 8, guiTop + 8, 0xFFFFFFFF);

        // 分隔线
        graphics.fill(guiLeft + 10, guiTop + 26, guiLeft + imageWidth - 10, guiTop + 27, 0xFF_4A4A6A);

        // 物品预览
        if (!icon.isEmpty()) {
            graphics.renderItem(icon, guiLeft + 15, guiTop + 35);
            graphics.renderItemDecorations(font, icon, guiLeft + 15, guiTop + 35);
            String itemName = entry.getItemDisplayName() != null
                    ? entry.getItemDisplayName() : icon.getHoverName().getString();
            graphics.drawString(font, "§f" + itemName, guiLeft + 35, guiTop + 40, 0xFFFFFFFF);
        }

        // 单价
        String unitPrice = Component.translatable("my_shop_panel.label.unit_price").getString() + ClientBalanceData.format(entry.getPrice());
        graphics.drawString(font, unitPrice, guiLeft + 15, guiTop + 58, 0xFFFFFFFF);

        // 数量标签
        graphics.drawString(font, Component.translatable("my_shop_panel.hint.qty_label").getString(), guiLeft + 15, guiTop + 84, 0xFFAAAAAA);

        // 花费与剩余（输入框下方）
        double unit = entry.getPrice();
        double totalCost = unit * quantity;
        double remaining = isBuyMode ? ClientBalanceData.balance - totalCost : ClientBalanceData.balance + totalCost;
        String costLine = (isBuyMode ? Component.translatable("my_shop_panel.label.cost") : Component.translatable("my_shop_panel.label.earn")).getString() + ClientBalanceData.format(totalCost);
        String remainLine = Component.translatable("my_shop_panel.label.remaining").getString() + ClientBalanceData.format(remaining);
        graphics.drawString(font, costLine, guiLeft + 15, guiTop + 110, 0xFFFFFFFF);
        graphics.drawString(font, remainLine, guiLeft + 15, guiTop + 126, 0xFFFFFFFF);

        // 透支警告
        if (isBuyMode && remaining < 0) {
            int warnY = guiTop + 145;
            graphics.fill(guiLeft + 10, warnY - 2, guiLeft + imageWidth - 10, warnY + 14, 0x55_FF0000);
            drawCenteredString(graphics, Component.translatable("my_shop_panel.error.overdraft_warn").getString(), warnY, 0xFFFF5555);
        }

        // 错误信息
        if (!errorMsg.isEmpty()) {
            drawCenteredString(graphics, errorMsg, guiTop + imageHeight - 48, 0xFFFF5555);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0xAA_000000);
        renderFrostedBackground(graphics);
    }
}
