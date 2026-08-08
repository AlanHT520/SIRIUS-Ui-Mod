package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.render.card.CardDefinition;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;

public class CardDefWidget extends BaseWidget {

    private final CardDefinition definition;

    public CardDefWidget(String id, CardDefinition definition) {
        super(id, new LayoutProps(), Map.of(), Map.of());
        this.definition = definition;
    }

    public CardDefinition getDefinition() {
        return definition;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
    }
}