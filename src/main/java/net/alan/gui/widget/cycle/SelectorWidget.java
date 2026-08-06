package net.alan.gui.widget.cycle;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.TextProps;
import net.alan.gui.data.widget.TextureSet;
import net.alan.gui.render.screen.BackgroundRenderer;
import net.alan.gui.render.screen.OptionBinder;
import net.alan.gui.widget.BaseWidget;
import net.alan.gui.widget.TextWidget;
import net.alan.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SelectorWidget extends BaseWidget {
    private final String optionKey;
    private final List<SegmentDef> segments;
    private final TextureSet containerTexture;
    private final TextProps textProps;
    private final List<Widget> children;
    private final String stateKey;
    private final int backgroundColor;
    private final int selectedColor;
    private final int highlightedColor;
    private int currentIndex = 0;
    private int hoveredIndex = -1;
    private final Minecraft minecraft;

    public record SegmentDef(
            String key,
            String textKey,
            TextureSet texture,
            int width
    ) {
        public CycleValue toCycleValue() {
            return new CycleValue(key, textKey);
        }
    }

    public SelectorWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                          String optionKey, List<SegmentDef> segments, TextureSet containerTexture,
                          TextProps textProps, List<Widget> children) {
        this(id, layout, variables, member, optionKey, segments, containerTexture, textProps, children,
                null, 0xFF3A3A3C, 0xFF4A6FA5, 0xFF505052);
    }

    public SelectorWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                          String optionKey, List<SegmentDef> segments, TextureSet containerTexture,
                          TextProps textProps, List<Widget> children, String stateKey) {
        this(id, layout, variables, member, optionKey, segments, containerTexture, textProps, children,
                stateKey, 0xFF3A3A3C, 0xFF4A6FA5, 0xFF505052);
    }

    public SelectorWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                          String optionKey, List<SegmentDef> segments, TextureSet containerTexture,
                          TextProps textProps, List<Widget> children, String stateKey,
                          int backgroundColor, int selectedColor, int highlightedColor) {
        super(id, layout, variables, member);
        this.optionKey = optionKey;
        this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
        this.containerTexture = containerTexture;
        this.textProps = textProps;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();
        this.stateKey = stateKey;
        this.backgroundColor = backgroundColor;
        this.selectedColor = selectedColor;
        this.highlightedColor = highlightedColor;

        if (!this.segments.isEmpty()) {
            if (optionKey != null) {
                List<CycleValue> values = this.segments.stream().map(SegmentDef::toCycleValue).toList();
                int idx = OptionBinder.getCycleOptionIndex(optionKey, values, minecraft.options);
                this.currentIndex = Mth.clamp(idx, 0, this.segments.size() - 1);
            } else if (this.member.containsKey("current_index")) {
                try {
                    int idx = Integer.parseInt(this.member.get("current_index"));
                    this.currentIndex = Mth.clamp(idx, 0, this.segments.size() - 1);
                } catch (NumberFormatException ignored) {
                    this.currentIndex = 0;
                }
            } else {
                this.currentIndex = 0;
            }
            this.member.put("current_index", String.valueOf(currentIndex));
            this.member.put("current_value", this.segments.get(currentIndex).textKey());
            this.member.put("current_key", this.segments.get(currentIndex).key());
        }

        if (textProps != null && hasTextContent(textProps)) {
            String xPos = textProps.offsetX() != null && !textProps.offsetX().equals("0")
                    ? textProps.offsetX() : "4";
            String yPos = textProps.offsetY() != null && !textProps.offsetY().equals("0")
                    ? textProps.offsetY() : "parent.height / 2 - this.height / 2";
            this.children.add(0, new TextWidget(id + "_label",
                    new LayoutProps(xPos, yPos, "auto", "auto", true, true),
                    null, null, textProps));
        }
    }

    private static boolean hasTextContent(TextProps tp) {
        if (tp.text() != null && !tp.text().isEmpty() && !tp.text().trim().isEmpty()) return true;
        if (tp.textKey() != null && !tp.textKey().isEmpty()) return true;
        if (tp.textKeyDynamic() != null && !tp.textKeyDynamic().isEmpty()) return true;
        if (tp.textKeyOption() != null && !tp.textKeyOption().isEmpty()) return true;
        return false;
    }

    private int calcLabelWidth() {
        if (textProps == null || textProps.textKey() == null) return 0;
        Component labelComp = Component.translatable(textProps.textKey());
        return minecraft.font.width(labelComp) + 8;
    }

    private void selectIndex(int index, RenderContext ctx) {
        if (index < 0 || index >= segments.size()) return;
        if (index == currentIndex) return;
        currentIndex = index;
        String key = segments.get(currentIndex).key();
        this.member.put("current_index", String.valueOf(currentIndex));
        this.member.put("current_value", segments.get(currentIndex).textKey());
        this.member.put("current_key", key);
        if (stateKey != null && ctx != null && ctx.sharedState() != null) {
            ctx.sharedState().put(stateKey, key);
        }
        syncToOptions();
        playValueChangeSound();
    }

    private void syncToOptions() {
        if (optionKey != null && !segments.isEmpty()) {
            SegmentDef seg = segments.get(currentIndex);
            if ("default".equalsIgnoreCase(seg.key())) {
                OptionBinder.resetOptionToDefault(optionKey, minecraft.options);
            } else {
                OptionBinder.setCycleOptionValue(optionKey, seg.key(), minecraft.options);
            }
            OptionBinder.saveOptions(minecraft.options);
        }
    }

    @Override
    public List<Widget> getChildren() {
        return children;
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

        if (segments.isEmpty()) return;

        int labelWidth = calcLabelWidth();

        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
        }

        int segAreaX = screenX + labelWidth;
        int segAreaW = dim.w - labelWidth;
        if (segAreaW <= 0) return;

        int[] widths = calcSegmentWidths(segAreaW);

        int curX = segAreaX;
        for (int i = 0; i < segments.size(); i++) {
            int segW = widths[i];
            SegmentDef seg = segments.get(i);
            boolean selected = (i == currentIndex);
            boolean hovered = (i == hoveredIndex);

            renderSegment(graphics, curX, screenY, segW, dim.h, seg, selected, hovered);

            Component text = Component.translatable(seg.textKey());
            int textW = minecraft.font.width(text);
            int textX = curX + (segW - textW) / 2;
            int textY = screenY + (dim.h - 8) / 2;
            int textColor = textProps != null ? BackgroundRenderer.parseColor(textProps.color()) : 0xFFFFFFFF;
            graphics.drawString(minecraft.font, text, textX, textY, textColor);

            curX += segW;
        }
    }

    private int[] calcSegmentWidths(int totalWidth) {
        int[] widths = new int[segments.size()];
        int fixedTotal = 0;
        int autoCount = 0;

        for (int i = 0; i < segments.size(); i++) {
            int w = segments.get(i).width();
            if (w > 0) {
                widths[i] = w;
                fixedTotal += w;
            } else {
                autoCount++;
            }
        }

        int remaining = totalWidth - fixedTotal;
        if (autoCount > 0 && remaining > 0) {
            int autoW = remaining / autoCount;
            for (int i = 0; i < segments.size(); i++) {
                if (widths[i] == 0) {
                    widths[i] = autoW;
                }
            }
        } else if (autoCount > 0) {
            for (int i = 0; i < segments.size(); i++) {
                if (widths[i] == 0) {
                    widths[i] = 1;
                }
            }
        }

        return widths;
    }

    private void renderSegment(GuiGraphics graphics, int x, int y, int w, int h,
                               SegmentDef seg, boolean selected, boolean hovered) {
        int bgColor;
        if (selected) {
            bgColor = selectedColor;
        } else if (hovered) {
            bgColor = highlightedColor;
        } else {
            bgColor = backgroundColor;
        }

        TextureSet tex = seg.texture();
        if (tex == null) {
            tex = containerTexture;
        }

        String texPath = null;
        if (tex != null) {
            if (selected) {
                texPath = tex.getSelected() != null ? tex.getSelected() : tex.getHighlighted();
            } else if (hovered) {
                texPath = tex.getHighlighted();
            }
            if (texPath == null || texPath.isEmpty()) {
                texPath = tex.getNormal();
            }
        }

        if (texPath != null && !texPath.isEmpty()) {
            var id = ResourceLocation.tryParse(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, x, y, w, h);
            }
        } else {
            graphics.fill(x, y, x + w, y + h, bgColor);
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

        if (segments.isEmpty()) return false;

        int labelWidth = calcLabelWidth();
        int segAreaX = screenX + labelWidth;
        int segAreaW = dim.w - labelWidth;
        if (segAreaW <= 0) return false;

        int clickedIdx = segmentIndexAt(mouseX, mouseY, segAreaX, segAreaW, screenY, dim.h);
        if (clickedIdx >= 0 && clickedIdx != currentIndex) {
            playClickSound();
            selectIndex(clickedIdx, mergedCtx);
            return true;
        }

        return false;
    }

    private int segmentIndexAt(double mouseX, double mouseY, int areaX, int areaW, int areaY, int areaH) {
        if (mouseX < areaX || mouseX > areaX + areaW || mouseY < areaY || mouseY > areaY + areaH) {
            return -1;
        }

        int[] widths = calcSegmentWidths(areaW);
        int curX = areaX;
        for (int i = 0; i < widths.length; i++) {
            if (mouseX >= curX && mouseX < curX + widths[i]) {
                return i;
            }
            curX += widths[i];
        }
        return -1;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY,
                           RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (segments.isEmpty()) {
            hoveredIndex = -1;
            return;
        }

        int labelWidth = calcLabelWidth();
        int segAreaX = screenX + labelWidth;
        int segAreaW = dim.w - labelWidth;

        int oldHoveredIndex = hoveredIndex;
        hoveredIndex = segmentIndexAt(mouseX, mouseY, segAreaX, segAreaW, screenY, dim.h);
        if (oldHoveredIndex == -1 && hoveredIndex != -1) {
            playHoverSound();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        return false;
    }
}