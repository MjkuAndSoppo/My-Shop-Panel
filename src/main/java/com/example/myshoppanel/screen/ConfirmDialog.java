package com.example.myshoppanel.screen;

import com.example.myshoppanel.economy.ClientBalanceData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 通用确认弹窗。
 * 模态框，覆盖于当前界面之上，包含具体事务明细和透支风险提示。
 */
public class ConfirmDialog extends BaseStoreScreen {

    private final String transactionDetails;
    private final boolean hasOverdraftWarning;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmDialog(String transactionDetails, boolean hasOverdraftWarning,
                         Runnable onConfirm, Runnable onCancel) {
        super(Component.literal("Confirm Transaction"), 260, 200);
        this.transactionDetails = transactionDetails;
        this.hasOverdraftWarning = hasOverdraftWarning;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onCancel.run();
            if (minecraft != null) minecraft.setScreen(new PlayerMarketScreen());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void init() {
        super.init();

        int btnWidth = 100;
        int btnHeight = 24;
        int btnY = guiTop + imageHeight - 35;

        // 确认按钮（左侧）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.confirm"),
                btn -> {
                    try {
                        onConfirm.run();
                    } catch (Exception e) {
                        com.mojang.logging.LogUtils.getLogger().error("[MyShopPanel] 确认交易异常", e);
                    }
                    if (minecraft != null) minecraft.setScreen(new PlayerMarketScreen());
                }
        ).pos(guiLeft + 10, btnY).size(btnWidth, btnHeight).build());

        // 取消按钮（右侧）
        addRenderableWidget(Button.builder(
                Component.translatable("my_shop_panel.btn.cancel"),
                btn -> {
                    onCancel.run();
                    if (minecraft != null) minecraft.setScreen(new PlayerMarketScreen());
                }
        ).pos(guiLeft + imageWidth - btnWidth - 10, btnY).size(btnWidth, btnHeight).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // 标题
        drawCenteredString(graphics, Component.translatable("my_shop_panel.title.confirm_dialog").getString(), guiTop + 10, 0xFFFFD700);

        // 右上角余额
        String balanceText = Component.translatable("my_shop_panel.label.balance").getString() + ClientBalanceData.format();
        graphics.drawString(font, balanceText, guiLeft + imageWidth - font.width(balanceText) - 8, guiTop + 8, 0xFFFFFFFF);

        // 分隔线
        graphics.fill(guiLeft + 10, guiTop + 28, guiLeft + imageWidth - 10, guiTop + 29, 0xFF_4A4A6A);

        // 交易明细
        String[] lines = transactionDetails.split("\n");
        int textY = guiTop + 35;
        for (String line : lines) {
            if (line.isEmpty()) {
                textY += 5;
                continue;
            }
            graphics.drawString(font, line, guiLeft + 15, textY, 0xFFFFFFFF);
            textY += 12;
        }

        // 透支警告高亮
        if (hasOverdraftWarning) {
            int warnY = guiTop + imageHeight - 55;
            graphics.fill(guiLeft + 10, warnY - 2, guiLeft + imageWidth - 10, warnY + 14, 0x55_FF0000);
            drawCenteredString(graphics, Component.translatable("my_shop_panel.error.overdraft").getString(), warnY, 0xFFFF5555);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // 半透明遮罩覆盖整个屏幕
        graphics.fill(0, 0, this.width, this.height, 0xAA_000000);
        // 弹窗背景
        renderFrostedBackground(graphics);
    }
}
