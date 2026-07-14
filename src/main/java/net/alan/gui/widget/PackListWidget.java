package net.alan.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alan.gui.context.RenderContext;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PackListWidget extends BaseWidget {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackListWidget.class);

    public enum Mode {
        ACTIVE, UNACTIVE, ALL
    }

    public enum Ordering {
        ACTIVE_FIRST, UNACTIVE_FIRST, MIXED
    }

    public static class RowStyle {
        public final String backgroundColor;
        public final String backgroundTexture;
        public final String textColor;
        public final String hoverColor;
        public final String toggleColor;
        public final String toggleHoverColor;

        public RowStyle(String bg, String tex, String text, String hover, String toggle, String toggleHover) {
            this.backgroundColor = bg;
            this.backgroundTexture = tex;
            this.textColor = text;
            this.hoverColor = hover;
            this.toggleColor = toggle;
            this.toggleHoverColor = toggleHover;
        }

        public static final RowStyle DEFAULT_ACTIVE = new RowStyle(
            "0x2000AA00", null, "0xFFFFFFFF", "0x4000AA00", "0xCC00AA00", "0xCC55FF55"
        );
        public static final RowStyle DEFAULT_UNACTIVE = new RowStyle(
            "0x20000000", null, "0xCCAAAAAA", "0x40000000", "0xCCAA0000", "0xCCFF5555"
        );
    }

    public static class DividerStyle {
        public final int height;
        public final String color;
        public final String texture;

        public DividerStyle(int height, String color, String texture) {
            this.height = height;
            this.color = color;
            this.texture = texture;
        }

        public static final DividerStyle DEFAULT = new DividerStyle(2, "0x55FFFFFF", null);
    }

    private final PackDataSource dataSource;
    private final Mode mode;
    private final Ordering ordering;
    private final ActionExecutor executor;
    private final int entryHeight;
    private final int entryGap;
    private final String backgroundColor;
    private final RowStyle activeRowStyle;
    private final RowStyle unactiveRowStyle;
    private final DividerStyle dividerStyle;
    private final Minecraft minecraft;

    private List<PackDataSource.PackEntryData> entries;
    private double scrollAmount;
    private boolean scrolling;
    private double lastMouseY;
    private int hoveredToggleIndex = -1;
    private int hoveredMoveUpIndex = -1;
    private int hoveredMoveDownIndex = -1;
    private int dragIndex = -1;
    private int dragTargetIndex = -1;
    private double dragOffsetY;
    private boolean isDragging;

    public PackListWidget(String id, LayoutProps layout, Map<String, String> variables,
                          Map<String, String> member, PackDataSource dataSource,
                          String modeStr, ActionExecutor executor,
                          int entryHeight, int entryGap, String backgroundColor,
                          RowStyle activeRowStyle, RowStyle unactiveRowStyle,
                          DividerStyle dividerStyle, String orderingStr) {
        super(id, layout, variables, member);
        this.dataSource = dataSource;
        this.mode = parseMode(modeStr);
        this.ordering = parseOrdering(orderingStr);
        this.executor = executor;
        this.entryHeight = entryHeight;
        this.entryGap = entryGap;
        this.backgroundColor = backgroundColor;
        this.activeRowStyle = activeRowStyle != null ? activeRowStyle : RowStyle.DEFAULT_ACTIVE;
        this.unactiveRowStyle = unactiveRowStyle != null ? unactiveRowStyle : RowStyle.DEFAULT_UNACTIVE;
        this.dividerStyle = dividerStyle != null ? dividerStyle : DividerStyle.DEFAULT;
        this.minecraft = Minecraft.getInstance();
        this.entries = new ArrayList<>();

        dataSource.addListener(this::refreshEntries);
        refreshEntries();
    }

    private static Ordering parseOrdering(String orderingStr) {
        if (orderingStr == null) return Ordering.ACTIVE_FIRST;
        return switch (orderingStr.toLowerCase()) {
            case "unactive_first" -> Ordering.UNACTIVE_FIRST;
            case "mixed" -> Ordering.MIXED;
            default -> Ordering.ACTIVE_FIRST;
        };
    }

    private static Mode parseMode(String modeStr) {
        if (modeStr == null) return Mode.ALL;
        return switch (modeStr.toLowerCase()) {
            case "active" -> Mode.ACTIVE;
            case "unactive" -> Mode.UNACTIVE;
            default -> Mode.ALL;
        };
    }

    public void refreshEntries() {
        List<PackDataSource.PackEntryData> active = dataSource.getActivePacks();
        List<PackDataSource.PackEntryData> unactive = dataSource.getUnactivePacks();

        entries = switch (mode) {
            case ACTIVE -> active;
            case UNACTIVE -> unactive;
            case ALL -> {
                List<PackDataSource.PackEntryData> all = new ArrayList<>(active);
                List<PackDataSource.PackEntryData> sortedUnactive = new ArrayList<>(unactive);
                sortedUnactive.sort(Comparator.comparing(
                    PackDataSource.PackEntryData::getTitle,
                    String.CASE_INSENSITIVE_ORDER
                ));
                all.addAll(sortedUnactive);
                yield all;
            }
        };
    }

    private int getTotalContentHeight() {
        if (entries.isEmpty()) return 0;
        return entries.size() * entryHeight + (entries.size() - 1) * entryGap;
    }

    private static int parseColor(String colorStr) {
        if (colorStr == null) return 0xFFFFFFFF;
        try {
            return (int) Long.parseLong(colorStr.replace("0x", "").replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        dataSource.tick();

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int listX = x + dim.x;
        int listY = y + dim.y;

        if (backgroundColor != null) {
            int bgColor = parseColor(backgroundColor);
            graphics.fill(listX, listY, listX + dim.w, listY + dim.h, bgColor);
        }

        int totalH = getTotalContentHeight();
        double maxScroll = Math.max(0, totalH - dim.h);
        scrollAmount = Math.max(0, Math.min(scrollAmount, maxScroll));

        graphics.enableScissor(listX, listY, listX + dim.w, listY + dim.h);

        int currentY = listY - (int) scrollAmount;
        hoveredToggleIndex = -1;
        hoveredMoveUpIndex = -1;
        hoveredMoveDownIndex = -1;

        for (int i = 0; i < entries.size(); i++) {

            int rowTop = currentY;
            int rowBottom = currentY + entryHeight;

            if (rowBottom > listY && rowTop < listY + dim.h) {
                PackDataSource.PackEntryData entry = entries.get(i);
                RowStyle style = entry.isActive() ? activeRowStyle : unactiveRowStyle;

                renderRowBackground(graphics, listX, rowTop, dim.w, entryHeight, style, entry);

                if (isDragging && i == dragTargetIndex) {
                    graphics.fill(listX, rowTop - 1, listX + dim.w, rowTop + 1, 0xCCFFFFFF);
                }

                if (isDragging && i == dragIndex) {
                    renderEntry(graphics, listX, rowTop + (int) dragOffsetY, dim.w, entryHeight,
                        entry, style, mouseX, mouseY, i, 0.5f);
                } else {
                    renderEntry(graphics, listX, rowTop, dim.w, entryHeight,
                        entry, style, mouseX, mouseY, i, 1.0f);
                }
            }

            currentY += entryHeight + entryGap;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarW = 4;
            int scrollbarX = listX + dim.w - scrollbarW - 2;
            int scrollbarH = (int) ((double) dim.h * dim.h / (dim.h + maxScroll));
            scrollbarH = Math.max(16, Math.min(scrollbarH, dim.h - 8));
            int scrollbarY = listY + (int) (scrollAmount * (dim.h - scrollbarH) / maxScroll);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarW, scrollbarY + scrollbarH, 0x44FFFFFF);
        }
    }

    private void renderRowBackground(GuiGraphics graphics, int listX, int rowTop,
                                      int listW, int rowH, RowStyle style, PackDataSource.PackEntryData entry) {
        if (style.backgroundTexture != null) {
            ResourceLocation tex = ResourceLocation.parse(style.backgroundTexture);
            graphics.blit(tex, listX, rowTop, 0, 0, listW, rowH, listW, rowH);
            return;
        }

        String bgColor = style.backgroundColor;
        if (!entry.isCompatible()) {
            bgColor = "0x55897843";
        }
        if (bgColor != null) {
            int color = parseColor(bgColor);
            if (color != 0) {
                graphics.fill(listX, rowTop, listX + listW, rowTop + rowH, color);
            }
        }
    }

    private void renderEntry(GuiGraphics graphics, int listX, int rowTop, int listW, int rowH,
                             PackDataSource.PackEntryData entry, RowStyle style,
                             int mouseX, int mouseY, int index, float opacity) {
        int iconX = listX + 4;
        int iconY = rowTop + (rowH - 32) / 2;
        int iconW = 32;
        int iconH = 32;

        ResourceLocation iconTex = entry.getIconTexture();
        if (iconTex != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
            graphics.blit(iconTex, iconX, iconY, 0, 0, iconW, iconH, iconW, iconH);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            int pc = 0xFF555555;
            graphics.fill(iconX, iconY, iconX + iconW, iconY + iconH, pc);
        }

        int textX = iconX + iconW + 6;
        int textColor = parseColor(style.textColor);
        if (!entry.isCompatible()) textColor = 0xFFFF5555;

        String title = entry.getTitle();
        if (title.length() > 30) title = title.substring(0, 28) + "...";
        int titleColor = (int) ((textColor & 0x00FFFFFF) | ((int) (((textColor >> 24) & 0xFF) * opacity) << 24));
        graphics.drawString(minecraft.font, title, textX, rowTop + 4, titleColor);

        String desc = entry.getExtendedDescription();
        if (desc.length() > 40) desc = desc.substring(0, 38) + "...";
        int descColor = parseColor(entry.isActive() ? style.textColor : unactiveRowStyle.textColor);
        descColor = (int) ((descColor & 0x00FFFFFF) | ((int) (((descColor >> 24) & 0xFF) * 0.6f) << 24));
        if (!entry.isCompatible()) descColor = refineColor(0xCCFF8888, opacity);
        descColor = refineColor(descColor, opacity);
        graphics.drawString(minecraft.font, desc, textX, rowTop + 16, descColor);

        if (!entry.isCompatible() && entry.getCompatibilityDescription() != null) {
            String compat = entry.getCompatibilityDescription();
            if (compat.length() > 40) compat = compat.substring(0, 38) + "...";
            graphics.drawString(minecraft.font, compat, textX, rowTop + 28, 0xFFFF5555);
        }

        String sourceLabel = entry.getSourceLabel();
        if (sourceLabel != null && !sourceLabel.isEmpty()) {
            int srcW = minecraft.font.width(sourceLabel);
            graphics.drawString(minecraft.font, sourceLabel, listX + listW - srcW - 24, rowTop + 4, 0xCCAAAAAA);
        }

        boolean mouseInRow = mouseX >= listX && mouseX <= listX + listW
            && mouseY >= rowTop && mouseY < rowTop + rowH;

        if (mouseInRow && !isDragging) {
            int toggleX = listX + listW - 20;
            int toggleY = rowTop + (rowH - 16) / 2;
            int toggleW = 16;
            int toggleH = 16;

            if (entry.canActivate()) {
                boolean hovered = mouseX >= toggleX && mouseX <= toggleX + toggleW
                    && mouseY >= toggleY && mouseY <= toggleY + toggleH;
                if (hovered) hoveredToggleIndex = index;
                int toggleColor = hovered
                    ? parseColor(style.toggleHoverColor)
                    : parseColor(style.toggleColor);
                graphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, toggleColor);
                graphics.drawString(minecraft.font, "+", toggleX + 4, toggleY + 1, 0xFFFFFFFF);
            } else if (entry.canDeactivate()) {
                boolean hovered = mouseX >= toggleX && mouseX <= toggleX + toggleW
                    && mouseY >= toggleY && mouseY <= toggleY + toggleH;
                if (hovered) hoveredToggleIndex = index;
                int toggleColor = hovered
                    ? parseColor(unactiveRowStyle.toggleHoverColor)
                    : parseColor(unactiveRowStyle.toggleColor);
                graphics.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, toggleColor);
                graphics.drawString(minecraft.font, "x", toggleX + 4, toggleY + 1, 0xFFFFFFFF);
            } else if (entry.isRequired()) {
                graphics.drawString(minecraft.font, "!", toggleX + 4, toggleY + 1, 0xCCAAAAAA);
            }

            if (mode != Mode.UNACTIVE && entry.canMoveUp()) {
                int upX = toggleX - 20;
                int upY = toggleY;
                boolean upHovered = mouseX >= upX && mouseX <= upX + 16
                    && mouseY >= upY && mouseY <= upY + 8;
                if (upHovered) hoveredMoveUpIndex = index;
                int upColor = upHovered ? 0xCCFFFFFF : 0xCC888888;
                graphics.fill(upX, upY, upX + 16, upY + 8, 0x33000000);
                graphics.drawString(minecraft.font, "▲", upX + 2, upY, upColor);
            }

            if (mode != Mode.UNACTIVE && entry.canMoveDown()) {
                int downX = toggleX - 20;
                int downY = toggleY + 8;
                boolean downHovered = mouseX >= downX && mouseX <= downX + 16
                    && mouseY >= downY && mouseY <= downY + 8;
                if (downHovered) hoveredMoveDownIndex = index;
                int downColor = downHovered ? 0xCCFFFFFF : 0xCC888888;
                graphics.fill(downX, downY, downX + 16, downY + 8, 0x33000000);
                graphics.drawString(minecraft.font, "▼", downX + 2, downY, downColor);
            }
        }
    }

    private static int refineColor(int color, float opacity) {
        return (int) ((color & 0x00FFFFFF) | ((int) (((color >> 24) & 0xFF) * opacity) << 24));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;

        int listX = x + dim.x;
        int listY = y + dim.y + (int) scrollAmount;

        if (mouseX < listX || mouseX > listX + dim.w || mouseY < y + dim.y || mouseY > y + dim.y + dim.h) {
            return false;
        }

        int adjustedY = (int) mouseY + (int) scrollAmount - (y + dim.y);

        if (hoveredToggleIndex >= 0 && hoveredToggleIndex < entries.size()) {
            PackDataSource.PackEntryData entry = entries.get(hoveredToggleIndex);
            if (entry.canActivate()) {
                handleToggle(entry);
                return true;
            } else if (entry.canDeactivate()) {
                handleToggle(entry);
                return true;
            }
        }

        if (hoveredMoveUpIndex >= 0 && hoveredMoveUpIndex < entries.size()) {
            PackDataSource.PackEntryData entry = entries.get(hoveredMoveUpIndex);
            if (entry.canMoveUp()) {
                PackSelectionModel.Entry modelEntry = dataSource.getEntry(entry.getId());
                if (modelEntry != null) {
                    modelEntry.moveUp();
                    refreshEntries();
                }
                return true;
            }
        }

        if (hoveredMoveDownIndex >= 0 && hoveredMoveDownIndex < entries.size()) {
            PackDataSource.PackEntryData entry = entries.get(hoveredMoveDownIndex);
            if (entry.canMoveDown()) {
                PackSelectionModel.Entry modelEntry = dataSource.getEntry(entry.getId());
                if (modelEntry != null) {
                    modelEntry.moveDown();
                    refreshEntries();
                }
                return true;
            }
        }

        if (mode != Mode.UNACTIVE && button == 0) {
            int entryIndex = adjustedY / (entryHeight + entryGap);
            if (entryIndex >= 0 && entryIndex < entries.size()) {
                PackDataSource.PackEntryData entry = entries.get(entryIndex);
                if (!entry.isFixedPosition()) {
                    dragIndex = entryIndex;
                    isDragging = true;
                    dragOffsetY = 0;
                    dragTargetIndex = entryIndex;
                    lastMouseY = mouseY;
                    return true;
                }
            }
        }

        return false;
    }

    private void handleToggle(PackDataSource.PackEntryData entry) {
        PackSelectionModel.Entry modelEntry = dataSource.getEntry(entry.getId());
        if (modelEntry == null) return;

        if (entry.canActivate()) {
            if (entry.isCompatible()) {
                modelEntry.select();
                refreshEntries();
            } else {
                Screen parentScreen = Minecraft.getInstance().screen;
                Minecraft.getInstance().setScreen(new ConfirmScreen(
                    confirmed -> {
                        Minecraft.getInstance().setScreen(parentScreen);
                        if (confirmed) {
                            modelEntry.select();
                            refreshEntries();
                        }
                    },
                    Component.translatable("pack.incompatible.confirm.title"),
                    Component.literal(entry.getCompatibilityConfirmation())
                ));
            }
        } else if (entry.canDeactivate()) {
            modelEntry.unselect();
            refreshEntries();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        if (isDragging && dragIndex >= 0 && dragTargetIndex >= 0 && dragTargetIndex != dragIndex) {
            PackDataSource.PackEntryData entry = entries.get(dragIndex);
            PackSelectionModel.Entry modelEntry = dataSource.getEntry(entry.getId());
            if (modelEntry != null) {
                int diff = dragTargetIndex - dragIndex;
                if (diff > 0) {
                    for (int i = 0; i < diff; i++) {
                        if (modelEntry.canMoveDown()) {
                            modelEntry.moveDown();
                        }
                    }
                } else {
                    for (int i = 0; i < -diff; i++) {
                        if (modelEntry.canMoveUp()) {
                            modelEntry.moveUp();
                        }
                    }
                }
                refreshEntries();
            }
        }
        isDragging = false;
        dragIndex = -1;
        dragTargetIndex = -1;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                 RenderContext context, int x, int y, int width, int height) {
        if (isDragging && dragIndex >= 0) {
            dragOffsetY += dragY;

            RenderContext mergedCtx = mergeContext(context);
            WidgetDimension dim = computeLayout(mergedCtx, width, height);
            int listY = y + dim.y;

            int adjustedY = (int) mouseY + (int) scrollAmount - listY;
            int targetIndex = adjustedY / (entryHeight + entryGap);
            targetIndex = Math.max(0, Math.min(targetIndex, entries.size() - 1));

            PackDataSource.PackEntryData targetEntry = entries.get(targetIndex);
            if (targetEntry.isFixedPosition()) {
                targetIndex = dragIndex;
            }

            dragTargetIndex = targetIndex;

            if (mouseY < listY && scrollAmount > 0) {
                scrollAmount = Math.max(0, scrollAmount - 4);
            } else if (mouseY > listY + dim.h) {
                int maxScroll = Math.max(0, getTotalContentHeight() - dim.h);
                scrollAmount = Math.min(maxScroll, scrollAmount + 4);
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                  RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;

        int listX = x + dim.x;
        int listY = y + dim.y;

        if (mouseX < listX || mouseX > listX + dim.w || mouseY < listY || mouseY > listY + dim.h) {
            return false;
        }

        int totalH = getTotalContentHeight();
        double maxScroll = Math.max(0, totalH - dim.h);
        scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * 20, maxScroll));
        return true;
    }
}