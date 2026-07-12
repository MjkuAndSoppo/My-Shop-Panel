package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.EditModeData;
import com.example.myshoppanel.shop.MainMenuConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class MainMenuScreen extends BaseStoreScreen {

    public MainMenuScreen() {
        super(Component.literal("My Shop Panel"), 220, 265);
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 180, btnHeight = 28;
        int btnX = guiLeft + (imageWidth - btnWidth) / 2;
        int y = guiTop + 30;
        int gap = 34;

        if (MainMenuConfig.isEnabled("player_market")) {
            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.menu.player_market"),
                    btn -> openPlayerMarket()
            ).pos(btnX, y).size(btnWidth, btnHeight).build());
            y += gap;
        }

        if (MainMenuConfig.isEnabled("admin_shop")) {
            int adminBtnWidth = EditModeData.enabled ? 140 : btnWidth;
            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.menu.global_shop"),
                    btn -> openAdminShop()
            ).pos(btnX, y).size(adminBtnWidth, btnHeight).build());

            if (EditModeData.enabled) {
                int editBtnX = btnX + adminBtnWidth + 5;
                addRenderableWidget(Button.builder(
                        Component.translatable("my_shop_panel.menu.edit"),
                        btn -> openAdminShopEdit()
                ).pos(editBtnX, y).size(btnWidth - adminBtnWidth - 5, btnHeight).build());
            }
            y += gap;
        }

        if (MainMenuConfig.isEnabled("warehouse")) {
            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.menu.warehouse"),
                    btn -> openRedundantWarehouse()
            ).pos(btnX, y).size(btnWidth, btnHeight).build());
            y += gap;
        }

        if (MainMenuConfig.isEnabled("history")) {
            addRenderableWidget(Button.builder(
                    Component.translatable("my_shop_panel.menu.history"),
                    btn -> openTransactionHistory()
            ).pos(btnX, y).size(btnWidth, btnHeight).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawCenteredString(graphics, "§e§lMy Shop Panel", guiTop + 10, 0xFFFFD700);
    }

    private void openPlayerMarket() { if (minecraft != null) minecraft.setScreen(new PlayerMarketScreen()); }
    private void openAdminShop() { if (minecraft != null) minecraft.setScreen(new AdminShopScreen()); }
    private void openAdminShopEdit() { if (minecraft != null) minecraft.setScreen(new AdminShopEditScreen()); }
    private void openRedundantWarehouse() { if (minecraft != null) minecraft.setScreen(new RedundantWarehouseScreen()); }
    private void openTransactionHistory() { if (minecraft != null) minecraft.setScreen(new TransactionHistoryScreen()); }
}
