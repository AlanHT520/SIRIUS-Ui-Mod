package net.alan.gui.widget.cycle;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.data.widget.TextProps;
import net.alan.gui.data.widget.TextureSet;
import net.alan.gui.render.screen.BackgroundRenderer;
import net.alan.gui.render.screen.OptionBinder;
import net.alan.gui.util.NineSliceHelper;
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

public class ArrowSwitchWidget extends BaseWidget {
    private final String optionKey;
    private final List<CycleValue> values;
    private final TextureSet leftButtonTexture;
    private final TextureSet rightButtonTexture;
    private final TextureSet centerTexture;
    private final TextProps textProps;
    private final List<Widget> children;
    private final String stateKey;
    private final int backgroundColor;
    private final int highlightedBackgroundColor;
    private final int arrowColor;
    private int currentIndex = 0;
    private int hoveredPart = -1;
    private final String buttonWidthExpr;
    private final Minecraft minecraft;

    public ArrowSwitchWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                            String optionKey, List<CycleValue> values,
                            TextureSet leftButtonTexture, TextureSet rightButtonTexture, TextureSet centerTexture,
                            TextProps textProps, List<Widget> children) {
        this(id, layout, variables, member, optionKey, values, leftButtonTexture, rightButtonTexture, centerTexture,
                textProps, children, null, 0xFF3A3A3C, 0xFF505052, 0xFFFFFFFF, null);
    }

    public ArrowSwitchWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                            String optionKey, List<CycleValue> values,
                            TextureSet leftButtonTexture, TextureSet rightButtonTexture, TextureSet centerTexture,
                            TextProps textProps, List<Widget> children, String stateKey) {
        this(id, layout, variables, member, optionKey, values, leftButtonTexture, rightButtonTexture, centerTexture,
                textProps, children, stateKey, 0xFF3A3A3C, 0xFF505052, 0xFFFFFFFF, null);
    }

    public ArrowSwitchWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                            String optionKey, List<CycleValue> values,
                            TextureSet leftButtonTexture, TextureSet rightButtonTexture, TextureSet centerTexture,
                            TextProps textProps, List<Widget> children, String stateKey,
                            int backgroundColor, int highlightedBackgroundColor, int arrowColor,
                            String buttonWidthExpr) {
        super(id, layout, variables, member);
        this.optionKey = optionKey;
        this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
        this.leftButtonTexture = leftButtonTexture;
        this.rightButtonTexture = rightButtonTexture;
        this.centerTexture = centerTexture;
        this.textProps = textProps;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();
        this.stateKey = stateKey;
        this.backgroundColor = backgroundColor;
        this.highlightedBackgroundColor = highlightedBackgroundColor;
        this.arrowColor = arrowColor;
        this.buttonWidthExpr = buttonWidthExpr;

        if (!this.values.isEmpty()) {
            if (optionKey != null) {
                int idx = OptionBinder.getCycleOptionIndex(optionKey, this.values, minecraft.options);
                this.currentIndex = Mth.clamp(idx, 0, this.values.size() - 1);
            } else if (this.member.containsKey("current_index")) {
                try {
                    int idx = Integer.parseInt(this.member.get("current_index"));
                    this.currentIndex = Mth.clamp(idx, 0, this.values.size() - 1);
                } catch (NumberFormatException ignored) {
                    this.currentIndex = 0;
                }
            } else {
                this.currentIndex = 0;
            }
            this.member.put("current_value", getCurrentDisplayText());
            this.member.put("current_index", String.valueOf(currentIndex));
            this.member.put("current_key", this.values.get(currentIndex).key());
        }
    }

    private String getCurrentDisplayText() {
        if (values.isEmpty()) return "?";
        CycleValue cv = values.get(currentIndex);
        return cv.textKey();
    }

    private void cycleValue(int delta, RenderContext ctx) {
        if (values.isEmpty()) return;
        int size = values.size();
        currentIndex = (currentIndex + delta + size) % size;
        String key = values.get(currentIndex).key();
        this.member.put("current_value", getCurrentDisplayText());
        this.member.put("current_index", String.valueOf(currentIndex));
        this.member.put("current_key", key);
        if (stateKey != null && ctx != null && ctx.sharedState() != null) {
            ctx.sharedState().put(stateKey, key);
        }
        syncToOptions();
        playValueChangeSound();
    }

    private void syncToOptions() {
        if (optionKey != null && !values.isEmpty()) {
            OptionBinder.setCycleOptionValue(optionKey, values.get(currentIndex).key(), minecraft.options);
        }
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

        int labelWidth = calcLabelWidth();
        int buttonAreaW = dim.w - labelWidth;
        if (buttonAreaW <= 0) buttonAreaW = dim.w;

        int buttonW = getButtonWidth(buttonAreaW, dim.h);
        int centerW = buttonAreaW - buttonW * 2;

        if (textProps != null && textProps.textKey() != null) {
            Component label = Component.translatable(textProps.textKey());
            int textW = minecraft.font.width(label);
            int textX = screenX;
            int textY = screenY + (dim.h - 8) / 2;
            int labelColor = textProps != null ? BackgroundRenderer.parseColor(textProps.color()) : arrowColor;
            graphics.drawString(minecraft.font, label, textX, textY, labelColor);
        }

        int controlAreaX = screenX + labelWidth;

        renderButton(graphics, controlAreaX, screenY, buttonW, dim.h, leftButtonTexture, 0);
        renderButton(graphics, controlAreaX + buttonW, screenY, centerW, dim.h, centerTexture, 1);
        renderButton(graphics, controlAreaX + buttonW + centerW, screenY, buttonW, dim.h, rightButtonTexture, 2);

        if (!values.isEmpty()) {
            CycleValue cv = values.get(currentIndex);
            Component text = Component.translatable(cv.textKey());
            int textW = minecraft.font.width(text);
            int textX = controlAreaX + buttonW + (centerW - textW) / 2;
            int textY = screenY + (dim.h - 8) / 2;
            int textColor = textProps != null ? BackgroundRenderer.parseColor(textProps.color()) : 0xFFFFFFFF;
            graphics.drawString(minecraft.font, text, textX, textY, textColor);
        }

        RenderContext childCtx = mergedCtx;
        childCtx = childCtx.withVar("current_value", getCurrentDisplayText());
        childCtx = childCtx.withVar("current_index", String.valueOf(currentIndex));
        childCtx = childCtx.withVar("current_key", values.isEmpty() ? "" : values.get(currentIndex).key());
        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, childCtx, mouseX, mouseY, delta);
        }
    }

    private int calcLabelWidth() {
        if (textProps == null || textProps.textKey() == null) return 0;
        Component labelComp = Component.translatable(textProps.textKey());
        return minecraft.font.width(labelComp) + 8;
    }

    private void renderButton(GuiGraphics graphics, int x, int y, int w, int h, TextureSet tex, int partIndex) {
        String texPath = getTexturePath(tex, partIndex == hoveredPart);
        if (texPath != null && !texPath.isEmpty()) {
            var id = parseTexturePath(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                NineSliceHelper.NineSliceInfo nineSlice = NineSliceHelper.loadNineSlice(id);
                if (nineSlice != null) {
                    NineSliceHelper.blitNineSliced(graphics, id, x, y, w, h, nineSlice);
                } else {
                    graphics.blit(id, x, y, 0, 0, w, h, w, h);
                }
            }
        } else {
            int bgColor;
            if (partIndex == hoveredPart) {
                bgColor = highlightedBackgroundColor;
            } else {
                bgColor = backgroundColor;
            }
            graphics.fill(x, y, x + w, y + h, bgColor);
        }

        if (w > 10 && h > 10 && (partIndex == 0 || partIndex == 2)) {
            String arrow = partIndex == 0 ? "<" : ">";
            int textWidth = this.minecraft.font.width(arrow);
            int textX = x + (w - textWidth) / 2;
            int textY = y + (h - 8) / 2;
            graphics.drawString(this.minecraft.font, arrow, textX, textY, arrowColor);
        }
    }

    private String getTexturePath(TextureSet tex, boolean hovered) {
        if (tex == null) return null;
        if (!layout.enabled()) {
            String t = tex.getDisabled();
            return t != null ? t : tex.getNormal();
        }
        if (hovered) {
            String t = tex.getHighlighted();
            return t != null ? t : tex.getNormal();
        }
        return tex.getNormal();
    }

    private int getButtonWidth(int totalAreaW, int height) {
        if (totalAreaW <= 0) return 0;
        if (buttonWidthExpr != null && !buttonWidthExpr.isEmpty()) {
            Map<String, Integer> vars = new java.util.HashMap<>();
            vars.put("parent.width", totalAreaW);
            vars.put("parent.height", height);
            return eval(buttonWidthExpr, vars);
        }
        int buttonW = totalAreaW / 6;
        if (buttonW < 10) buttonW = 10;
        if (buttonW > 40) buttonW = 40;
        return buttonW;
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

        int labelWidth = calcLabelWidth();
        int buttonAreaW = dim.w - labelWidth;
        if (buttonAreaW <= 0) buttonAreaW = dim.w;
        int buttonW = getButtonWidth(buttonAreaW, dim.h);
        int centerW = buttonAreaW - buttonW * 2;
        int controlAreaX = screenX + labelWidth;

        if (mouseY >= screenY && mouseY <= screenY + dim.h) {
            if (mouseX >= controlAreaX && mouseX <= controlAreaX + buttonW) {
                playClickSound();
                cycleValue(-1, context);
                return true;
            }
            if (mouseX >= controlAreaX + buttonW + centerW && mouseX <= controlAreaX + buttonW + centerW + buttonW) {
                playClickSound();
                cycleValue(1, context);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY, RenderContext context,
                             int x, int y, int width, int height) {
        WidgetDimension dim = computeLayout(mergeContext(context), width, height);
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        int labelWidth = calcLabelWidth();
        int buttonAreaW = dim.w - labelWidth;
        if (buttonAreaW <= 0) buttonAreaW = dim.w;
        int buttonW = getButtonWidth(buttonAreaW, dim.h);
        int centerW = buttonAreaW - buttonW * 2;
        int controlAreaX = screenX + labelWidth;

        int oldHoveredPart = hoveredPart;
        if (mouseY >= screenY && mouseY <= screenY + dim.h) {
            if (mouseX >= controlAreaX && mouseX <= controlAreaX + buttonW) {
                hoveredPart = 0;
            } else if (mouseX >= controlAreaX + buttonW + centerW && mouseX <= controlAreaX + buttonW + centerW + buttonW) {
                hoveredPart = 2;
            } else {
                hoveredPart = -1;
            }
        } else {
            hoveredPart = -1;
        }
        if (oldHoveredPart == -1 && hoveredPart != -1) {
            playHoverSound();
        }
    }
}