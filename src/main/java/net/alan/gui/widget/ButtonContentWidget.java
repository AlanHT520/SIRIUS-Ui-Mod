package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.StyleProps;
import net.alan.gui.data.props.TextProps;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ButtonContentWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(ButtonContentWidget.class);

    private final StyleProps style;
    private final ActionExecutor actionExecutor;
    private final List<Widget> children;
    private boolean isHovered = false;
    private final String boxId;
    private final String targetId;
    private final Map<String, Map<String, String>> stateVariables;

    public ButtonContentWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                               StyleProps style, TextProps textProps,
                               ActionExecutor actionExecutor, List<Widget> children,
                               String boxId, String targetId,
                               Map<String, Map<String, String>> stateVariables) {
        super(id, layout, variables, member);
        this.style = style;
        this.actionExecutor = actionExecutor;
        this.children = new ArrayList<>();
        this.boxId = boxId;
        this.targetId = targetId != null ? targetId : id;
        this.stateVariables = stateVariables;

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

    public String getBoxId() { return boxId; }
    public String getTargetId() { return targetId; }

    private boolean isSelected(RenderContext context, Map<String, Integer> vars) {
        String resolvedBoxId = evalStringExpr(vars, boxId);
        String resolvedTargetId = evalStringExpr(vars, targetId);
        if (actionExecutor != null && resolvedBoxId != null) {
            BoxWidget box = actionExecutor.getBox(resolvedBoxId);
            if (box != null) {
                String currentId = box.getCurrentId();
                return currentId != null && currentId.equals(resolvedTargetId);
            }
        }
        return false;
    }

    private String getCurrentState(RenderContext context, Map<String, Integer> vars) {
        if (!layout.enabled()) return "disabled";
        if (isHovered) return "highlighted";
        if (isSelected(context, vars)) return "selected";
        return "normal";
    }

    private Map<String, String> getStateOverrides(RenderContext context, Map<String, Integer> vars) {
        if (stateVariables == null) return null;
        if (!layout.enabled()) return stateVariables.get("disabled");
        if (isHovered) return stateVariables.get("highlighted");
        if (isSelected(context, vars)) return stateVariables.get("selected");
        return stateVariables.get("normal");
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);

        Map<String, String> stateOverrides = getStateOverrides(mergedCtx, vars);
        if (stateOverrides != null) {
            mergedCtx = mergedCtx.withVars(stateOverrides);
        }

        mergedCtx = mergedCtx.withVar("_button_state", getCurrentState(mergedCtx, vars));
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        isHovered = mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h;

        if (style.texture() != null) {
            String texPath = null;
            if (!layout.enabled()) texPath = evalStringExpr(vars, style.texture().getDisabled());
            else if (isHovered) texPath = evalStringExpr(vars, style.texture().getHighlighted());
            else if (isSelected(mergedCtx, vars)) texPath = evalStringExpr(vars, style.texture().getSelected());
            else texPath = evalStringExpr(vars, style.texture().getNormal());

            if (texPath != null && !texPath.isEmpty()) {
                var texId = ResourceLocation.tryParse(texPath);
                if (texId != null) {
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.blitSprite(texId, screenX, screenY, dim.w, dim.h);
                }
            }
        }

        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
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
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }

        if (mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h) {
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
            String resolvedBoxId = evalStringExpr(vars, boxId);
            String resolvedTargetId = evalStringExpr(vars, targetId);
            if (actionExecutor != null && resolvedBoxId != null) {
                BoxWidget box = actionExecutor.getBox(resolvedBoxId);
                if (box != null) {
                    box.switchTo(resolvedTargetId);
                    LOGGER.info("ButtonContent {} switched box '{}' to '{}'", id, resolvedBoxId, resolvedTargetId);
                } else {
                    LOGGER.warn("ButtonContent {} cannot find box '{}'", id, resolvedBoxId);
                }
            } else {
                LOGGER.warn("ButtonContent {} has no boxId or executor", id);
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

        for (Widget child : children) {
            child.mouseReleased(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h);
        }
        return false;
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
}