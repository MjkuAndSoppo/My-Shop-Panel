package com.example.myshoppanel.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * 所有商店界面的基类。
 * 继承 Screen（非 ContainerScreen），完全脱离原版容器界面。
 * 提供统一的磨砂背景渲染。
 */
public abstract class BaseStoreScreen extends Screen {

    protected static final int DEFAULT_WIDTH = 256;
    protected static final int DEFAULT_HEIGHT = 200;
    protected static final int MAX_IMAGE_WIDTH = 600;
    protected static final int MAX_IMAGE_HEIGHT = 480;

    protected int guiLeft;
    protected int guiTop;
    protected int imageWidth;
    protected int imageHeight;

    protected BaseStoreScreen(Component title, int width, int height) {
        super(title);
        this.imageWidth = width;
        this.imageHeight = height;
    }

    protected BaseStoreScreen(Component title) {
        this(title, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        // 根据屏幕尺寸动态调整界面大小（高度自适应，宽度横向扩宽）
        this.imageHeight = Math.max(DEFAULT_HEIGHT, Math.min(this.height - 40, MAX_IMAGE_HEIGHT));
        this.imageWidth = Math.max(DEFAULT_WIDTH, Math.min(this.width - 40, getMaxImageWidth()));
        this.guiLeft = (this.width - imageWidth) / 2;
        this.guiTop = (this.height - imageHeight) / 2;
    }

    /** 子类可重写以限制最大宽度 */
    protected int getMaxImageWidth() {
        return MAX_IMAGE_WIDTH;
    }

    /**
     * 根据 imageHeight 动态计算可见行数
     */
    protected int computeRowsPerPage(int rowHeight, int topOffset, int bottomOffset) {
        int available = imageHeight - topOffset - bottomOffset;
        return Math.max(1, available / rowHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * 绘制半透明磨砂背景。
     */
    protected void renderFrostedBackground(GuiGraphics graphics) {
        renderFrostedBackground(graphics, guiLeft, guiTop, imageWidth, imageHeight);
    }

    protected void renderFrostedBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        // 主背景 - 深色半透明
        graphics.fill(x, y, x + w, y + h, 0xDD_1A1A2E);
        // 边框
        graphics.fill(x - 1, y - 1, x + w + 1, y, 0xFF_4A4A6A);           // 上
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF_4A4A6A);   // 下
        graphics.fill(x - 1, y, x, y + h, 0xFF_4A4A6A);                     // 左
        graphics.fill(x + w, y, x + w + 1, y + h, 0xFF_4A4A6A);             // 右
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // 覆盖默认背景，使用自定义磨砂背景
        renderFrostedBackground(graphics, 0, 0, this.width, this.height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 绘制居中文本
     */
    protected void drawCenteredString(GuiGraphics graphics, String text, int y, int color) {
        int x = guiLeft + imageWidth / 2 - font.width(text) / 2;
        graphics.drawString(font, text, x, y, color);
    }

    /**
     * 渲染物品tooltip（如果鼠标悬停在图标区域上）
     */
    protected void renderItemTooltipIfHovered(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack,
                                               int iconX, int iconY, int mouseX, int mouseY) {
        if (!stack.isEmpty() && mouseX >= iconX && mouseX < iconX + 16
                && mouseY >= iconY && mouseY < iconY + 16) {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
    }

    /** 页面块宽度，为 " 1000/1000 " 预留空间 */
    protected static final String PAGE_SAMPLE = "  1000/1000  ";

    /**
     * 绘制一个 Component，保留其自身颜色和格式（附魔色、自定义名斜体等）
     */
    protected void drawComponent(GuiGraphics graphics, net.minecraft.network.chat.Component component, int x, int y) {
        drawComponent(graphics, component, x, y, false);
    }

    protected void drawComponent(GuiGraphics graphics, net.minecraft.network.chat.Component component, int x, int y, boolean muted) {
        net.minecraft.network.chat.Style style = component.getStyle();
        int color;
        if (muted) {
            color = 0xFF555555;
        } else if (style.getColor() != null) {
            color = style.getColor().getValue();
        } else {
            color = 0xFFFFFFFF;
        }
        graphics.drawString(font, component.getVisualOrderText(), x, y, color);
    }
}