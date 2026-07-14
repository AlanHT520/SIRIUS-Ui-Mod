package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.EditBoxProps;
import net.alan.gui.data.props.LayoutProps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class StringEditBoxWidget extends BaseWidget {
    private final EditBoxProps props;
    private final EditBox editBox;
    private boolean isFocused;

    public StringEditBoxWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member, EditBoxProps props) {
        super(id, layout, variables, member);
        this.props = props;

        Minecraft minecraft = Minecraft.getInstance();
        Component hint = props.hint() != null
                ? Component.translatable(props.hint())
                : Component.empty();

        int color;
        try {
            color = Integer.decode(props.textColor());
        } catch (NumberFormatException e) {
            color = 0xE0E0E0;
        }

        this.editBox = new EditBox(minecraft.font, 0, 0, 100, 20, Component.empty());
        this.editBox.setMaxLength(props.maxLength());
        this.editBox.setBordered(props.bordered());
        this.editBox.setHint(hint);
        this.editBox.setValue(props.initialValue() != null ? props.initialValue() : "");
        this.editBox.setTextColor(color);

        this.editBox.setResponder(newValue -> {
            this.member.put("edit_value", newValue);
        });
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        editBox.setRectangle(dim.w, dim.h, screenX, screenY);
        editBox.render(graphics, mouseX, mouseY, delta);
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

        editBox.setRectangle(dim.w, dim.h, screenX, screenY);
        return editBox.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        return editBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers,
                             RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        return editBox.charTyped(codePoint, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        this.isFocused = focused;
        this.editBox.setFocused(focused);
    }

    @Override
    public boolean isWidgetFocused() {
        return isFocused;
    }
}