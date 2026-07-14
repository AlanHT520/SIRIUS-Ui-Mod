package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.ListProps;
import net.alan.gui.data.style.TextureSet;
import net.alan.gui.render.BackgroundRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ContentWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentWidget.class);

    private final List<Widget> buttons;
    private final int gap;
    private final ListProps.ScrollbarDef scrollbar;
    private final String backgroundColor;
    private final TextureSet backgroundTexture;
    private final String borderColor;
    private final String direction;
    private double scrollAmount;
    private boolean scrolling;
    private double lastMouseY;
    private double lastMouseX;

    public ContentWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                         List<Widget> buttons, int gap, ListProps.ScrollbarDef scrollbar,
                         String backgroundColor, String borderColor, TextureSet backgroundTexture,
                         String direction) {
        super(id, layout, variables, member);
        this.buttons = buttons;
        this.gap = gap;
        this.scrollbar = scrollbar;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.backgroundTexture = backgroundTexture;
        this.direction = direction != null ? direction : "vertical";
        this.scrollAmount = 0;
        this.scrolling = false;
    }

    private boolean isHorizontal() {
        return "horizontal".equals(direction);
    }

    @Override
    public List<Widget> getChildren() {
        return buttons;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (backgroundTexture != null && backgroundTexture.getNormal() != null) {
            var texId = ResourceLocation.tryParse(backgroundTexture.getNormal());
            if (texId != null) {
                graphics.blitSprite(texId, cx, cy, dim.w, dim.h);
            }
        } else if (backgroundColor != null && !backgroundColor.isEmpty()) {
            graphics.fill(cx, cy, cx + dim.w, cy + dim.h, BackgroundRenderer.parseColor(backgroundColor));
        }

        int maxScroll;
        if (isHorizontal()) {
            int totalW = calcTotalW(mergedCtx, dim.w, dim.h);
            maxScroll = Math.max(0, totalW - dim.w);
        } else {
            int totalH = calcTotalH(mergedCtx, dim.w, dim.h);
            maxScroll = Math.max(0, totalH - dim.h);
        }
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        graphics.enableScissor(cx, cy, cx + dim.w, cy + dim.h);

        List<OverlayEntry> overlays = new ArrayList<>();

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                int childRight = currentX + childDim.w;

                if (childRight > cx && currentX < cx + dim.w) {
                    child.render(graphics, currentX, cy, childDim.w, dim.h,
                            mergedCtx, mouseX, mouseY, delta);
                    if (child.hasOverlay()) {
                        overlays.add(new OverlayEntry(child, currentX, cy, childDim.w, dim.h));
                    }
                }
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                int childBottom = currentY + childDim.h;

                if (childBottom > cy && currentY < cy + dim.h) {
                    child.render(graphics, cx, currentY, dim.w, childDim.h,
                            mergedCtx, mouseX, mouseY, delta);
                    if (child.hasOverlay()) {
                        overlays.add(new OverlayEntry(child, cx, currentY, dim.w, childDim.h));
                    }
                }
                currentY += childDim.h + gap;
            }
        }

        graphics.disableScissor();

        for (OverlayEntry entry : overlays) {
            entry.widget.renderOverlay(graphics, entry.x, entry.y, entry.w, entry.h,
                    mergedCtx, mouseX, mouseY, delta);
        }

        if (borderColor != null && !borderColor.isEmpty()) {
            int bc = BackgroundRenderer.parseColor(borderColor);
            graphics.fill(cx - 1, cy - 1, cx + dim.w + 1, cy, bc);
            graphics.fill(cx - 1, cy + dim.h, cx + dim.w + 1, cy + dim.h + 1, bc);
            graphics.fill(cx - 1, cy - 1, cx, cy + dim.h + 1, bc);
            graphics.fill(cx + dim.w, cy - 1, cx + dim.w + 1, cy + dim.h + 1, bc);
        }

        if (maxScroll > 0 && scrollbar != null) {
            if (isHorizontal()) {
                renderScrollbarH(graphics, cx, cy, dim.w, dim.h, maxScroll);
            } else {
                renderScrollbarV(graphics, cx, cy, dim.w, dim.h, maxScroll);
            }
        }
    }

    private void renderScrollbarV(GuiGraphics graphics, int cx, int cy, int cw, int ch, int maxScroll) {
        int sbW = scrollbar.width();
        int sbX = computeScrollbarX(cx, cw, sbW);
        int sbY = computeScrollbarY(cy, ch);

        ListProps.TrackDef track = scrollbar.track();
        if (track.texture() != null && track.texture().getNormal() != null) {
            var texId = ResourceLocation.tryParse(track.texture().getNormal());
            if (texId != null) {
                graphics.blitSprite(texId, sbX, sbY, sbW, ch);
            }
        } else if (track.color() != null) {
            graphics.fill(sbX, sbY, sbX + sbW, sbY + ch, BackgroundRenderer.parseColor(track.color()));
        } else {
            graphics.fill(sbX, sbY, sbX + sbW, sbY + ch, 0x33000000);
        }

        ListProps.ThumbDef thumb = scrollbar.thumb();
        int thumbH = Math.max(16, ch * ch / (ch + maxScroll));
        int thumbY = cy + (int) ((scrollAmount / maxScroll) * (ch - thumbH));

        if (thumb.texture() != null && thumb.texture().getNormal() != null) {
            var texId = ResourceLocation.tryParse(thumb.texture().getNormal());
            if (texId != null) {
                graphics.blitSprite(texId, sbX, thumbY, sbW, thumbH);
            }
        } else if (thumb.color() != null) {
            renderScrollPart(graphics, sbX, thumbY, sbW, thumbH, null, thumb.color(), 0xAAFFFFFF);
        } else {
            renderScrollPart(graphics, sbX, thumbY, sbW, thumbH, null, null, 0xAAFFFFFF);
        }
    }

    private void renderScrollbarH(GuiGraphics graphics, int cx, int cy, int cw, int ch, int maxScroll) {
        int sbH = scrollbar.width();
        int sbX = cx;
        int sbY = cy + ch - sbH;

        ListProps.TrackDef track = scrollbar.track();
        if (track.texture() != null && track.texture().getNormal() != null) {
            var texId = ResourceLocation.tryParse(track.texture().getNormal());
            if (texId != null) {
                graphics.blitSprite(texId, sbX, sbY, cw, sbH);
            }
        } else if (track.color() != null) {
            graphics.fill(sbX, sbY, sbX + cw, sbY + sbH, BackgroundRenderer.parseColor(track.color()));
        } else {
            graphics.fill(sbX, sbY, sbX + cw, sbY + sbH, 0x33000000);
        }

        ListProps.ThumbDef thumb = scrollbar.thumb();
        int thumbW = Math.max(16, cw * cw / (cw + maxScroll));
        int thumbX = cx + (int) ((scrollAmount / maxScroll) * (cw - thumbW));

        if (thumb.texture() != null && thumb.texture().getNormal() != null) {
            var texId = ResourceLocation.tryParse(thumb.texture().getNormal());
            if (texId != null) {
                graphics.blitSprite(texId, thumbX, sbY, thumbW, sbH);
            }
        } else if (thumb.color() != null) {
            renderScrollPart(graphics, thumbX, sbY, thumbW, sbH, null, thumb.color(), 0xAAFFFFFF);
        } else {
            renderScrollPart(graphics, thumbX, sbY, thumbW, sbH, null, null, 0xAAFFFFFF);
        }
    }

    private int computeScrollbarX(int cx, int cw, int sbW) {
        if (scrollbar.x() == null || scrollbar.x().isEmpty()) {
            return cx + cw - sbW;
        }
        Map<String, Integer> vars = new HashMap<>();
        vars.put("parent.width", cw);
        vars.put("this.width", sbW);
        return cx + eval(scrollbar.x(), vars);
    }

    private int computeScrollbarY(int cy, int ch) {
        if (scrollbar.y() == null || scrollbar.y().isEmpty()) {
            return cy;
        }
        Map<String, Integer> vars = new HashMap<>();
        vars.put("parent.height", ch);
        return cy + eval(scrollbar.y(), vars);
    }

    private void renderScrollPart(GuiGraphics graphics, int x, int y, int w, int h, TextureSet tex, String color, int defaultColor) {
        if (tex != null && tex.getNormal() != null) {
            ResourceLocation rl = ResourceLocation.tryParse(tex.getNormal());
            if (rl != null) {
                graphics.blit(rl, x, y, 0, 0, w, h, w, h);
                return;
            }
        }
        int c = defaultColor;
        if (color != null && !color.isEmpty()) {
            c = BackgroundRenderer.parseColor(color);
        }
        graphics.fill(x, y, x + w, y + h, c);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (mouseX < cx || mouseX > cx + dim.w || mouseY < cy || mouseY > cy + dim.h) {
            return false;
        }

        int maxScroll;
        if (isHorizontal()) {
            int totalW = calcTotalW(mergedCtx, dim.w, dim.h);
            maxScroll = Math.max(0, totalW - dim.w);
        } else {
            int totalH = calcTotalH(mergedCtx, dim.w, dim.h);
            maxScroll = Math.max(0, totalH - dim.h);
        }

        if (maxScroll > 0 && scrollbar != null) {
            if (isHorizontal()) {
                int sbH = scrollbar.width();
                int sbX = cx;
                int sbY = cy + dim.h - sbH;
                if (mouseX >= sbX && mouseX <= sbX + dim.w && mouseY >= sbY && mouseY <= sbY + sbH) {
                    int thumbW = Math.max(16, dim.w * dim.w / (dim.w + maxScroll));
                    int trackW = dim.w - thumbW;
                    if (trackW > 0) {
                        double ratio = (mouseX - sbX - thumbW / 2.0) / trackW;
                        scrollAmount = Math.max(0, Math.min(ratio * maxScroll, maxScroll));
                    }
                    scrolling = true;
                    lastMouseX = mouseX;
                    return true;
                }
            } else {
                int sbW = scrollbar.width();
                int sbX = computeScrollbarX(cx, dim.w, sbW);
                int sbY = computeScrollbarY(cy, dim.h);
                if (mouseX >= sbX && mouseX <= sbX + sbW && mouseY >= sbY && mouseY <= sbY + dim.h) {
                    int thumbH = Math.max(16, dim.h * dim.h / (dim.h + maxScroll));
                    int trackH = dim.h - thumbH;
                    if (trackH > 0) {
                        double ratio = (mouseY - sbY - thumbH / 2.0) / trackH;
                        scrollAmount = Math.max(0, Math.min(ratio * maxScroll, maxScroll));
                    }
                    scrolling = true;
                    lastMouseY = mouseY;
                    return true;
                }
            }
        }

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, currentX, cy, childDim.w, dim.h)) {
                    return true;
                }
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, cx, currentY, dim.w, childDim.h)) {
                    return true;
                }
                currentY += childDim.h + gap;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        if (scrolling) {
            scrolling = false;
            return true;
        }
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                child.mouseReleased(mouseX, mouseY, button, mergedCtx, currentX, cy, childDim.w, dim.h);
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                child.mouseReleased(mouseX, mouseY, button, mergedCtx, cx, currentY, dim.w, childDim.h);
                currentY += childDim.h + gap;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY, RenderContext context,
                                int x, int y, int width, int height) {
        if (scrolling) {
            RenderContext mergedCtx = mergeContext(context);
            WidgetDimension dim = computeLayout(mergedCtx, width, height);
            Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
            if (!checkCondition(vars)) return false;

            if (isHorizontal()) {
                double deltaX = mouseX - lastMouseX;
                lastMouseX = mouseX;
                int totalW = calcTotalW(mergedCtx, dim.w, dim.h);
                int maxScroll = Math.max(0, totalW - dim.w);
                if (maxScroll > 0) {
                    int thumbW = Math.max(16, dim.w * dim.w / (dim.w + maxScroll));
                    double ratio = deltaX / (dim.w - thumbW);
                    scrollAmount = Math.max(0, Math.min(scrollAmount + ratio * maxScroll, maxScroll));
                }
            } else {
                double deltaY = mouseY - lastMouseY;
                lastMouseY = mouseY;
                int totalH = calcTotalH(mergedCtx, dim.w, dim.h);
                int maxScroll = Math.max(0, totalH - dim.h);
                if (maxScroll > 0) {
                    int thumbH = Math.max(16, dim.h * dim.h / (dim.h + maxScroll));
                    double ratio = deltaY / (dim.h - thumbH);
                    scrollAmount = Math.max(0, Math.min(scrollAmount + ratio * maxScroll, maxScroll));
                }
            }
            return true;
        }
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx,
                        currentX, cy, childDim.w, dim.h)) {
                    return true;
                }
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx,
                        cx, currentY, dim.w, childDim.h)) {
                    return true;
                }
                currentY += childDim.h + gap;
            }
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

        if (isHorizontal()) {
            int totalW = calcTotalW(mergedCtx, dim.w, dim.h);
            int maxScroll = Math.max(0, totalW - dim.w);
            scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * 40, maxScroll));
        } else {
            int totalH = calcTotalH(mergedCtx, dim.w, dim.h);
            int maxScroll = Math.max(0, totalH - dim.h);
            scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * 36, maxScroll));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.keyPressed(keyCode, scanCode, modifiers, mergedCtx, currentX, cy, childDim.w, dim.h)) {
                    return true;
                }
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.keyPressed(keyCode, scanCode, modifiers, mergedCtx, cx, currentY, dim.w, childDim.h)) {
                    return true;
                }
                currentY += childDim.h + gap;
            }
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
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.charTyped(codePoint, modifiers, mergedCtx, currentX, cy, childDim.w, dim.h)) {
                    return true;
                }
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                if (child.charTyped(codePoint, modifiers, mergedCtx, cx, currentY, dim.w, childDim.h)) {
                    return true;
                }
                currentY += childDim.h + gap;
            }
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
        int cx = x + dim.x;
        int cy = y + dim.y;

        if (isHorizontal()) {
            int currentX = cx - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                child.mouseMoved(mouseX, mouseY, mergedCtx, currentX, cy, childDim.w, dim.h);
                currentX += childDim.w + gap;
            }
        } else {
            int currentY = cy - (int) scrollAmount;
            for (Widget child : buttons) {
                WidgetDimension childDim = child.computeLayout(mergedCtx, dim.w, dim.h);
                child.mouseMoved(mouseX, mouseY, mergedCtx, cx, currentY, dim.w, childDim.h);
                currentY += childDim.h + gap;
            }
        }
    }

    private int calcTotalH(RenderContext ctx, int parentW, int parentH) {
        int total = 0;
        for (Widget child : buttons) {
            WidgetDimension childDim = child.computeLayout(ctx, parentW, parentH);
            total += childDim.h;
        }
        total += Math.max(0, buttons.size() - 1) * gap;
        return total;
    }

    private int calcTotalW(RenderContext ctx, int parentW, int parentH) {
        int total = 0;
        for (Widget child : buttons) {
            WidgetDimension childDim = child.computeLayout(ctx, parentW, parentH);
            total += childDim.w;
        }
        total += Math.max(0, buttons.size() - 1) * gap;
        return total;
    }

    private record OverlayEntry(Widget widget, int x, int y, int w, int h) {}
}