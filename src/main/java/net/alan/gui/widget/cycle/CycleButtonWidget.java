package net.alan.gui.widget.cycle;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.props.CycleButtonProps;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CycleButtonWidget extends BaseWidget {
    private final CycleButtonProps cycleProps;
    private final TextureSet texture;
    private final List<Widget> children;
    private int currentIndex = 0;
    private boolean isHovered = false;
    private final Minecraft minecraft;

    public CycleButtonWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                             CycleButtonProps cycleProps, TextureSet texture,
                             TextProps textProps, List<Widget> children) {
        super(id, layout, variables, member);
        this.cycleProps = cycleProps;
        this.texture = texture;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();

        // 初始化：从 Minecraft options 读取当前值（必须在创建 text child 之前）
        if (cycleProps.optionKey() != null && cycleProps.values() != null && !cycleProps.values().isEmpty()) {
            int idx = OptionBinder.getCycleOptionIndex(cycleProps.optionKey(), cycleProps.values(), minecraft.options);
            this.currentIndex = Mth.clamp(idx, 0, cycleProps.values().size() - 1);
            this.member.put("current_value", getCurrentDisplayText());
            this.member.put("current_index", String.valueOf(currentIndex));
        }

        // 将 textProps 转为 TextWidget child（默认居中）
        if (textProps != null) {
            String xPos = textProps.offsetX() != null && !textProps.offsetX().equals("0")
                    ? textProps.offsetX() : "parent.width / 2 - this.width / 2";
            String yPos = textProps.offsetY() != null && !textProps.offsetY().equals("0")
                    ? textProps.offsetY() : "parent.height / 2 - this.height / 2";
            this.children.add(0, new TextWidget(id + "_text",
                    new LayoutProps(xPos, yPos, "auto", "auto", true, true),
                    null, null, textProps));
        }
    }

    private String getCurrentDisplayText() {
        if (cycleProps.values() == null || cycleProps.values().isEmpty()) return "?";
        CycleValue cv = cycleProps.values().get(currentIndex);
        return cv.textKey();
    }

    private void cycleValue(int delta) {
        if (cycleProps.values() == null || cycleProps.values().isEmpty()) return;
        int size = cycleProps.values().size();
        currentIndex = Mth.positiveModulo(currentIndex + delta, size);
        this.member.put("current_value", getCurrentDisplayText());
        this.member.put("current_index", String.valueOf(currentIndex));
        syncToOptions();
    }

    private void syncToOptions() {
        if (cycleProps.optionKey() != null && cycleProps.values() != null && !cycleProps.values().isEmpty()) {
            CycleValue cv = cycleProps.values().get(currentIndex);
            if ("default".equalsIgnoreCase(cv.key())) {
                OptionBinder.resetOptionToDefault(cycleProps.optionKey(), minecraft.options);
            } else {
                OptionBinder.setCycleOptionValue(cycleProps.optionKey(), cv.key(), minecraft.options);
            }
            OptionBinder.saveOptions(minecraft.options);
        }
    }

    @Override
    public List<Widget> getChildren() { return children; }

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

        // 背景纹理
        String texPath = getSprite();
        if (texPath != null && !texPath.isEmpty()) {
            var id = ResourceLocation.tryParse(texPath);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, screenX, screenY, dim.w, dim.h);
            }
        } else {
            // 默认背景
            int bgColor = isHovered ? 0xFFCCCCCC : 0xFFAAAAAA;
            graphics.fill(screenX, screenY, screenX + dim.w, screenY + dim.h, bgColor);
        }

        // 渲染子元素
        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, mergedCtx, mouseX, mouseY, delta);
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

        // 先让子元素响应
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child.mouseClicked(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }

        if (mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (Screen.hasShiftDown()) {
                cycleValue(-1);
            } else {
                cycleValue(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                 RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h) {
            if (scrollY > 0.0) {
                cycleValue(-1);
            } else if (scrollY < 0.0) {
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

        isHovered = mouseX >= screenX && mouseX <= screenX + dim.w
                && mouseY >= screenY && mouseY <= screenY + dim.h;

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

    private String getSprite() {
        if (texture == null) return null;
        if (!layout.enabled()) {
            String tex = texture.getDisabled();
            return tex != null ? tex : texture.getNormal();
        }
        if (isHovered) {
            String tex = texture.getHighlighted();
            return tex != null ? tex : texture.getNormal();
        }
        return texture.getNormal();
    }
}