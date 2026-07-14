/*
* 注意，这个功能还没有实现，后续会添加
*
*
* */

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

public class DropdownWidget extends BaseWidget {
    private final String optionKey;
    private final List<CycleValue> values;
    private final TextureSet buttonTexture;
    private final TextureSet dropdownTexture;
    private final TextureSet itemNormalTexture;
    private final TextureSet itemHighlightedTexture;
    private final TextProps textProps;
    private final List<Widget> children;
    private int currentIndex = 0;
    private int hoveredIndex = -1;
    private boolean expanded = false;
    private final Minecraft minecraft;

    public DropdownWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                          String optionKey, List<CycleValue> values,
                          TextureSet buttonTexture, TextureSet dropdownTexture,
                          TextureSet itemNormalTexture, TextureSet itemHighlightedTexture,
                          TextProps textProps, List<Widget> children) {
        super(id, layout, variables, member);
        this.optionKey = optionKey;
        this.values = values != null ? new ArrayList<>(values) : new ArrayList<>();
        this.buttonTexture = buttonTexture;
        this.dropdownTexture = dropdownTexture;
        this.itemNormalTexture = itemNormalTexture;
        this.itemHighlightedTexture = itemHighlightedTexture;
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

    private void toggleExpanded() {
        expanded = !expanded;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void selectIndex(int index) {
        if (index < 0 || index >= values.size()) return;
        if (index == currentIndex && expanded) {
            expanded = false;
            return;
        }
        currentIndex = index;
        this.member.put("current_value", getCurrentDisplayText());
        this.member.put("current_index", String.valueOf(currentIndex));
        expanded = false;
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

    private int getItemHeight() {
        return 20;
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

        if (textProps != null && textProps.textKey() != null) {
            Component label = Component.translatable(textProps.textKey());
            int textW = minecraft.font.width(label);
            int textX = screenX;
            int textY = screenY + (dim.h - 8) / 2;
            graphics.drawString(minecraft.font, label, textX, textY, 0xFFFFFFFF);
        }

        int controlAreaX = screenX + labelWidth;
        int controlAreaW = buttonAreaW;

        renderMainButton(graphics, controlAreaX, screenY, controlAreaW, height);

        if (!values.isEmpty()) {
            CycleValue cv = values.get(currentIndex);
            Component text = Component.translatable(cv.textKey());
            int textW = minecraft.font.width(text);
            int textX = controlAreaX + (controlAreaW - 16 - textW) / 2;
            int textY = screenY + (height - 8) / 2;
            graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF);
        }

        int arrowX = controlAreaX + controlAreaW - 12;
        int arrowY = screenY + height / 2 - 3;
        int color = 0xFFFFFFFF;
        if (expanded) {
            graphics.fill(arrowX, arrowY + 3, arrowX + 2, arrowY + 7, color);
            graphics.fill(arrowX + 3, arrowY + 1, arrowX + 5, arrowY + 3, color);
            graphics.fill(arrowX + 6, arrowY + 3, arrowX + 8, arrowY + 7, color);
        } else {
            graphics.fill(arrowX, arrowY + 1, arrowX + 2, arrowY + 5, color);
            graphics.fill(arrowX + 3, arrowY + 4, arrowX + 5, arrowY + 6, color);
            graphics.fill(arrowX + 6, arrowY + 1, arrowX + 8, arrowY + 5, color);
        }

        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
        }
    }

    private int calcLabelWidth() {
        if (textProps == null || textProps.textKey() == null) return 0;
        Component labelComp = Component.translatable(textProps.textKey());
        return minecraft.font.width(labelComp) + 8;
    }

    @Override
    public boolean hasOverlay() {
        return expanded && !values.isEmpty();
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int x, int y, int width, int height,
                              RenderContext context, int mouseX, int mouseY, float delta) {
        if (!expanded || values.isEmpty()) return;

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

        int dropdownH = values.size() * getItemHeight() + 4;
        int dropdownY = screenY + height + 2;

        renderDropdownBackground(graphics, controlAreaX, dropdownY, buttonAreaW, dropdownH);

        for (int i = 0; i < values.size(); i++) {
            int itemY = dropdownY + 2 + i * getItemHeight();
            boolean hovered = mouseX >= controlAreaX && mouseX <= controlAreaX + buttonAreaW &&
                             mouseY >= itemY && mouseY <= itemY + getItemHeight();
            renderDropdownItem(graphics, controlAreaX, itemY, buttonAreaW, getItemHeight(), values.get(i), hovered);
        }
    }

    private void renderMainButton(GuiGraphics graphics, int x, int y, int w, int h) {
        String texPath = getTexturePath(buttonTexture, false);
        if (texPath != null && !texPath.isEmpty()) {
            var id = ResourceLocation.tryParse(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, x, y, w, h);
            }
        } else {
            int bgColor = 0xFF3A3A3C;
            graphics.fill(x, y, x + w, y + h, bgColor);
        }
    }

    private void renderDropdownBackground(GuiGraphics graphics, int x, int y, int w, int h) {
        if (dropdownTexture != null) {
            String texPath = dropdownTexture.getNormal();
            if (texPath != null && !texPath.isEmpty()) {
                var id = ResourceLocation.tryParse(texPath);
                if (id != null) {
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.blitSprite(id, x, y, w, h);
                }
            }
        } else {
            graphics.fill(x, y, x + w, y + h, 0xFF2A2A2C);
        }
    }

    private void renderDropdownItem(GuiGraphics graphics, int x, int y, int w, int h, CycleValue item, boolean hovered) {
        TextureSet tex = hovered ? itemHighlightedTexture : itemNormalTexture;
        if (tex != null) {
            String texPath = hovered ? tex.getHighlighted() : tex.getNormal();
            if (texPath != null && !texPath.isEmpty()) {
                var id = ResourceLocation.tryParse(texPath);
                if (id != null) {
                    graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    graphics.blitSprite(id, x + 2, y, w - 4, h);
                }
            }
        } else {
            int bgColor = hovered ? 0xFF505052 : 0xFF2A2A2C;
            graphics.fill(x + 2, y, x + w - 2, y + h, bgColor);
        }

        Component text = Component.translatable(item.textKey());
        int textW = minecraft.font.width(text);
        int textX = x + (w - textW) / 2;
        int textY = y + (h - 8) / 2;
        graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF);
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

    private int getDropdownIndexAt(double mouseX, double mouseY, int controlAreaX, int screenY, int controlAreaW, int height) {
        if (!expanded || values.isEmpty()) return -1;
        int dropdownY = screenY + height + 2;
        int itemH = getItemHeight();
        if (mouseX < controlAreaX || mouseX > controlAreaX + controlAreaW) return -1;
        if (mouseY < dropdownY + 2) return -1;
        int idx = (int) ((mouseY - (dropdownY + 2)) / itemH);
        if (idx >= 0 && idx < values.size()) return idx;
        return -1;
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

        // 点击主按钮
        if (mouseX >= controlAreaX && mouseX <= controlAreaX + buttonAreaW &&
            mouseY >= screenY && mouseY <= screenY + height) {
            toggleExpanded();
            return true;
        }

        // 点击下拉菜单项
        int idx = getDropdownIndexAt(mouseX, mouseY, controlAreaX, screenY, buttonAreaW, height);
        if (idx >= 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            selectIndex(idx);
            return true;
        }

        // 如果点击了下拉菜单外面，关闭它
        if (expanded) {
            expanded = false;
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

        hoveredIndex = getDropdownIndexAt(mouseX, mouseY, controlAreaX, screenY, buttonAreaW, height);

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!expanded || !layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (mouseX >= screenX && mouseX <= screenX + dim.w) {
            int dropdownY = screenY + height + 2;
            if (mouseY >= dropdownY) {
                // 滚动下拉菜单
                return true;
            }
        }
        return false;
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

        if (expanded && keyCode == 256) {
            expanded = false;
            return true;
        }

        return false;
    }
}