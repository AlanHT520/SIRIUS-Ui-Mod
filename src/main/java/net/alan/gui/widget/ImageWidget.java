package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.StyleProps;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class ImageWidget extends BaseWidget {
    private final StyleProps style;

    public ImageWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member, StyleProps style) {
        super(id, layout, variables, member);
        this.style = style;
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

        if (style.texture() == null) return;
        String normal = evalStringExpr(vars, style.texture().getNormal());
        if (normal == null || normal.isEmpty()) return;

        ResourceLocation id = ResourceLocation.tryParse(normal);
        if (id == null) return;

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(id, screenX, screenY, 0, 0, dim.w, dim.h, dim.w, dim.h);
    }
}