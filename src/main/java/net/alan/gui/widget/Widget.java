package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public interface Widget {
    void render(GuiGraphics graphics, int x, int y, int width, int height,
                RenderContext context, int mouseX, int mouseY, float delta);

    default boolean mouseClicked(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button,
                                  RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                   RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                  RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers,
                               RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default void mouseMoved(double mouseX, double mouseY,
                            RenderContext context, int x, int y, int width, int height) {}

    default void setFocused(boolean focused) {}

    default boolean isWidgetFocused() { return false; }

    default List<Widget> getChildren() { return List.of(); }

    default String getId() { return null; }

    default boolean charTyped(char codePoint, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    default void setStateV(java.util.Map<String, java.util.Map<String, java.util.Map<String, String>>> stateV) {}

    default void setStateVProps(java.util.Map<String, java.util.Map<String, String>> stateVProps) {}



    default BaseWidget.WidgetDimension computeLayout(RenderContext ctx, int parentW, int parentH) {
        return new BaseWidget.WidgetDimension(0, 0, 0, 0);
    }

    default void renderOverlay(GuiGraphics graphics, int x, int y, int width, int height,
                               RenderContext context, int mouseX, int mouseY, float delta) {}

    default boolean hasOverlay() { return false; }
}