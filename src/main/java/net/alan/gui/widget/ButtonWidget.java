package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.Action;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.StyleProps;
import net.alan.gui.data.widget.TextProps;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ButtonWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonWidget.class);

    private static ButtonWidget currentIngButton = null;

    private final StyleProps style;
    private final Action action;
    private final ActionExecutor actionExecutor;
    private final List<Widget> children;
    private boolean isHovered = false;
    private boolean isIng = false;
    private float ingTimer = 0.0f;
    private final int ingDuration;
    private final Map<String, Map<String, String>> stateVariables;

    public ButtonWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                        StyleProps style, TextProps textProps, Action action,
                        ActionExecutor actionExecutor, List<Widget> children,
                        int ingDuration,
                        Map<String, Map<String, String>> stateVariables) {
        super(id, layout, variables, member);
        this.style = style;
        this.action = action;
        this.actionExecutor = actionExecutor;
        this.children = new ArrayList<>();
        this.ingDuration = ingDuration;
        this.stateVariables = stateVariables;
        // ??textProps 转为 TextWidget child（快捷方式，兼容旧写法）
        // 默认居中，可通过 text 块内??"position" 覆盖
        if (textProps != null) {
            String xPos = textProps.offsetX() != null && !textProps.offsetX().equals("0")
                    ? textProps.offsetX() : "parent.width / 2 - this.width / 2";
            String yPos = textProps.offsetY() != null && !textProps.offsetY().equals("0")
                    ? textProps.offsetY() : "parent.height / 2 - this.height / 2";
            this.children.add(new TextWidget(id + "_text",
                    new LayoutProps(xPos, yPos, "auto", "auto", true, true),
                    null, null, textProps));
        }
        if (children != null) {
            this.children.addAll(children);
        }
    }

    @Override
    public List<Widget> getChildren() { return children; }

    public Action getAction() { return action; }

    public void cancelIng() {
        if (isIng) {
            isIng = false;
            ingTimer = 0.0f;
            if (currentIngButton == this) {
                currentIngButton = null;
            }
        }
    }

    private Map<String, String> getStateOverrides() {
        if (stateVariables == null) return null;
        if (!layout.enabled()) return stateVariables.get("disabled");
        if (isIng) return stateVariables.get("ing");
        if (isHovered) return stateVariables.get("highlighted");
        return stateVariables.get("normal");
    }

    private String getCurrentState() {
        if (!layout.enabled()) return "disabled";
        if (isIng) return "ing";
        if (isHovered) return "highlighted";
        return "normal";
    }
    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);

        // 根据当前状态应??state_variables 覆盖
        Map<String, String> stateOverrides = getStateOverrides();
        if (stateOverrides != null) {
            mergedCtx = mergedCtx.withVars(stateOverrides);
        }

        // 注入 _button_state 供子元素??state_v 使用
        mergedCtx = mergedCtx.withVar("_button_state", getCurrentState());

        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        isHovered = mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h;

        if (style.texture() != null) {
            String texPath;
            if (!layout.enabled()) texPath = evalStringExpr(vars, style.texture().getDisabled());
            else if (isIng && style.texture().getIng() != null) texPath = evalStringExpr(vars, style.texture().getIng());
            else if (isHovered) texPath = evalStringExpr(vars, style.texture().getHighlighted());
            else texPath = evalStringExpr(vars, style.texture().getNormal());
            if (texPath != null && !texPath.isEmpty()) {
                var texId = ResourceLocation.tryParse(texPath);
                if (texId != null) {
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.blitSprite(texId, screenX, screenY, dim.w, dim.h);
                }
            }
        } else {
            String bgStr = style.backgroundColor();
            int bgColor;
            int borderColor;
            if (bgStr != null && !bgStr.isEmpty()) {
                bgColor = parseColor(bgStr);
                borderColor = isHovered ? lightenColor(bgColor, 0.3f) : bgColor;
            } else {
                bgColor = layout.enabled() ? (isHovered ? 0xFF555555 : 0xFF3A3A3A) : 0xFF2A2A2A;
                borderColor = isHovered ? 0xFF888888 : 0xFF505050;
            }
            int bgAlpha = (bgColor >> 24) & 0xFF;
            if (bgAlpha > 0) {
                graphics.fill(screenX, screenY, screenX + dim.w, screenY + dim.h, bgColor);
                graphics.fill(screenX, screenY, screenX + dim.w, screenY + 1, 0x30FFFFFF);
                graphics.fill(screenX, screenY + dim.h - 1, screenX + dim.w, screenY + dim.h, 0x40000000);
                graphics.renderOutline(screenX, screenY, dim.w, dim.h, borderColor);
            }
            if (isHovered) {
                graphics.fill(screenX, screenY, screenX + dim.w, screenY + dim.h, 0x18FFFFFF);
            }
        }

        // ing 状态计时：持续时间结束后触??action
        if (isIng) {
            ingTimer += Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
            if (ingTimer >= ingDuration) {
                isIng = false;
                ingTimer = 0.0f;
                if (currentIngButton == this) {
                    currentIngButton = null;
                }
                if (action != null && actionExecutor != null) {
                    actionExecutor.execute(resolveAction(action, vars));
                }
            }
        }

        // 渲染子元素（文本通过 text 快捷方式转为 TextWidget child??
        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) {
            return false;
        }

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        // 先让子元素响应（反向遍历，上层优先）
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }

        if (mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h) {
            LOGGER.info("Button {} clicked!", id);
            playClickSound();
            if (ingDuration > 0) {
                if (currentIngButton != null && currentIngButton != this) {
                    currentIngButton.cancelIng();
                }
                isIng = true;
                ingTimer = 0.0f;
                currentIngButton = this;
            } else if (action != null && actionExecutor != null) {
                actionExecutor.execute(resolveAction(action, vars));
            } else {
                LOGGER.warn("Button {} has no action or executor", id);
            }
            return true;
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
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        boolean childConsumed = false;
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                childConsumed = true;
            }
        }
        return childConsumed;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY,
                           RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        for (Widget child : children) {
            child.mouseMoved(mouseX, mouseY, mergedCtx, screenX, screenY, dim.w, dim.h);
        }
    }

    private static int parseColor(String hex) {
        if (hex == null) return 0xFFFFFFFF;
        hex = hex.replace("0x", "").replace("0X", "");
        return (int) Long.parseLong(hex, 16);
    }

    private static int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * (1 + factor));
        int g = (int)(((color >> 8) & 0xFF) * (1 + factor));
        int b = (int)((color & 0xFF) * (1 + factor));
        r = Math.min(r, 255);
        g = Math.min(g, 255);
        b = Math.min(b, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}