package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContainerWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerWidget.class);
    private final List<Widget> children;

    public ContainerWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member, List<Widget> children) {
        super(id, layout, variables, member);
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
    }

    @Override
    public List<Widget> getChildren() { return children; }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (Widget child : children) {
            child.render(graphics, containerX, containerY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
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
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                // 获得焦点的子组件设为 focused，其他失焦
                for (Widget w : children) {
                    w.setFocused(w == child);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        // 必须通知所有子组件，不能因为某个返回 true 就提前终止
        // 否则子组件的 isDragging 等状态无法重置
        boolean consumed = false;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseReleased(mouseX, mouseY, button, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                consumed = true;
            }
        }
        return consumed;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                  RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseScrolled(mouseX, mouseY, scrollX, scrollY, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                return true;
            }
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
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY,
                           RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible()) return;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (Widget child : children) {
            child.mouseMoved(mouseX, mouseY, mergedCtx, containerX, containerY, dim.w, dim.h);
        }
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers,
                             RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int containerX = x + dim.x;
        int containerY = y + dim.y;

        for (Widget child : children) {
            if (child.charTyped(codePoint, modifiers, mergedCtx, containerX, containerY, dim.w, dim.h)) {
                return true;
            }
        }
        return false;
    }
}