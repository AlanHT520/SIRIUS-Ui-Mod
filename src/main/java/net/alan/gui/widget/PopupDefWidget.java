package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;

public class PopupDefWidget extends BaseWidget {

    private final net.alan.gui.render.popup.PopupDefinition definition;

    public PopupDefWidget(String id, net.alan.gui.render.popup.PopupDefinition definition) {
        super(id, new LayoutProps(), Map.of(), Map.of());
        this.definition = definition;
    }

    public net.alan.gui.render.popup.PopupDefinition getDefinition() {
        return definition;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
    }
}