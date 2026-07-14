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

public class ArrowSwitchWidget extends BaseWidget {
    private final String optionKey;
    private final List<CycleValue> values;
    private final TextureSet leftButtonTexture;
    private final TextureSet rightButtonTexture;
    private final TextureSet centerTexture;
    private final TextProps textProps;
    private final List<Widget> children;
    private int currentIndex = 0;
    private int hoveredPart = -1; // -1: none, 0: left, 1: center, 2: right
    private final Minecraft minecraft;

    public ArrowSwitchWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                            String optionKey, List<CycleValue> values,
                            TextureSet leftButtonTexture, TextureSet rightButtonTexture, TextureSet centerTexture,
                            TextProps textProps, List<Widget> children) {
        super(id, layout, variables, member);
        this.optionKey = optionKey;
        this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
        this.leftButtonTexture = leftButtonTexture;
        this.rightButtonTexture = rightButtonTexture;
        this.centerTexture = centerTexture;
        this.textProps = textProps;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();

        if (optionKey != null && !this.values.isEmpty()) {
            int idx = OptionBinder.getCycleOptionIndex(optionKey, values, minecraft.options);
            this.currentIndex = Mth.clamp(idx, 0, this.values.size() - 1);
            this.member.put("current_value", getCurrentDisplayText());
            this.member.put("current_index", String.valueOf(currentIndex));
        }
        // 不在构造器里添加 TextWidget，直接在 render 中绘制，避免重复
    }

    private String getCurrentDisplayText() {
        if (values == null || values.isEmpty()) return "?";
        CycleValue cv = values.get(currentIndex);
        return cv.textKey();
    }

    private void cycleValue(int delta) {
        if (values == null || values.isEmpty()) return;
        int size = values.size();
        currentIndex = Mth.positiveModulo(currentIndex + delta, size);
        this.member.put("current_value", getCurrentDisplayText());
        this.member.put("current_index", String.valueOf(currentIndex));
        syncToOptions();
    }

    private void syncToOptions() {
        if (optionKey != null && !values.isEmpty()) {
            CycleValue cv = values.get(currentIndex);
            if ("default".equalsIgnoreCase(cv.key())) {
                OptionBinder.resetOptionToDefault(optionKey, minecraft.options);
            } else {
                OptionBinder.setCycleOptionValue(optionKey, cv.key(), minecraft.options);
            }
            OptionBinder.saveOptions(minecraft.options);
        }
    }

    @Override
    public List<Widget> getChildren() { return children; }

    private int getButtonWidth(int totalWidth) {
        return Math.min(20, totalWidth / 4);
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

        int buttonW = getButtonWidth(buttonAreaW);
        int centerW = buttonAreaW - buttonW * 2;

        // 如果有标签文本，在左侧显示
        if (textProps != null && textProps.textKey() != null) {
            Component label = Component.translatable(textProps.textKey());
            int textW = minecraft.font.width(label);
            int textX = screenX;
            int textY = screenY + (dim.h - 8) / 2;
            graphics.drawString(minecraft.font, label, textX, textY, 0xFFFFFFFF);
        }

        int controlAreaX = screenX + labelWidth;

        // 渲染左键
        renderButton(graphics, controlAreaX, screenY, buttonW, dim.h, leftButtonTexture, 0);
        // 渲染中间显示区
        renderButton(graphics, controlAreaX + buttonW, screenY, centerW, dim.h, centerTexture, 1);
        // 渲染右键
        renderButton(graphics, controlAreaX + buttonW + centerW, screenY, buttonW, dim.h, rightButtonTexture, 2);

        // 绘制当前值文本
        if (!values.isEmpty()) {
            CycleValue cv = values.get(currentIndex);
            Component text = Component.translatable(cv.textKey());
            int textW = minecraft.font.width(text);
            int textX = controlAreaX + buttonW + (centerW - textW) / 2;
            int textY = screenY + (dim.h - 8) / 2;
            graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF);
        }

        // 渲染子元素
        RenderContext childCtx = mergedCtx;
        if (optionKey != null) {
            childCtx = mergedCtx.withVar("current_value", getCurrentDisplayText());
            childCtx = childCtx.withVar("current_index", String.valueOf(currentIndex));
        }
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
            var id = ResourceLocation.tryParse(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, x, y, w, h);
            }
        } else {
            int bgColor;
            if (partIndex == 1) {
                bgColor = 0xFF3A3A3C;
            } else if (partIndex == hoveredPart) {
                bgColor = 0xFF505052;
            } else {
                bgColor = 0xFF3A3A3C;
            }
            graphics.fill(x, y, x + w, y + h, bgColor);
        }

        // 绘制箭头指示符
        if (w > 10 && h > 10 && (partIndex == 0 || partIndex == 2)) {
            String arrow = partIndex == 0 ? "<" : ">";
            int textWidth = this.minecraft.font.width(arrow);
            int textX = x + (w - textWidth) / 2;
            int textY = y + (h - 8) / 2;
            graphics.drawString(this.minecraft.font, arrow, textX, textY, 0xFFFFFFFF);
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

    private int getPartAt(double mouseX, double mouseY, int screenX, int screenY, int width, int height) {
        if (mouseX < screenX || mouseX >= screenX + width || mouseY < screenY || mouseY >= screenY + height) {
            return -1;
        }
        int buttonW = getButtonWidth(width);
        if (mouseX < screenX + buttonW) return 0;
        if (mouseX >= screenX + width - buttonW) return 2;
        return 1;
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
        int controlAreaX = screenX + labelWidth;

        // 先让子元素响应
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }

        int part = getPartAt(mouseX, mouseY, controlAreaX, screenY, buttonAreaW, dim.h);
        if (part >= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (part == 0) {
                cycleValue(-1);
            } else if (part == 2) {
                cycleValue(1);
            }
            return true;
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
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        int labelWidth = calcLabelWidth();
        int buttonAreaW = dim.w - labelWidth;
        if (buttonAreaW <= 0) buttonAreaW = dim.w;
        int controlAreaX = screenX + labelWidth;

        hoveredPart = getPartAt(mouseX, mouseY, controlAreaX, screenY, buttonAreaW, dim.h);

        for (Widget child : children) {
            child.mouseMoved(mouseX, mouseY, mergedCtx, screenX, screenY, dim.w, dim.h);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        boolean consumed = false;
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                consumed = true;
            }
        }
        return consumed;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }
        return false;
    }
}