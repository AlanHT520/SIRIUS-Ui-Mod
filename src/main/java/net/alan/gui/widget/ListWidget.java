package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.ListProps;
import net.alan.gui.data.style.TextureSet;
import net.alan.gui.render.BackgroundRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListWidget extends BaseWidget {
    private final ListProps props;
    private EditBox searchBox;
    private final List<RowDef> allRows;
    private double scrollAmount;
    private int selectedIndex = -1;
    private boolean scrolling;
    private double lastMouseY;
    private String filterText = "";

    public ListWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member, ListProps props) {
        super(id, layout, variables, member);
        this.props = props;

        this.allRows = new ArrayList<>();
        if (props.rows() != null) {
            this.allRows.addAll(props.rows());
        }

        if (props.search() != null) {
            ListProps.SearchDef sd = props.search();
            Minecraft minecraft = Minecraft.getInstance();
            Component hint = sd.hint() != null
                    ? Component.translatable(sd.hint())
                    : Component.empty();

            int color;
            try {
                color = Integer.decode(sd.textColor() != null ? sd.textColor() : "0xE0E0E0");
            } catch (NumberFormatException e) {
                color = 0xE0E0E0;
            }

            this.searchBox = new EditBox(minecraft.font, 0, 0, 100, 20, Component.empty());
            this.searchBox.setMaxLength(sd.maxLength() > 0 ? sd.maxLength() : 32);
            this.searchBox.setBordered(sd.bordered());
            this.searchBox.setHint(hint);
            this.searchBox.setValue(sd.initialValue() != null ? sd.initialValue() : "");
            this.searchBox.setTextColor(color);

            this.searchBox.setResponder(text -> {
                this.filterText = text != null ? text.toLowerCase() : "";
                this.member.put("search_text", this.filterText);
                this.scrollAmount = 0;
            });
        }
    }

    @Override
    public List<Widget> getChildren() {
        List<Widget> all = new ArrayList<>();
        for (RowDef row : allRows) {
            if (row.children != null) all.addAll(row.children);
        }
        return all;
    }

    private List<RowDef> getVisibleRows() {
        if (filterText.isEmpty()) return allRows;
        List<RowDef> visible = new ArrayList<>();
        for (RowDef row : allRows) {
            if (row.filterKey != null && row.filterKey.toLowerCase().contains(filterText)) {
                visible.add(row);
            }
        }
        this.member.put("filtered_count", String.valueOf(visible.size()));
        return visible;
    }

    private int getTotalContentHeight(List<RowDef> rows) {
        int total = 0;
        for (RowDef row : rows) {
            total += row.height;
        }
        if (rows.size() > 1) {
            total += (rows.size() - 1) * props.gap();
        }
        return total;
    }

    private double getMaxScroll(int listHeight, List<RowDef> visibleRows) {
        int contentHeight = getTotalContentHeight(visibleRows);
        return Math.max(0, contentHeight - listHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int listX = x + dim.x;
        int listY = y + dim.y;

        if (searchBox != null) {
            ListProps.SearchDef sd = props.search();
            int sx = evalExpr(sd.x(), mergedCtx, x, width, height);
            int sy = evalExpr(sd.y(), mergedCtx, y, width, height);
            int sw = evalExpr(sd.width(), mergedCtx, x, width, height);
            int sh = evalExpr(sd.height(), mergedCtx, y, width, height);
            searchBox.setRectangle(Math.max(1, sw), Math.max(1, sh), sx, sy);
            searchBox.render(graphics, mouseX, mouseY, delta);
        }

        List<RowDef> visibleRows = getVisibleRows();
        double maxScroll = getMaxScroll(dim.h, visibleRows);
        this.scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        renderBackground(graphics, listX, listY, dim.w, dim.h, mergedCtx);

        graphics.enableScissor(listX, listY, listX + dim.w, listY + dim.h);

        int currentY = listY - (int) scrollAmount;
        int rowIndex = 0;
        // 记录有 overlay 的 widget 位置，在 scissor 外渲染
        List<OverlayEntry> overlays = new ArrayList<>();
        for (RowDef row : visibleRows) {
            int rowTop = currentY;
            int rowBottom = currentY + row.height;

            if (rowBottom > listY && rowTop < listY + dim.h) {
                if (row.children != null) {
                    for (Widget child : row.children) {
                        child.render(graphics, listX, rowTop, dim.w, row.height,
                                mergedCtx, mouseX, mouseY, delta);
                        if (child.hasOverlay()) {
                            overlays.add(new OverlayEntry(child, rowTop));
                        }
                    }
                }
            }

            currentY += row.height + props.gap();
            rowIndex++;
        }

        graphics.disableScissor();

        // 渲染叠加层（如 dropdown 展开菜单，需要出现在 scissor 之外）
        for (OverlayEntry entry : overlays) {
            entry.widget.renderOverlay(graphics, listX, entry.rowY, dim.w, 0,
                    mergedCtx, mouseX, mouseY, delta);
        }

        if (props.scrollbar() != null && maxScroll > 0) {
            renderScrollbar(graphics, listX, listY, dim.w, dim.h, maxScroll, mergedCtx);
        }
    }

    private void renderBackground(GuiGraphics graphics, int listX, int listY, int listW, int listH, RenderContext ctx) {
        TextureSet bgTex = props.backgroundTexture();
        if (bgTex != null && bgTex.getNormal() != null) {
            ResourceLocation tex = ResourceLocation.tryParse(bgTex.getNormal());
            if (tex != null) {
                graphics.blit(tex, listX, listY, 0, 0, listW, listH, listW, listH);
                return;
            }
        }
        String bgColor = props.backgroundColor();
        if (bgColor != null) {
            try {
                int color = (int) Long.parseLong(bgColor.replace("0x", ""), 16);
                graphics.fill(listX, listY, listX + listW, listY + listH, color);
            } catch (NumberFormatException ignored) {}
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int listX, int listY, int listW, int listH, double maxScroll, RenderContext ctx) {
        ListProps.ScrollbarDef sb = props.scrollbar();
        int scrollbarW = sb.width() > 0 ? sb.width() : 6;
        int scrollbarX = computeScrollbarX(sb, listX, listW, scrollbarW, ctx);
        int scrollbarY = computeScrollbarY(sb, listY, listH, ctx);

        int barHeight = (int) ((double) listH * listH / (listH + maxScroll));
        barHeight = Math.max(16, Math.min(barHeight, listH - 8));
        int barY = (int) (scrollAmount * (listH - barHeight) / maxScroll);

        // track
        renderScrollPart(graphics, scrollbarX, scrollbarY, scrollbarW, listH, sb.track().texture(), sb.track().color(), 0x33000000);
        // thumb
        renderScrollPart(graphics, scrollbarX, scrollbarY + barY, scrollbarW, barHeight, sb.thumb().texture(), sb.thumb().color(), 0xAAFFFFFF);
    }

    private int computeScrollbarX(ListProps.ScrollbarDef sb, int listX, int listW, int scrollbarW, RenderContext ctx) {
        if (sb.x() == null || sb.x().isEmpty()) {
            return listX + listW - scrollbarW;
        }
        Map<String, Integer> vars = new HashMap<>();
        vars.put("parent.width", listW);
        vars.put("this.width", scrollbarW);
        vars.put("screen.width", ctx.screenWidth());
        vars.put("screen.height", ctx.screenHeight());
        return listX + eval(sb.x(), vars);
    }

    private int computeScrollbarY(ListProps.ScrollbarDef sb, int listY, int listH, RenderContext ctx) {
        if (sb.y() == null || sb.y().isEmpty()) {
            return listY;
        }
        Map<String, Integer> vars = new HashMap<>();
        vars.put("parent.height", listH);
        vars.put("screen.width", ctx.screenWidth());
        vars.put("screen.height", ctx.screenHeight());
        return listY + eval(sb.y(), vars);
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

    private int evalExpr(String expr, RenderContext ctx, int base, int screenW, int screenH) {
        if (expr == null) return 0;
        try {
            return Integer.parseInt(expr);
        } catch (NumberFormatException e) {
            Map<String, Integer> numVars = new HashMap<>();
            numVars.put("screen.width", ctx.screenWidth());
            numVars.put("screen.height", ctx.screenHeight());
            numVars.put("parent.width", screenW);
            numVars.put("parent.height", screenH);
            return eval(expr, numVars);
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
        int listX = x + dim.x;
        int listY = y + dim.y;

        if (searchBox != null) {
            if (searchBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (mouseX < listX || mouseX > listX + dim.w || mouseY < listY || mouseY > listY + dim.h) {
            return false;
        }

        List<RowDef> visibleRows = getVisibleRows();
        double maxScroll = getMaxScroll(dim.h, visibleRows);

        // 滚动条点击处理（含轨道跳转）
        if (props.scrollbar() != null && maxScroll > 0) {
            ListProps.ScrollbarDef sb = props.scrollbar();
            int scrollbarW = sb.width() > 0 ? sb.width() : 6;
            int scrollbarX = computeScrollbarX(sb, listX, dim.w, scrollbarW, mergedCtx);
            int scrollbarY = computeScrollbarY(sb, listY, dim.h, mergedCtx);
            int barHeight = (int) ((double) dim.h * dim.h / (dim.h + maxScroll));
            barHeight = Math.max(16, Math.min(barHeight, dim.h - 8));
            int barY = (int) (scrollAmount * (dim.h - barHeight) / maxScroll);

            // 点击滑块
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarW
                    && mouseY >= scrollbarY + barY && mouseY <= scrollbarY + barY + barHeight) {
                scrolling = true;
                lastMouseY = mouseY;
                return true;
            }
            // 点击轨道（跳转）
            if (mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarW
                    && mouseY >= scrollbarY && mouseY <= scrollbarY + dim.h) {
                int trackH = dim.h - barHeight;
                if (trackH > 0) {
                    double ratio = (mouseY - scrollbarY - barHeight / 2.0) / trackH;
                    scrollAmount = Math.max(0, Math.min(ratio * maxScroll, maxScroll));
                }
                scrolling = true;
                lastMouseY = mouseY;
                return true;
            }
        }

        int currentY = listY - (int) scrollAmount;
        int rowIndex = 0;
        for (RowDef row : visibleRows) {
            if (mouseY >= currentY && mouseY < currentY + row.height) {
                selectedIndex = rowIndex;
                if (row.children != null) {
                    for (Widget child : row.children) {
                        if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, listX, currentY, dim.w, row.height)) {
                            return true;
                        }
                    }
                }
                return true;
            }
            currentY += row.height + props.gap();
            rowIndex++;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        if (scrolling) {
            scrolling = false;
            return true;
        }

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int listX = x + dim.x;
        int listY = y + dim.y;

        List<RowDef> visibleRows = getVisibleRows();
        int currentY = listY - (int) scrollAmount;
        for (RowDef row : visibleRows) {
            if (row.children != null) {
                for (Widget child : row.children) {
                    child.mouseReleased(mouseX, mouseY, button, mergedCtx, listX, currentY, dim.w, row.height);
                }
            }
            currentY += row.height + props.gap();
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
        int listX = x + dim.x;
        int listY = y + dim.y;

        // 先处理滚动条拖拽
        if (scrolling) {
            List<RowDef> visibleRows = getVisibleRows();
            double maxScroll = getMaxScroll(dim.h, visibleRows);

            int barHeight = (int) ((double) dim.h * dim.h / (dim.h + maxScroll));
            barHeight = Math.max(16, Math.min(barHeight, dim.h - 8));
            double delta = (mouseY - lastMouseY) * maxScroll / (dim.h - barHeight);
            scrollAmount = Math.max(0, Math.min(scrollAmount + delta, maxScroll));
            lastMouseY = mouseY;
            return true;
        }

        // 转发给子组件（让 slider 等组件可以拖拽）
        List<RowDef> visibleRows = getVisibleRows();
        int currentY = listY - (int) scrollAmount;
        for (RowDef row : visibleRows) {
            if (row.children != null) {
                for (Widget child : row.children) {
                    if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY, mergedCtx, listX, currentY, dim.w, row.height)) {
                        return true;
                    }
                }
            }
            currentY += row.height + props.gap();
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY,
                           RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return;
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int listX = x + dim.x;
        int listY = y + dim.y;
        List<RowDef> visibleRows = getVisibleRows();
        int currentY = listY - (int) scrollAmount;
        for (RowDef row : visibleRows) {
            if (row.children != null) {
                for (Widget child : row.children) {
                    child.mouseMoved(mouseX, mouseY, mergedCtx, listX, currentY, dim.w, row.height);
                }
            }
            currentY += row.height + props.gap();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int listX = x + dim.x;
        int listY = y + dim.y;

        if (mouseX < listX || mouseX > listX + dim.w || mouseY < listY || mouseY > listY + dim.h) {
            return false;
        }

        List<RowDef> visibleRows = getVisibleRows();
        double maxScroll = getMaxScroll(dim.h, visibleRows);
        // 修正：滚轮向上 scrollY>0 时减少滚动量（内容向下移动）
        scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * 36, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }

        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers,
                             RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(codePoint, modifiers);
        }
        return false;
    }

    public record RowDef(
            int height,
            String filterKey,
            List<Widget> children
    ) {}

    private record OverlayEntry(Widget widget, int rowY) {}
}