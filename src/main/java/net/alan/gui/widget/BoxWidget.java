package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.style.TextureSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * BoxWidget - 视图切换容器
 * 
 * 功能：
 * - 作为视图切换器，根据 currentId 显示不同的子元素
 * - 支持多个 id 对应不同的 Widget（List、Button 等）
 * - 提供 switchTo() 方法供外部（如 Content 内的 button）切换视图
 * - Content 内的 button 可以执行原 button 的任何功能
 */
public class BoxWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxWidget.class);

    private final Map<String, Widget> elements;
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
        if (!elements.containsKey(defaultId)) {
            LOGGER.warn("Box {} defaultId '{}' not found, using first", id, defaultId);
            this.currentId = elements.isEmpty() ? null : elements.keySet().iterator().next();
        }
    }

    /**
     * 切换到指定 id 的视图
     * @param id 目标视图的 id
     * @return 是否切换成功
     */
    public boolean switchTo(String id) {
        if (elements.containsKey(id)) {
            this.currentId = id;
            return true;
        }
        LOGGER.warn("Box {} attempted to switch to unknown id '{}'", this.id, id);
        return false;
    }

    /**
     * 获取当前视图的 id
     */
    public String getCurrentId() {
        return currentId;
    }

    /**
     * 获取所有可用的视图 id
     */
    public Set<String> getAvailableIds() {
        return elements.keySet();
    }

    private Widget getCurrentElement() {
        if (currentId == null) return null;
        return elements.get(currentId);
    }

    @Override
    public List<Widget> getChildren() {
        Widget current = getCurrentElement();
        return current != null ? List.of(current) : Collections.emptyList();
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

        // 渲染背景色
        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            graphics.fill(boxX, boxY, boxX + dim.w, boxY + dim.h, parseColor(backgroundColor));
        }

        // 渲染框架纹理
        if (frameTexture != null && frameTexture.getNormal() != null) {
            var texId = ResourceLocation.tryParse(frameTexture.getNormal());
            if (texId != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(texId, boxX, boxY, dim.w, dim.h);
            }
        }

        // 渲染当前元素，铺满内容区域
        Widget current = getCurrentElement();
        if (current != null) {
            // 注入 _box_current_id，供 button_content 判断 selected 状态
            RenderContext boxCtx = mergedCtx.withVar("_box_current_id", currentId);
            graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
            current.render(graphics, contentX, contentY, contentW, contentH,
                    boxCtx, mouseX, mouseY, delta);
            graphics.disableScissor();
        }

        // 渲染边框（最后渲染，放在最上层，包在实际空间外部）
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
        Widget current = getCurrentElement();
        if (current != null) {
            return current.mouseClicked(mouseX, mouseY, button, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        Widget current = getCurrentElement();
        if (current != null) {
            return current.mouseReleased(mouseX, mouseY, button, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        Widget current = getCurrentElement();
        if (current != null) {
            return current.mouseScrolled(mouseX, mouseY, scrollX, scrollY, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        Widget current = getCurrentElement();
        if (current != null) {
            return current.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        Widget current = getCurrentElement();
        if (current != null) {
            return current.keyPressed(keyCode, scanCode, modifiers, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers, RenderContext context,
                             int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        Widget current = getCurrentElement();
        if (current != null) {
            return current.charTyped(codePoint, modifiers, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY, RenderContext context,
                           int x, int y, int width, int height) {
        if (!layout.visible()) return;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        Widget current = getCurrentElement();
        if (current != null) {
            current.mouseMoved(mouseX, mouseY, mergedCtx,
                    x + dim.x + paddingLeft, y + dim.y + paddingTop,
                    dim.w - paddingLeft - paddingRight, dim.h - paddingTop - paddingBottom);
        }
    }

    private static int parseColor(String str) {
        String hex = str.startsWith("0x") || str.startsWith("0X") ? str.substring(2) : str;
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        return (int) Long.parseLong(hex, 16);
    }
}