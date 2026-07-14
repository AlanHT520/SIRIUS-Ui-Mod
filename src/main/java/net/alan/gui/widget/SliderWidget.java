package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.data.props.SliderProps;
import net.alan.gui.data.props.TextProps;
import net.alan.gui.data.style.TextureSet;
import net.alan.gui.render.OptionBinder;
import net.minecraft.client.InputType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SliderWidget extends BaseWidget {
    private static final int HANDLE_WIDTH = 8;
    private final SliderProps sliderProps;
    private final TextureSet trackTexture;
    private final TextureSet handleTexture;
    private final List<Widget> children;
    private double currentRatio = 0.5;
    private boolean isDragging = false;
    private boolean canChangeValue = false;
    private boolean isHovered = false;
    private boolean isFocused = false;
    private final Minecraft minecraft;

    public SliderWidget(String id, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                        SliderProps sliderProps, TextureSet trackTexture,
                        TextureSet handleTexture, TextProps textProps,
                        List<Widget> children) {
        super(id, layout, variables, member);
        this.sliderProps = sliderProps;
        this.trackTexture = trackTexture;
        this.handleTexture = handleTexture;
        this.children = children != null ? new ArrayList<>(children) : new ArrayList<>();
        this.minecraft = Minecraft.getInstance();

        // 初始化：从 Minecraft options 读取当前真实值（必须在创建 text child 之前）
        if (sliderProps.optionKey() != null) {
            double currentValue = OptionBinder.getOptionRawValue(sliderProps.optionKey(), minecraft.options);
            this.currentRatio = Mth.clamp(
                    (currentValue - sliderProps.min()) / (sliderProps.max() - sliderProps.min()),
                    0.0, 1.0
            );
            this.member.put("slider_value", String.valueOf(currentRatio));
            this.member.put("current_value", OptionBinder.formatValue(sliderProps.optionKey(), getCurrentRawValue()));
        } else if (member != null && member.containsKey("slider_value")) {
            try {
                currentRatio = Double.parseDouble(member.get("slider_value"));
            } catch (NumberFormatException ignored) {}
        }

        // 将 textProps 转为 TextWidget child（快捷方式，兼容旧写法）
        // 默认居中，可通过 text 块内的 "position" 覆盖
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

        // 轨道
        String trackTex = getTrackSprite();
        if (trackTex != null && !trackTex.isEmpty()) {
            var id = ResourceLocation.tryParse(trackTex);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, screenX, screenY, dim.w, dim.h);
            }
        } else {
            int trackY = screenY + dim.h / 2 - 2;
            graphics.fill(screenX, trackY, screenX + dim.w, trackY + 4, 0xFFAAAAAA);
        }

        // 手柄
        int handleX = screenX + (int) (currentRatio * (dim.w - HANDLE_WIDTH));
        String handleTex = getHandleSprite();
        if (handleTex != null && !handleTex.isEmpty()) {
            var id = ResourceLocation.tryParse(handleTex);
            if (id != null) {
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                graphics.blitSprite(id, handleX, screenY, HANDLE_WIDTH, dim.h);
            }
        } else {
            graphics.fill(handleX, screenY, handleX + HANDLE_WIDTH, screenY + dim.h, 0xFFFFFFFF);
        }

        // 渲染子元素（文本通过 text 快捷方式转为 TextWidget child）
        // 确保 current_value / slider_value 等动态变量在上下文可用
        RenderContext childCtx = mergedCtx;
        if (sliderProps.optionKey() != null) {
            childCtx = mergedCtx.withVar("current_value",
                    OptionBinder.formatValue(sliderProps.optionKey(), getCurrentRawValue()));
            childCtx = childCtx.withVar("slider_value", String.valueOf(currentRatio));
        }
        for (Widget child : children) {
            child.render(graphics, screenX, screenY, dim.w, dim.h, childCtx, mouseX, mouseY, delta);
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
                for (Widget w : children) {
                    w.setFocused(w == child);
                }
                return true;
            }
        }

        if (mouseX >= screenX && mouseX <= screenX + dim.w &&
                mouseY >= screenY && mouseY <= screenY + dim.h) {
            isDragging = true;
            canChangeValue = true;
            updateRatio(mouseX, screenX, dim.w);
            syncToOptions();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button,
                                 RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return false;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        // 通知所有子组件
        boolean childConsumed = false;
        for (Widget child : children) {
            if (child.mouseReleased(mouseX, mouseY, button, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                childConsumed = true;
            }
        }

        if (isDragging) {
            isDragging = false;
            syncToOptions();
            OptionBinder.saveOptions(minecraft.options);
            return true;
        }
        canChangeValue = false;
        return childConsumed;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                RenderContext context, int x, int y, int width, int height) {
        if (!layout.visible() || !layout.enabled()) return false;

        if (isDragging) {
            RenderContext mergedCtx = mergeContext(context);
            WidgetDimension dim = computeLayout(mergedCtx, width, height);
            Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
            if (!checkCondition(vars)) return false;
            int screenX = x + dim.x;
            updateRatio(mouseX, screenX, dim.w);
            syncToOptions();
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY,
                           RenderContext context, int x, int y, int width, int height) {
        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        if (isDragging) {
            updateRatio(mouseX, screenX, dim.w);
            syncToOptions();
        } else {
            isHovered = mouseX >= screenX && mouseX <= screenX + dim.w
                    && mouseY >= screenY && mouseY <= screenY + dim.h;
        }

        // 转发给子组件
        for (Widget child : children) {
            child.mouseMoved(mouseX, mouseY, mergedCtx, screenX, screenY, dim.w, dim.h);
        }
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

        // 转发给子组件
        for (Widget child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers, mergedCtx, screenX, screenY, dim.w, dim.h)) {
                return true;
            }
        }

        // 空格/回车/Numpad回车 切换键盘调整模式（对标原版 CommonInputs.selected）
        if (CommonInputs.selected(keyCode)) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        }

        if (this.canChangeValue) {
            double effectiveWidth = dim.w - HANDLE_WIDTH;
            if (effectiveWidth <= 0) return false;

            if (keyCode == 263) { // LEFT arrow
                setValue(currentRatio - (1.0 / effectiveWidth));
                syncToOptions();
                return true;
            }
            if (keyCode == 262) { // RIGHT arrow
                setValue(currentRatio + (1.0 / effectiveWidth));
                syncToOptions();
                return true;
            }
        }
        return false;
    }

    private void updateRatio(double mouseX, int sliderX, int sliderWidth) {
        double effectiveWidth = sliderWidth - HANDLE_WIDTH;
        double ratio = (mouseX - sliderX - HANDLE_WIDTH / 2.0) / effectiveWidth;
        setValue(ratio);
    }

    private void setValue(double ratio) {
        double oldRatio = this.currentRatio;
        this.currentRatio = Mth.clamp(ratio, 0.0, 1.0);
        if (oldRatio != this.currentRatio && sliderProps.step() > 0) {
            double value = sliderProps.min() + currentRatio * (sliderProps.max() - sliderProps.min());
            value = Math.round(value / sliderProps.step()) * sliderProps.step();
            currentRatio = (value - sliderProps.min()) / (sliderProps.max() - sliderProps.min());
            currentRatio = Mth.clamp(currentRatio, 0.0, 1.0);
        }
        if (this.member != null) {
            this.member.put("slider_value", String.valueOf(currentRatio));
            this.member.put("current_value", sliderProps.optionKey() != null
                    ? OptionBinder.formatValue(sliderProps.optionKey(), getCurrentRawValue())
                    : formatCurrentValue());
        }
    }

    private double getCurrentRawValue() {
        double value = sliderProps.min() + currentRatio * (sliderProps.max() - sliderProps.min());
        if (sliderProps.step() > 0) {
            value = Math.round(value / sliderProps.step()) * sliderProps.step();
        }
        return value;
    }

    private String formatCurrentValue() {
        double raw = getCurrentRawValue();
        // 整数步长 → 整数显示（如 FOV: 70），否则保留小数
        if (sliderProps.step() >= 1.0 && Math.abs(sliderProps.step() - Math.round(sliderProps.step())) < 0.001) {
            return String.valueOf((int) Math.round(raw));
        }
        return String.valueOf(raw);
    }

    private void syncToOptions() {
        if (sliderProps.optionKey() != null) {
            OptionBinder.syncSlider(
                    sliderProps.optionKey(),
                    currentRatio,
                    sliderProps.min(),
                    sliderProps.max(),
                    this.member,
                    minecraft.options
            );
        }
    }

    /**
     * 轨道纹理：获得焦点且未激活键盘模式时高亮（对标原版 isFocused && !canChangeValue）
     */
    private String getTrackSprite() {
        if (trackTexture == null) return null;
        if (isFocused && !canChangeValue) {
            String tex = trackTexture.getHighlighted();
            return tex != null ? tex : trackTexture.getNormal();
        }
        return trackTexture.getNormal();
    }

    /**
     * 手柄纹理：悬停或键盘模式时高亮（对标原版 !isHovered && !canChangeValue → normal）
     */
    private String getHandleSprite() {
        if (handleTexture == null) return null;
        if (!isHovered && !canChangeValue) {
            return handleTexture.getNormal() != null ? handleTexture.getNormal() : handleTexture.getHighlighted();
        }
        String tex = handleTexture.getHighlighted();
        return tex != null ? tex : handleTexture.getNormal();
    }

    @Override
    public void setFocused(boolean focused) {
        this.isFocused = focused;
        if (!focused) {
            this.canChangeValue = false;
        } else {
            InputType inputType = minecraft.getLastInputType();
            if (inputType == InputType.MOUSE || inputType == InputType.KEYBOARD_TAB) {
                this.canChangeValue = true;
            }
        }
    }

    @Override
    public boolean isWidgetFocused() {
        return isFocused;
    }
}