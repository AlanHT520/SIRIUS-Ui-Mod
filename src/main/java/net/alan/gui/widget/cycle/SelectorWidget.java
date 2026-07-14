package net.alan.gui.widget.cycle;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.TextProps;
import net.alan.gui.data.style.TextureSet;
import net.alan.gui.render.OptionBinder;
import net.alan.gui.widget.BaseWidget;
import net.alan.gui.widget.TextWidget;
import net.alan.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
        super(id, layout, variables, member);
        this.optionKey = optionKey;
        this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
        this.containerTexture = containerTexture;
        this.textProps = textProps;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();

        if (optionKey != null && !this.segments.isEmpty()) {
            List<CycleValue> values = this.segments.stream().map(SegmentDef::toCycleValue).toList();
            int idx = OptionBinder.getCycleOptionIndex(optionKey, values, minecraft.options);
            this.currentIndex = Mth.clamp(idx, 0, this.segments.size() - 1);
            this.member.put("current_index", String.valueOf(currentIndex));
        }

        if (textProps != null) {
            String xPos = textProps.offsetX() != null ? textProps.offsetX() : "4";
            String yPos = textProps.offsetY() != null ? textProps.offsetY() : "parent.height / 2 - this.height / 2";
            this.children.add(0, new TextWidget(id + "_label",
                    new LayoutProps(xPos, yPos, "auto", "auto", true, true),
                    null, null, textProps));
        }
    }

    private int calcLabelWidth() {
        if (textProps == null || textProps.textKey() == null) return 0;
        Component labelComp = Component.translatable(textProps.textKey());
        return minecraft.font.width(labelComp) + 8;
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= segments.size()) return;
        if (index == currentIndex) return;
        currentIndex = index;
        this.member.put("current_index", String.valueOf(currentIndex));
        syncToOptions();
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
            graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF);

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
            bgColor = 0xFF4A6FA5;
        } else if (hovered) {
            bgColor = 0xFF505052;
        } else {
            bgColor = 0xFF3A3A3C;
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
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectIndex(clickedIdx);
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

        hoveredIndex = segmentIndexAt(mouseX, mouseY, segAreaX, segAreaW, screenY, dim.h);
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