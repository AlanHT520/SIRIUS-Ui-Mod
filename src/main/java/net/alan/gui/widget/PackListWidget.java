package net.alan.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alan.gui.context.RenderContext;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.TextureSet;
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
        public final String selectedColor;
        public final String toggleColor;
        public final String toggleHoverColor;

        public RowStyle(String bg, String tex, String text, String hover, String selected,
                        String toggle, String toggleHover) {
            this.backgroundColor = bg;
            this.backgroundTexture = tex;
            this.textColor = text;
            this.hoverColor = hover;
            this.selectedColor = selected;
            this.toggleColor = toggle;
            this.toggleHoverColor = toggleHover;
        }

        public static final RowStyle DEFAULT_ACTIVE = new RowStyle(
            "0x2000AA00", null, "0xFFFFFFFF", "0x4000AA00", "0x6000AA00",
            "0xCC00AA00", "0xCC55FF55"
        );
        public static final RowStyle DEFAULT_UNACTIVE = new RowStyle(
            "0x20000000", null, "0xCCAAAAAA", "0x40000000", "0x60000000",
            "0xCCAA0000", "0xCCFF5555"
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

    public static class ListButtonConfig {
        public final TextureSet texture;
        public final String text;
        public final String textKey;
        public final String textColor;
        public final String highlightedTextColor;
        public final boolean textShadow;
        public final int width;
        public final int height;
        public final Set<String> showWhen;

        public ListButtonConfig(TextureSet texture, String text, String textKey,
                                String textColor, String highlightedTextColor, boolean textShadow,
                                int width, int height, Set<String> showWhen) {
            this.texture = texture;
            this.text = text;
            this.textKey = textKey;
            this.textColor = textColor;
            this.highlightedTextColor = highlightedTextColor;
            this.textShadow = textShadow;
            this.width = width;
            this.height = height;
            this.showWhen = showWhen;
        }

        public boolean shouldShow(boolean isHovered, boolean isSelected) {
            if (showWhen.contains("always")) return true;
            if (showWhen.contains("hovered") && isHovered) return true;
            if (showWhen.contains("selected") && isSelected) return true;
            return false;
        }

        public static final ListButtonConfig DEFAULT_ACTIVATE = new ListButtonConfig(
            null, "+", null, "0xFFFFFFFF", "0xCC55FF55", false,
            16, 16, Set.of("hovered", "selected")
        );
        public static final ListButtonConfig DEFAULT_DEACTIVATE = new ListButtonConfig(
            null, "x", null, "0xFFFFFFFF", "0xCCFF5555", false,
            16, 16, Set.of("hovered", "selected")
        );
        public static final ListButtonConfig DEFAULT_REQUIRED = new ListButtonConfig(
            null, "!", null, "0xCCAAAAAA", "0xCCAAAAAA", false,
            16, 16, Set.of("always")
        );
        public static final ListButtonConfig DEFAULT_MOVE_UP = new ListButtonConfig(
            null, "\u2191", null, "0xCC888888", "0xCCFFFFFF", false,
            16, 8, Set.of("hovered", "selected")
        );
        public static final ListButtonConfig DEFAULT_MOVE_DOWN = new ListButtonConfig(
            null, "\u2193", null, "0xCC888888", "0xCCFFFFFF", false,
            16, 8, Set.of("hovered", "selected")
        );
    }

    public static class ToggleConfig {
        public final int rightPadding;
        public final ListButtonConfig activate;
        public final ListButtonConfig deactivate;
        public final ListButtonConfig required;

        public ToggleConfig(int rightPadding, ListButtonConfig activate,
                            ListButtonConfig deactivate, ListButtonConfig required) {
            this.rightPadding = rightPadding;
            this.activate = activate;
            this.deactivate = deactivate;
            this.required = required;
        }

        public static final ToggleConfig DEFAULT = new ToggleConfig(
            20,
            ListButtonConfig.DEFAULT_ACTIVATE,
            ListButtonConfig.DEFAULT_DEACTIVATE,
            ListButtonConfig.DEFAULT_REQUIRED
        );
    }

    public static class MoveConfig {
        public final int gap;
        public final ListButtonConfig up;
        public final ListButtonConfig down;

        public MoveConfig(int gap, ListButtonConfig up, ListButtonConfig down) {
            this.gap = gap;
            this.up = up;
            this.down = down;
        }

        public static final MoveConfig DEFAULT = new MoveConfig(
            20,
            ListButtonConfig.DEFAULT_MOVE_UP,
            ListButtonConfig.DEFAULT_MOVE_DOWN
        );
    }

    public static class LayoutConfig {
        public final int iconX;
        public final int iconSize;
        public final String iconPlaceholderColor;
        public final int iconTextGap;
        public final int titleY;
        public final int descriptionY;
        public final int compatibilityY;
        public final int titleMaxLength;
        public final int descriptionMaxLength;
        public final String incompatibleBgColor;
        public final String incompatibleTextColor;
        public final String incompatibleDescColor;
        public final String sourceLabelColor;
        public final int sourceLabelRightPadding;
        public final ToggleConfig toggle;
        public final MoveConfig move;
        public final int scrollbarWidth;
        public final String scrollbarColor;
        public final int scrollbarMinHeight;
        public final int scrollbarPadding;
        public final String dragIndicatorColor;
        public final int dragAutoScrollSpeed;
        public final int scrollSpeed;

        public LayoutConfig(
                int iconX, int iconSize, String iconPlaceholderColor,
                int iconTextGap, int titleY, int descriptionY, int compatibilityY,
                int titleMaxLength, int descriptionMaxLength,
                String incompatibleBgColor, String incompatibleTextColor, String incompatibleDescColor,
                String sourceLabelColor, int sourceLabelRightPadding,
                ToggleConfig toggle, MoveConfig move,
                int scrollbarWidth, String scrollbarColor, int scrollbarMinHeight, int scrollbarPadding,
                String dragIndicatorColor, int dragAutoScrollSpeed, int scrollSpeed) {
            this.iconX = iconX;
            this.iconSize = iconSize;
            this.iconPlaceholderColor = iconPlaceholderColor;
            this.iconTextGap = iconTextGap;
            this.titleY = titleY;
            this.descriptionY = descriptionY;
            this.compatibilityY = compatibilityY;
            this.titleMaxLength = titleMaxLength;
            this.descriptionMaxLength = descriptionMaxLength;
            this.incompatibleBgColor = incompatibleBgColor;
            this.incompatibleTextColor = incompatibleTextColor;
            this.incompatibleDescColor = incompatibleDescColor;
            this.sourceLabelColor = sourceLabelColor;
            this.sourceLabelRightPadding = sourceLabelRightPadding;
            this.toggle = toggle;
            this.move = move;
            this.scrollbarWidth = scrollbarWidth;
            this.scrollbarColor = scrollbarColor;
            this.scrollbarMinHeight = scrollbarMinHeight;
            this.scrollbarPadding = scrollbarPadding;
            this.dragIndicatorColor = dragIndicatorColor;
            this.dragAutoScrollSpeed = dragAutoScrollSpeed;
            this.scrollSpeed = scrollSpeed;
        }

        public static final LayoutConfig DEFAULT = new LayoutConfig(
            4, 32, "0xFF555555",
            6, 4, 16, 28,
            30, 40,
            "0x55897843", "0xFFFF5555", "0xCCFF8888",
            "0xCCAAAAAA", 24,
            ToggleConfig.DEFAULT, MoveConfig.DEFAULT,
            4, "0x44FFFFFF", 16, 2,
            "0xCCFFFFFF", 4, 20
        );
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
    private final LayoutConfig layoutConfig;
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
    private int selectedIndex = -1;

    public PackListWidget(String id, LayoutProps layout, Map<String, String> variables,
                          Map<String, String> member, PackDataSource dataSource,
                          String modeStr, ActionExecutor executor,
                          int entryHeight, int entryGap, String backgroundColor,
                          RowStyle activeRowStyle, RowStyle unactiveRowStyle,
                          DividerStyle dividerStyle, LayoutConfig layoutConfig, String orderingStr) {
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
        this.layoutConfig = layoutConfig != null ? layoutConfig : LayoutConfig.DEFAULT;
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

                boolean mouseInRow = mouseX >= listX && mouseX <= listX + dim.w
                    && mouseY >= rowTop && mouseY < rowTop + entryHeight;
                boolean isSelected = (i == selectedIndex);

                renderRowBackground(graphics, listX, rowTop, dim.w, entryHeight, style, entry,
                    mouseInRow, isSelected);

                if (isDragging && i == dragTargetIndex) {
                    graphics.fill(listX, rowTop - 1, listX + dim.w, rowTop + 1, parseColor(layoutConfig.dragIndicatorColor));
                }

                if (isDragging && i == dragIndex) {
                    renderEntry(graphics, listX, rowTop + (int) dragOffsetY, dim.w, entryHeight,
                        entry, style, mouseX, mouseY, i, mouseInRow, isSelected, 0.5f);
                } else {
                    renderEntry(graphics, listX, rowTop, dim.w, entryHeight,
                        entry, style, mouseX, mouseY, i, mouseInRow, isSelected, 1.0f);
                }
            }

            currentY += entryHeight + entryGap;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int scrollbarW = layoutConfig.scrollbarWidth;
            int scrollbarX = listX + dim.w - scrollbarW - layoutConfig.scrollbarPadding;
            int scrollbarH = (int) ((double) dim.h * dim.h / (dim.h + maxScroll));
            scrollbarH = Math.max(layoutConfig.scrollbarMinHeight, Math.min(scrollbarH, dim.h - layoutConfig.scrollbarMinHeight / 2));
            int scrollbarY = listY + (int) (scrollAmount * (dim.h - scrollbarH) / maxScroll);
            graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarW, scrollbarY + scrollbarH, parseColor(layoutConfig.scrollbarColor));
        }
    }

    private void renderRowBackground(GuiGraphics graphics, int listX, int rowTop,
                                      int listW, int rowH, RowStyle style, PackDataSource.PackEntryData entry,
                                      boolean mouseInRow, boolean isSelected) {
        if (style.backgroundTexture != null) {
            ResourceLocation tex = new ResourceLocation(style.backgroundTexture);
            graphics.blit(tex, listX, rowTop, 0, 0, listW, rowH, listW, rowH);
            return;
        }

        String bgColor = style.backgroundColor;
        if (!entry.isCompatible()) {
            bgColor = layoutConfig.incompatibleBgColor;
        }
        if (mouseInRow && !isDragging && style.hoverColor != null) {
            bgColor = style.hoverColor;
        }
        if (isSelected && !isDragging && style.selectedColor != null) {
            bgColor = style.selectedColor;
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
                             int mouseX, int mouseY, int index,
                             boolean mouseInRow, boolean isSelected, float opacity) {
        int iconX = listX + layoutConfig.iconX;
        int iconY = rowTop + (rowH - layoutConfig.iconSize) / 2;
        int iconW = layoutConfig.iconSize;
        int iconH = layoutConfig.iconSize;

        ResourceLocation iconTex = entry.getIconTexture();
        if (iconTex != null) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, opacity);
            graphics.blit(iconTex, iconX, iconY, 0, 0, iconW, iconH, iconW, iconH);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } else {
            int pc = parseColor(layoutConfig.iconPlaceholderColor);
            graphics.fill(iconX, iconY, iconX + iconW, iconY + iconH, pc);
        }

        int textX = iconX + iconW + layoutConfig.iconTextGap;
        int textColor = parseColor(style.textColor);
        if (!entry.isCompatible()) textColor = parseColor(layoutConfig.incompatibleTextColor);

        String title = entry.getTitle();
        if (title.length() > layoutConfig.titleMaxLength)
            title = title.substring(0, layoutConfig.titleMaxLength - 2) + "...";
        int titleColor = (int) ((textColor & 0x00FFFFFF) | ((int) (((textColor >> 24) & 0xFF) * opacity) << 24));
        graphics.drawString(minecraft.font, title, textX, rowTop + layoutConfig.titleY, titleColor);

        String desc = entry.getExtendedDescription();
        if (desc.length() > layoutConfig.descriptionMaxLength)
            desc = desc.substring(0, layoutConfig.descriptionMaxLength - 2) + "...";
        int descColor = parseColor(entry.isActive() ? style.textColor : unactiveRowStyle.textColor);
        descColor = (int) ((descColor & 0x00FFFFFF) | ((int) (((descColor >> 24) & 0xFF) * 0.6f) << 24));
        if (!entry.isCompatible()) descColor = refineColor(parseColor(layoutConfig.incompatibleDescColor), opacity);
        descColor = refineColor(descColor, opacity);
        graphics.drawString(minecraft.font, desc, textX, rowTop + layoutConfig.descriptionY, descColor);

        if (!entry.isCompatible() && entry.getCompatibilityDescription() != null) {
            String compat = entry.getCompatibilityDescription();
            if (compat.length() > layoutConfig.descriptionMaxLength)
                compat = compat.substring(0, layoutConfig.descriptionMaxLength - 2) + "...";
            graphics.drawString(minecraft.font, compat, textX, rowTop + layoutConfig.compatibilityY,
                parseColor(layoutConfig.incompatibleTextColor));
        }

        String sourceLabel = entry.getSourceLabel();
        if (sourceLabel != null && !sourceLabel.isEmpty()) {
            int srcW = minecraft.font.width(sourceLabel);
            graphics.drawString(minecraft.font, sourceLabel,
                listX + listW - srcW - layoutConfig.sourceLabelRightPadding,
                rowTop + layoutConfig.titleY, parseColor(layoutConfig.sourceLabelColor));
        }

        boolean showButtons = mouseInRow && !isDragging;

        ToggleConfig toggle = layoutConfig.toggle;
        int toggleY = rowTop + (rowH - toggle.activate.height) / 2;
        if (entry.canActivate()) {
            renderListButton(graphics, listX, listW, rowTop, rowH, toggleY,
                toggle.activate, toggle.rightPadding, mouseX, mouseY,
                showButtons, isSelected, index, true, false);
        } else if (entry.canDeactivate()) {
            renderListButton(graphics, listX, listW, rowTop, rowH, toggleY,
                toggle.deactivate, toggle.rightPadding, mouseX, mouseY,
                showButtons, isSelected, index, true, false);
        } else if (entry.isRequired()) {
            renderListButton(graphics, listX, listW, rowTop, rowH, toggleY,
                toggle.required, toggle.rightPadding, mouseX, mouseY,
                showButtons, isSelected, index, false, false);
        }

        MoveConfig move = layoutConfig.move;
        if (mode != Mode.UNACTIVE && entry.canMoveUp()) {
            renderListButton(graphics, listX, listW, rowTop, rowH, toggleY,
                move.up, toggle.rightPadding + move.gap,
                mouseX, mouseY, showButtons, isSelected, index, false, true);
        }
        if (mode != Mode.UNACTIVE && entry.canMoveDown()) {
            renderListButton(graphics, listX, listW, rowTop, rowH, toggleY + move.up.height,
                move.down, toggle.rightPadding + move.gap,
                mouseX, mouseY, showButtons, isSelected, index, false, true);
        }
    }

    private void renderListButton(GuiGraphics graphics, int listX, int listW, int rowTop, int rowH,
                                   int baseY,
                                   ListButtonConfig btn, int rightPadding,
                                   int mouseX, int mouseY,
                                   boolean mouseInRow, boolean isSelected,
                                   int index, boolean isToggle, boolean isMove) {
        if (!btn.shouldShow(mouseInRow, isSelected)) return;

        int btnX = listX + listW - rightPadding;
        int btnY = baseY;
        int btnW = btn.width;
        int btnH = btn.height;

        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW
            && mouseY >= btnY && mouseY <= btnY + btnH;

        if (hovered) {
            if (isToggle) hoveredToggleIndex = index;
            if (isMove) {
                if (btn == layoutConfig.move.up) hoveredMoveUpIndex = index;
                if (btn == layoutConfig.move.down) hoveredMoveDownIndex = index;
            }
        }

        String texPath = null;
        if (btn.texture != null) {
            texPath = hovered ? btn.texture.getHighlighted() : btn.texture.getNormal();
            if (texPath == null) texPath = btn.texture.getNormal();
        }

        if (texPath != null && !texPath.isEmpty()) {
            ResourceLocation tex = new ResourceLocation(texPath);
            RenderSystem.enableBlend();
            graphics.blit(tex, btnX, btnY, 0, 0, btnW, btnH, btnW, btnH);
            RenderSystem.disableBlend();
        } else {
            String displayText = null;
            if (btn.textKey != null && !btn.textKey.isEmpty()) {
                displayText = Component.translatable(btn.textKey).getString();
            } else if (btn.text != null && !btn.text.isEmpty()) {
                displayText = btn.text;
            }

            if (displayText != null && !displayText.isEmpty()) {
                String colorStr = hovered && btn.highlightedTextColor != null
                    ? btn.highlightedTextColor : btn.textColor;
                int color = parseColor(colorStr);
                int textW = minecraft.font.width(displayText);
                int textX = btnX + (btnW - textW) / 2;
                int textY = btnY + (btnH - minecraft.font.lineHeight) / 2;
                if (btn.textShadow) {
                    graphics.drawString(minecraft.font, displayText, textX, textY, color);
                } else {
                    graphics.drawString(minecraft.font, displayText, textX, textY, color);
                }
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
            selectedIndex = -1;
            return false;
        }

        if (hoveredToggleIndex >= 0 && hoveredToggleIndex < entries.size()) {
            PackDataSource.PackEntryData entry = entries.get(hoveredToggleIndex);
            if (entry.canActivate() || entry.canDeactivate()) {
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
            int adjustedY = (int) mouseY + (int) scrollAmount - (y + dim.y);
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

        int adjustedY = (int) mouseY + (int) scrollAmount - (y + dim.y);
        int entryIndex = adjustedY / (entryHeight + entryGap);
        if (entryIndex >= 0 && entryIndex < entries.size()) {
            if (selectedIndex == entryIndex) {
                selectedIndex = -1;
            } else {
                selectedIndex = entryIndex;
            }
            return true;
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
                scrollAmount = Math.max(0, scrollAmount - layoutConfig.dragAutoScrollSpeed);
            } else if (mouseY > listY + dim.h) {
                int maxScroll = Math.max(0, getTotalContentHeight() - dim.h);
                scrollAmount = Math.min(maxScroll, scrollAmount + layoutConfig.dragAutoScrollSpeed);
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
        scrollAmount = Math.max(0, Math.min(scrollAmount - scrollY * layoutConfig.scrollSpeed, maxScroll));
        return true;
    }
}