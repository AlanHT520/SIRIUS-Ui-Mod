package net.alan.gui.render.card;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.Action;
import net.alan.gui.data.source.CardDataSourceRegistry;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.render.ActionExecutor;
import net.alan.gui.util.NineSliceHelper;
import net.alan.gui.widget.BaseWidget;
import net.alan.gui.widget.Widget;
import net.alan.gui.widget.WidgetFactory;
import net.alan.gui.widget.ContainerWidget;
import net.alan.gui.widget.InputFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardInstance {
    private final String id;
    private final CardDefinition definition;
    private final Map<String, String> params;
    private final Widget rootWidget;
    private final Widget titleBarWidget;
    private final RenderContext renderContext;
    private final RenderContext titleBarRenderContext;
    private final long createTime;
    private final ActionExecutor executor;
    private Runnable onDismiss;
    private Action pendingAction;

    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int contentW;
    private int contentH;
    private int titleBarH;

    private boolean dragging = false;
    private double dragStartMouseX;
    private double dragStartMouseY;
    private int dragStartCardX;
    private int dragStartCardY;
    private Integer dragOffsetX = null;
    private Integer dragOffsetY = null;

    public CardInstance(String id, CardDefinition definition, Map<String, String> params,
                        ResourceManager resourceManager, ActionExecutor executor) {
        this.id = id;
        this.definition = definition;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
        this.createTime = System.currentTimeMillis();
        this.executor = executor;

        Map<String, String> mergedVars = new LinkedHashMap<>();
        if (definition.getVariables() != null) {
            mergedVars.putAll(definition.getVariables());
        }

        if (definition.getDataSource() != null) {
            Map<String, String> context = new LinkedHashMap<>();
            if (params != null) {
                context.putAll(params);
            }
            Map<String, String> dsData = CardDataSourceRegistry.load(definition.getDataSource(), context);
            mergedVars.putAll(dsData);
        }

        if (params != null) {
            mergedVars.putAll(params);
        }

        int padding = definition.getPadding();
        this.titleBarH = definition.hasTitleBar() ? definition.getTitleBar().getHeight() : 0;

        this.contentW = definition.getWidth() - padding * 2;
        this.contentH = definition.getMinHeight() - titleBarH - padding * 2;
        if (this.contentH < 0) this.contentH = 0;

        this.renderContext = new RenderContext(contentW, contentH, mergedVars);

        List<Widget> widgets = new ArrayList<>();
        if (definition.getElements() != null) {
            for (var elem : definition.getElements()) {
                if (elem.isJsonObject()) {
                    Widget w = WidgetFactory.create(elem.getAsJsonObject(), resourceManager, executor);
                    if (w != null) widgets.add(w);
                }
            }
        }

        this.rootWidget = new ContainerWidget("card_root",
                new LayoutProps("0", "0", "screen.width", "screen.height", true, true),
                new HashMap<>(),
                new HashMap<>(),
                widgets
        );

        if (definition.hasTitleBar() && definition.getTitleBar().getElements() != null) {
            TitleBarDef tbDef = definition.getTitleBar();
            this.titleBarRenderContext = new RenderContext(definition.getWidth(), tbDef.getHeight(), mergedVars);

            List<Widget> tbWidgets = new ArrayList<>();
            for (var elem : tbDef.getElements()) {
                if (elem.isJsonObject()) {
                    Widget w = WidgetFactory.create(elem.getAsJsonObject(), resourceManager, executor);
                    if (w != null) tbWidgets.add(w);
                }
            }

            this.titleBarWidget = new ContainerWidget("card_title_bar",
                    new LayoutProps("0", "0", "screen.width", "screen.height", true, true),
                    new HashMap<>(),
                    new HashMap<>(),
                    tbWidgets
            );
        } else {
            this.titleBarWidget = null;
            this.titleBarRenderContext = null;
        }

        if (params != null) {
            applyInputDefaults(params);
        }
    }

    private void applyInputDefaults(Map<String, String> params) {
        applyInputDefaultsRecursive(rootWidget, params);
        if (titleBarWidget != null) {
            applyInputDefaultsRecursive(titleBarWidget, params);
        }
    }

    private void applyInputDefaultsRecursive(Widget widget, Map<String, String> params) {
        if (widget instanceof InputFieldWidget inputField) {
            String fieldId = inputField.getId();
            if (fieldId != null && params.containsKey(fieldId)) {
                inputField.setValue(params.get(fieldId));
            }
        }
        if (widget instanceof ContainerWidget container) {
            for (Widget child : container.getChildren()) {
                applyInputDefaultsRecursive(child, params);
            }
        }
    }

    public String getId() { return id; }
    public CardDefinition getDefinition() { return definition; }
    public Map<String, String> getParams() { return params; }
    public long getCreateTime() { return createTime; }
    public Runnable getOnDismiss() { return onDismiss; }
    public void setOnDismiss(Runnable onDismiss) { this.onDismiss = onDismiss; }
    public Action getPendingAction() { return pendingAction; }
    public void setPendingAction(Action pendingAction) { this.pendingAction = pendingAction; }

    public boolean isModal() {
        return definition.isModal();
    }

    public boolean isExpired() {
        int dur = definition.getDurationMs();
        if (dur <= 0) return false;
        return System.currentTimeMillis() - createTime > dur;
    }

    public float getRemainingAlpha() {
        int dur = definition.getDurationMs();
        if (dur <= 0) return 1.0f;
        int fadeMs = definition.getFadeMs();
        if (fadeMs <= 0) fadeMs = 500;
        long elapsed = System.currentTimeMillis() - createTime;
        long fadeStart = dur - fadeMs;
        if (elapsed < fadeStart) return 1.0f;
        if (elapsed >= dur) return 0.0f;
        return 1.0f - (float)(elapsed - fadeStart) / fadeMs;
    }

    public String getInputValue(String fieldId) {
        String result = findInputValue(rootWidget, fieldId);
        if (result == null && titleBarWidget != null) {
            result = findInputValue(titleBarWidget, fieldId);
        }
        return result;
    }

    private String findInputValue(Widget widget, String fieldId) {
        if (widget instanceof InputFieldWidget inputField && fieldId.equals(inputField.getId())) {
            return inputField.getValue();
        }
        if (widget instanceof ContainerWidget container) {
            for (Widget child : container.getChildren()) {
                String result = findInputValue(child, fieldId);
                if (result != null) return result;
            }
        }
        return null;
    }

    public void layout(int screenW, int screenH) {
        CardDefinition def = definition;
        CardDefinition.CardType cardType = def.resolveType();
        int padding = def.getPadding();

        titleBarH = def.hasTitleBar() ? def.getTitleBar().getHeight() : 0;

        contentW = def.getWidth() - padding * 2;
        contentH = def.getMinHeight() - titleBarH - padding * 2;
        if (contentH < 0) contentH = 0;

        renderContext.setScreenSize(contentW, contentH);
        if (titleBarRenderContext != null) {
            titleBarRenderContext.setScreenSize(def.getWidth(), titleBarH);
        }

        this.cardW = def.getWidth();
        this.cardH = def.getMinHeight();

        if (dragOffsetX != null && dragOffsetY != null) {
            this.cardX = dragOffsetX;
            this.cardY = dragOffsetY;
            return;
        }

        int posX = def.getPosX();
        int posY = def.getPosY();

        if (cardType == CardDefinition.CardType.TOAST) {
            this.cardX = (screenW - cardW) / 2;
            this.cardY = 10;
        } else if (cardType == CardDefinition.CardType.TOOLTIP) {
            this.cardX = posX > 0 ? posX : screenW / 2;
            this.cardY = posY > 0 ? posY : screenH / 2;
        } else {
            this.cardX = posX >= 0 ? posX : (screenW - cardW) / 2;
            this.cardY = posY >= 0 ? posY : (screenH - cardH) / 2;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        CardDefinition def = definition;
        CardDefinition.CardType cardType = def.resolveType();
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        layout(sw, sh);

        switch (cardType) {
            case TOAST -> renderToast(graphics, sw);
            case LOADING -> renderLoading(graphics, sw, sh);
            case TOOLTIP -> renderTooltip(graphics, sw, sh, mouseX, mouseY);
            default -> renderDialog(graphics, mouseX, mouseY, delta, sw, sh);
        }
    }

    private void renderBackground(GuiGraphics graphics, int x, int y, int w, int h, BackgroundDef bg) {
        if (bg == null) return;
        if (bg.hasTexture()) {
            ResourceLocation tex = BaseWidget.parseTexturePath(bg.getTexture());
            if (tex == null) return;
            String mode = bg.getTextureMode();
            if ("tile".equals(mode)) {
                int tileW = w;
                int tileH = h;
                for (int tx = x; tx < x + w; tx += tileW) {
                    int drawW = Math.min(tileW, x + w - tx);
                    for (int ty = y; ty < y + h; ty += tileH) {
                        int drawH = Math.min(tileH, y + h - ty);
                        graphics.blit(tex, tx, ty, 0, 0, drawW, drawH, drawW, drawH);
                    }
                }
            } else {
                NineSliceHelper.NineSliceInfo nineSlice = NineSliceHelper.loadNineSlice(tex);
                if (nineSlice != null) {
                    NineSliceHelper.blitNineSliced(graphics, tex, x, y, w, h, nineSlice);
                } else {
                    graphics.blit(tex, x, y, 0, 0, w, h, w, h);
                }
            }
        }
        if (bg.hasColor()) {
            graphics.fill(x, y, x + w, y + h, definition.parseColor(bg.getColor()));
        }
    }

    private void renderDialog(GuiGraphics graphics, int mouseX, int mouseY, float delta, int sw, int sh) {
        CardDefinition def = definition;

        if (def.hasOverlay()) {
            graphics.fill(0, 0, sw, sh, def.parseColor(def.getOverlay()));
        }

        renderShadow(graphics, cardX, cardY, cardW, cardH, def.getShadowOffset(), def.getShadowAlpha());
        renderBackground(graphics, cardX, cardY, cardW, cardH, def.getBackground());
        renderHighlight(graphics, cardX, cardY, cardW, cardH);
        if (def.hasBorder()) {
            renderBorder(graphics, cardX, cardY, cardW, cardH, def.parseColor(def.getBorder()), def.getBorderWidth());
        }

        int contentOffsetY = 0;

        if (def.hasTitleBar() && titleBarWidget != null) {
            TitleBarDef tb = def.getTitleBar();
            renderBackground(graphics, cardX, cardY, cardW, titleBarH, tb.getBackground());

            graphics.pose().pushPose();
            graphics.pose().translate(cardX, cardY, 1);
            titleBarWidget.render(graphics, 0, 0, cardW, titleBarH, titleBarRenderContext,
                    mouseX - cardX, mouseY - cardY, delta);
            titleBarWidget.mouseMoved(
                    mouseX - cardX, mouseY - cardY,
                    titleBarRenderContext, 0, 0, cardW, titleBarH);
            graphics.pose().popPose();

            contentOffsetY = titleBarH;
        }

        int padding = def.getPadding();
        graphics.pose().pushPose();
        graphics.pose().translate(cardX + padding, cardY + contentOffsetY + padding, 1);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, contentW, contentH, renderContext,
                    mouseX - cardX - padding,
                    mouseY - cardY - contentOffsetY - padding,
                    delta);
            rootWidget.mouseMoved(
                    mouseX - cardX - padding,
                    mouseY - cardY - contentOffsetY - padding,
                    renderContext, 0, 0, contentW, contentH);
        }

        graphics.pose().popPose();
    }

    private void renderToast(GuiGraphics graphics, int sw) {
        CardDefinition def = definition;
        float alpha = getRemainingAlpha();

        renderShadow(graphics, cardX, cardY, cardW, cardH, Math.max(1, def.getShadowOffset() - 1), def.getShadowAlpha());

        if (def.getBackground().hasTexture()) {
            ResourceLocation tex = new ResourceLocation(def.getBackground().getTexture());
            graphics.blit(tex, cardX, cardY, 0, 0, cardW, cardH, cardW, cardH);
        }
        if (def.getBackground().hasColor()) {
            int bgColor = applyAlpha(def.parseColor(def.getBackground().getColor()), alpha);
            graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);
        }
        graphics.fill(cardX, cardY, cardX + cardW, cardY + 1, applyAlpha(0x40FFFFFF, alpha));
        if (def.hasBorder()) {
            renderBorder(graphics, cardX, cardY, cardW, cardH, applyAlpha(def.parseColor(def.getBorder()), alpha), def.getBorderWidth());
        }

        int padding = def.getPadding();
        graphics.pose().pushPose();
        graphics.pose().translate(cardX + padding, cardY + padding, 0);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, contentW, contentH, renderContext, 0, 0, 0);
        }

        graphics.pose().popPose();
    }

    private void renderLoading(GuiGraphics graphics, int sw, int sh) {
        CardDefinition def = definition;

        if (def.hasOverlay()) {
            graphics.fill(0, 0, sw, sh, def.parseColor(def.getOverlay()));
        }

        renderShadow(graphics, cardX, cardY, cardW, cardH, Math.max(1, def.getShadowOffset() - 1), Math.min(def.getShadowAlpha(), 0x40));
        renderBackground(graphics, cardX, cardY, cardW, cardH, def.getBackground());
        graphics.fill(cardX, cardY, cardX + cardW, cardY + 1, 0x40FFFFFF);
        if (def.hasBorder()) {
            renderBorder(graphics, cardX, cardY, cardW, cardH, def.parseColor(def.getBorder()), def.getBorderWidth());
        }

        int padding = def.getPadding();
        graphics.pose().pushPose();
        graphics.pose().translate(cardX + padding, cardY + padding, 0);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, contentW, contentH, renderContext, 0, 0, 0);
        }

        graphics.pose().popPose();
    }

    private void renderTooltip(GuiGraphics graphics, int sw, int sh, int mouseX, int mouseY) {
        CardDefinition def = definition;

        int tx = cardX;
        int ty = cardY;

        if (def.getPosX() <= 0) tx = mouseX + 12;
        if (def.getPosY() <= 0) ty = mouseY - cardH;

        if (tx + cardW > sw) tx = sw - cardW - 4;
        if (ty < 4) ty = 4;
        if (ty + cardH > sh) ty = sh - cardH - 4;

        this.cardX = tx;
        this.cardY = ty;

        renderBackground(graphics, tx, ty, cardW, cardH, def.getBackground());
        graphics.fill(tx, ty, tx + cardW, ty + 1, 0x40FFFFFF);
        if (def.hasBorder()) {
            renderBorder(graphics, tx, ty, cardW, cardH, def.parseColor(def.getBorder()), def.getBorderWidth());
        }

        int padding = def.getPadding();
        graphics.pose().pushPose();
        graphics.pose().translate(tx + padding, ty + padding, 0);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, contentW, contentH, renderContext, 0, 0, 0);
        }

        graphics.pose().popPose();
    }

    private void renderShadow(GuiGraphics graphics, int x, int y, int w, int h, int shadowSize, int baseAlpha) {
        for (int i = shadowSize; i > 0; i--) {
            int alpha = (int)(((shadowSize - i + 1) / (float)(shadowSize + 1)) * baseAlpha);
            int c = (alpha << 24) | 0x000000;
            graphics.fill(x + i, y + i + 2, x + w + i, y + h + i + 2, c);
        }
    }

    private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color, int borderWidth) {
        if (borderWidth <= 1) {
            graphics.renderOutline(x, y, w, h, color);
            return;
        }
        for (int i = 0; i < borderWidth; i++) {
            graphics.renderOutline(x + i, y + i, w - i * 2, h - i * 2, color);
        }
    }

    private void renderHighlight(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + 1, 0x30FFFFFF);
        graphics.fill(x, y + 1, x + w, y + 2, 0x10FFFFFF);
        graphics.fill(x, y, x + 1, y + h, 0x20FFFFFF);
        graphics.fill(x + w - 1, y, x + w, y + h, 0x40000000);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        CardDefinition def = definition;
        CardDefinition.CardType cardType = def.resolveType();
        if (cardType != CardDefinition.CardType.DIALOG) return false;

        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        layout(sw, sh);

        if (def.hasTitleBar() && titleBarWidget != null) {
            TitleBarDef tb = def.getTitleBar();
            if (my >= cardY && my < cardY + titleBarH) {
                double tbRelX = mx - cardX;
                double tbRelY = my - cardY;
                if (titleBarWidget.mouseClicked(tbRelX, tbRelY, button,
                        titleBarRenderContext, 0, 0, cardW, titleBarH)) {
                    return true;
                }
                if (tb.isDraggable()) {
                    dragging = true;
                    dragStartMouseX = mx;
                    dragStartMouseY = my;
                    dragStartCardX = cardX;
                    dragStartCardY = cardY;
                    return true;
                }
            }
        }

        int padding = def.getPadding();
        int contentOffsetY = titleBarH;

        if (rootWidget != null) {
            return rootWidget.mouseClicked(
                    mx - cardX - padding,
                    my - cardY - contentOffsetY - padding,
                    button, renderContext, 0, 0, contentW, contentH);
        }

        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }

        CardDefinition def = definition;
        int padding = def.getPadding();
        int contentOffsetY = titleBarH;

        boolean handled = false;

        if (def.hasTitleBar() && titleBarWidget != null) {
            handled = titleBarWidget.mouseReleased(
                    mx - cardX, my - cardY,
                    button, titleBarRenderContext, 0, 0, cardW, titleBarH);
        }

        if (rootWidget != null) {
            boolean contentHandled = rootWidget.mouseReleased(
                    mx - cardX - padding,
                    my - cardY - contentOffsetY - padding,
                    button, renderContext, 0, 0, contentW, contentH);
            handled = handled || contentHandled;
        }

        return handled;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (dragging) {
            int dx = (int)(mx - dragStartMouseX);
            int dy = (int)(my - dragStartMouseY);
            int newX = dragStartCardX + dx;
            int newY = dragStartCardY + dy;

            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            newX = Math.max(0, Math.min(sw - cardW, newX));
            newY = Math.max(0, Math.min(sh - cardH, newY));

            dragOffsetX = newX;
            dragOffsetY = newY;
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rootWidget != null) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                cycleInputFocus();
                return true;
            }
            return rootWidget.keyPressed(keyCode, scanCode, modifiers,
                    renderContext, 0, 0, contentW, contentH);
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (rootWidget != null) {
            return rootWidget.charTyped(codePoint, modifiers,
                    renderContext, 0, 0, contentW, contentH);
        }
        return false;
    }

    private void cycleInputFocus() {
        List<InputFieldWidget> inputs = new ArrayList<>();
        collectInputFields(rootWidget, inputs);
        if (inputs.isEmpty()) return;

        int focusedIndex = -1;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).isWidgetFocused()) {
                focusedIndex = i;
                break;
            }
        }

        for (InputFieldWidget input : inputs) {
            input.setFocused(false);
        }

        int nextIndex = (focusedIndex + 1) % inputs.size();
        inputs.get(nextIndex).setFocused(true);
    }

    private void collectInputFields(Widget widget, List<InputFieldWidget> result) {
        if (widget instanceof InputFieldWidget inputField) {
            result.add(inputField);
        }
        for (Widget child : widget.getChildren()) {
            collectInputFields(child, result);
        }
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}