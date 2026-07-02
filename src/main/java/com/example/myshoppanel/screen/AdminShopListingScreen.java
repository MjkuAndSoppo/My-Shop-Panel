package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import com.example.myshoppanel.network.NetworkHandler;
import com.example.myshoppanel.network.packet.C2S_AdminShopEditPacket;
import com.example.myshoppanel.shop.ShopUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 世界商店编辑-上架界面（与玩家市场上架界面相同，但不消耗物品）。
 */
public class AdminShopListingScreen extends BaseStoreScreen {

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 1;
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 4;
    private static final int GRID_Y_OFFSET = 130;

    private List<ItemStack> inventoryItems;
    private ItemStack selectedItem = ItemStack.EMPTY;
    private EditBox priceInput;
    private EditBox qtyInput;
    private String errorMessage = "";

    public AdminShopListingScreen() {
        super(Component.translatable("my_shop_panel.title.admin_listing"), 300, 260);
    }

    @Override
    protected void init() {
        super.init();

        if (minecraft == null || minecraft.player == null) {
            inventoryItems = new java.util.ArrayList<>();
        } else {
            inventoryItems = minecraft.player.getInventory().items;
        }

        priceInput = new EditBox(font, guiLeft + 20, guiTop + 70, 120, 20,
                Component.translatable("my_shop_panel.price_input"));
        priceInput.setMaxLength(10);
        priceInput.setFilter(s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*"));
        priceInput.setValue("1");
        addRenderableWidget(priceInput);

        qtyInput = new EditBox(font, guiLeft + 160, guiTop + 70, 120, 20,
                Component.translatable("my_shop_panel.admin.stock_qty_label"));
        qtyInput.setMaxLength(3);
        qtyInput.setFilter(s -> s.matches("\\d*"));
        qtyInput.setValue("1");
        addRenderableWidget(qtyInput);
        setInitialFocus(priceInput);

        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.admin.confirm_list"),
                btn -> handleConfirm()
        ).pos(guiLeft + 20, guiTop + 98).size(120, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.cancel"),
                btn -> handleCancel()
        ).pos(guiLeft + 160, guiTop + 98).size(120, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        drawCenteredString(graphics, Component.translatable("my_shop_panel.title.admin_listing").getString(), guiTop + 8, 0xFFFFD700);

        String balanceText = Component.translatable("my_shop_panel.label.balance").getString() + ClientBalanceData.format();
        graphics.drawString(font, balanceText, guiLeft + imageWidth - font.width(balanceText) - 8, guiTop + 8, 0xFFFFFFFF);

        if (!selectedItem.isEmpty()) {
            graphics.renderItem(selectedItem, guiLeft + 20, guiTop + 28);
            graphics.renderItemDecorations(font, selectedItem, guiLeft + 20, guiTop + 28);
            String desc = selectedItem.getDisplayName().getString();
            graphics.drawString(font, "§f" + desc, guiLeft + 42, guiTop + 34, 0xFFFFFFFF);
        } else {
            drawCenteredString(graphics, Component.translatable("my_shop_panel.hint.select_item").getString(), guiTop + 35, 0xFFAAAAAA);
        }

        graphics.drawString(font, Component.translatable("my_shop_panel.hint.price_label").getString(), guiLeft + 20, guiTop + 58, 0xFFAAAAAA);
        graphics.drawString(font, Component.translatable("my_shop_panel.hint.qty_label").getString(), guiLeft + 160, guiTop + 58, 0xFFAAAAAA);

        if (!errorMessage.isEmpty()) {
            drawCenteredString(graphics, "§c" + errorMessage, guiTop + 122, 0xFFFF5555);
        }

        // 背包物品网格标签 — 放在网格上方
        drawCenteredString(graphics, Component.translatable("my_shop_panel.hint.backpack_noconsume").getString(),
                guiTop + GRID_Y_OFFSET - 16, 0xFFAAAAAA);

        int gridWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = guiLeft + (imageWidth - gridWidth) / 2;
        int startY = guiTop + GRID_Y_OFFSET;

        ItemStack currentHovered = ItemStack.EMPTY;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotIndex = row * GRID_COLS + col;
                if (slotIndex >= inventoryItems.size()) break;

                int x = startX + col * (SLOT_SIZE + SLOT_GAP);
                int y = startY + row * (SLOT_SIZE + SLOT_GAP);

                boolean hovered = mouseX >= x && mouseX < x + SLOT_SIZE
                        && mouseY >= y && mouseY < y + SLOT_SIZE;
                boolean isSelected = !selectedItem.isEmpty()
                        && ItemStack.isSameItemSameTags(selectedItem, inventoryItems.get(slotIndex));

                ItemStack stack = inventoryItems.get(slotIndex);
                if (hovered && !stack.isEmpty()) {
                    currentHovered = stack;
                }

                int bgColor = isSelected ? 0xFF_5A8A4A : (hovered ? 0xFF_5A5A7A : 0xFF_2A2A3E);
                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, bgColor);
                int b = 0xFF_4A4A6A;
                graphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, b);
                graphics.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, b);
                graphics.fill(x - 1, y, x, y + SLOT_SIZE, b);
                graphics.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, b);

                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x + 2, y + 2);
                    graphics.renderItemDecorations(font, stack, x + 2, y + 2);
                }
            }
        }

        // 鼠标 hover tooltip
        if (!currentHovered.isEmpty()) {
            graphics.renderTooltip(font, currentHovered, mouseX, mouseY);
        }

        drawCenteredString(graphics, Component.translatable("my_shop_panel.hint.list_noconsume").getString(), guiTop + imageHeight - 12, 0xFF666666);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridWidth = GRID_COLS * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
            int startX = guiLeft + (imageWidth - gridWidth) / 2;
            int startY = guiTop + GRID_Y_OFFSET;

            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int slotIndex = row * GRID_COLS + col;
                    if (slotIndex >= inventoryItems.size()) break;

                    int x = startX + col * (SLOT_SIZE + SLOT_GAP);
                    int y = startY + row * (SLOT_SIZE + SLOT_GAP);

                    if (mouseX >= x && mouseX < x + SLOT_SIZE
                            && mouseY >= y && mouseY < y + SLOT_SIZE) {
                        ItemStack clicked = inventoryItems.get(slotIndex);
                        if (!clicked.isEmpty()) {
                            selectedItem = clicked.copy();
                            // 联动 ProjectE：EMC 值作默认标价
                            double emc = ShopUtils.getEMCDefaultPrice(clicked);
                            priceInput.setValue(emc > 1 ? String.valueOf((long) emc) : "1");
                            qtyInput.setValue("1");
                            errorMessage = "";
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            handleConfirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleConfirm() {
        if (selectedItem.isEmpty()) {
            errorMessage = Component.translatable("my_shop_panel.error.select_item").getString();
            return;
        }

        String rawPrice = priceInput.getValue();
        if (rawPrice.isEmpty()) {
            errorMessage = Component.translatable("my_shop_panel.error.enter_price").getString();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(rawPrice);
        } catch (NumberFormatException e) {
            errorMessage = Component.translatable("my_shop_panel.error.invalid_price").getString();
            return;
        }
        if (price <= 0) {
            errorMessage = Component.translatable("my_shop_panel.error.price_positive").getString();
            return;
        }

        String rawQty = qtyInput.getValue();
        int qty;
        try {
            qty = Integer.parseInt(rawQty);
        } catch (NumberFormatException e) {
            errorMessage = Component.translatable("my_shop_panel.error.invalid_qty").getString();
            return;
        }
        if (selectedItem.getMaxStackSize() <= 1) {
            qty = 1;
        } else if (qty <= 0) {
            errorMessage = Component.translatable("my_shop_panel.error.qty_positive").getString();
            return;
        }

        String regName = ForgeRegistries.ITEMS.getKey(selectedItem.getItem()).toString();
        String displayName = selectedItem.getDisplayName().getString();

        NetworkHandler.sendToServer(new C2S_AdminShopEditPacket(regName, displayName, price, qty));

        if (minecraft != null && minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal("§a[MyShopPanel] 世界商店物品已上架！"), false);

            // 回到编辑界面
            minecraft.setScreen(new AdminShopEditScreen());
        }
    }

    private void handleCancel() {
        if (minecraft != null) minecraft.setScreen(new AdminShopEditScreen());
    }
}
