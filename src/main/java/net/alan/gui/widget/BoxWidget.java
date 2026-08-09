package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.TextureSet;
import net.alan.gui.util.NineSliceHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BoxWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxWidget.class);

    private final Map<String, Widget> elements;
    private final String defaultId;
    private String currentId;
    private final String backgroundColor;
    private final String borderColor;
    private final TextureSet frameTexture;
    private final int paddingTop, paddingBottom, paddingLeft, paddingRight;

    public BoxWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                     Map<String, Widget> elements, String defaultId,
                     String backgroundColor, String borderColor, TextureSet frameTexture,
                     int paddingTop, int paddingBottom, int paddingLeft, int paddingRight) {
        super(id, layout, variables, member);
        this.elements = elements;
        this.defaultId = defaultId;
        this.currentId = defaultId;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.frameTexture = frameTexture;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;

        if (elements.isEmpty()) {
            LOGGER.warn("Box {} created with no elements", id);
        }
    }

    public boolean switchTo(String elementId) {
        if (elements.containsKey(elementId)) {
            this.currentId = elementId;
            return true;
        }
        LOGGER.warn("Box {} has no element '{}'", id, elementId);
        return false;
    }

    public String getCurrentId() {
        return currentId;
    }

    public List<String> getAvailableIds() {
        return new ArrayList<>(elements.keySet());
    }

    @Override
    public List<Widget> getChildren() {
        Widget current = getCurrentElement();
        return current != null ? List.of(current) : Collections.emptyList();
    }

    private Widget getCurrentElement() {
        if (currentId == null || !elements.containsKey(currentId)) {
            if (!elements.isEmpty()) {
                currentId = elements.keySet().iterator().next();
            }
        }
        return currentId != null ? elements.get(currentId) : null;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int boxX = x + dim.x;
        int boxY = y + dim.y;

        int contentX = boxX + paddingLeft;
        int contentY = boxY + paddingTop;
        int contentW = dim.w - paddingLeft - paddingRight;
        int contentH = dim.h - paddingTop - paddingBottom;

        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            graphics.fill(boxX, boxY, boxX + dim.w, boxY + dim.h, parseColor(backgroundColor));
        }

        if (frameTexture != null && frameTexture.getNormal() != null) {
            var texId = parseTexturePath(frameTexture.getNormal());
            if (texId != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                NineSliceHelper.NineSliceInfo nineSlice = NineSliceHelper.loadNineSlice(texId);
                if (nineSlice != null) {
                    NineSliceHelper.blitNineSliced(graphics, texId, boxX, boxY, dim.w, dim.h, nineSlice);
                } else {
                    graphics.blit(texId, boxX, boxY, 0, 0, dim.w, dim.h, dim.w, dim.h);
                }
            }
        }

        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);

        Widget current = getCurrentElement();
        if (current != null) {
            current.render(graphics, contentX, contentY, contentW, contentH,
                    mergedCtx, mouseX, mouseY, delta);
        }

        graphics.disableScissor();

        if (borderColor != null && !borderColor.isEmpty()) {
            int bc = parseColor(borderColor);
            graphics.fill(boxX - 1, boxY - 1, boxX + dim.w + 1, boxY, bc);
            graphics.fill(boxX - 1, boxY + dim.h, boxX + dim.w + 1, boxY + dim.h + 1, bc);
            graphics.fill(boxX - 1, boxY - 1, boxX, boxY + dim.h + 1, bc);
            graphics.fill(boxX + dim.w, boxY - 1, boxX + dim.w + 1, boxY + dim.h + 1, bc);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.mouseClicked(mouseX, mouseY, button, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.mouseReleased(mouseX, mouseY, button, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.mouseScrolled(mouseX, mouseY, scrollX, scrollY, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.keyPressed(keyCode, scanCode, modifiers, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers, RenderContext context,
                             int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        return current != null && current.charTyped(codePoint, modifiers, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY, RenderContext context,
                           int x, int y, int width, int height) {
        if (!layout.visible()) return;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int[] cb = contentBounds(mergedCtx, x, y, width, height);
        Widget current = getCurrentElement();
        if (current != null) {
            current.mouseMoved(mouseX, mouseY, mergedCtx, cb[0], cb[1], cb[2], cb[3]);
        }
    }

    private int[] contentBounds(RenderContext context, int x, int y, int width, int height) {
        WidgetDimension dim = computeLayout(context, width, height);
        return new int[] {
            x + dim.x + paddingLeft,
            y + dim.y + paddingTop,
            dim.w - paddingLeft - paddingRight,
            dim.h - paddingTop - paddingBottom
        };
    }

    private static int parseColor(String str) {
        String hex = str.startsWith("0x") || str.startsWith("0X") ? str.substring(2) : str;
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        return (int) Long.parseLong(hex, 16);
    }
}