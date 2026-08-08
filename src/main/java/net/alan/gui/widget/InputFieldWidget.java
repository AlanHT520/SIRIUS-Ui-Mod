package net.alan.gui.widget;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Map;

public class InputFieldWidget extends BaseWidget {
    private final String label;
    private final String hint;
    private final int maxLength;
    private final String backgroundColor;
    private final String borderColor;
    private final String focusBorderColor;
    private final String textColor;
    private final String hintColor;
    private final String labelColor;

    private String value;
    private boolean isFocused;
    private long cursorBlinkStart;

    public InputFieldWidget(String id, LayoutProps layout, Map<String, String> variables,
                            Map<String, String> member, String label, String hint,
                            int maxLength, String initialValue,
                            String backgroundColor, String borderColor,
                            String focusBorderColor, String textColor,
                            String hintColor, String labelColor) {
        super(id, layout, variables, member);
        this.label = label;
        this.hint = hint != null ? hint : "";
        this.maxLength = maxLength > 0 ? maxLength : 128;
        this.value = initialValue != null ? initialValue : "";
        this.backgroundColor = backgroundColor != null ? backgroundColor : "0xFF1A1A1A";
        this.borderColor = borderColor != null ? borderColor : "0xFF555555";
        this.focusBorderColor = focusBorderColor != null ? focusBorderColor : "0xFFAAAAAA";
        this.textColor = textColor != null ? textColor : "0xFFFFFFFF";
        this.hintColor = hintColor != null ? hintColor : "0xFF555555";
        this.labelColor = labelColor != null ? labelColor : "0xFFAAAAAA";
        this.cursorBlinkStart = System.currentTimeMillis();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value != null ? value : "";
    }

    private int parseColor(String hex) {
        if (hex == null) return 0xFFFFFFFF;
        hex = hex.replace("0x", "").replace("0X", "");
        return (int) Long.parseLong(hex, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenX = x + dim.x;
        int screenY = y + dim.y;

        int currentY = screenY;

        if (label != null && !label.isEmpty()) {
            String resolvedLabel = replaceVars(label, mergedCtx.variables());
            graphics.drawString(font, resolvedLabel, screenX, currentY, parseColor(labelColor));
            currentY += font.lineHeight + 4;
        }

        int inputH = dim.h - (currentY - screenY);
        if (inputH < 4) inputH = dim.h;

        int bgC = parseColor(backgroundColor);
        int brdC = parseColor(isFocused ? focusBorderColor : borderColor);

        graphics.fill(screenX, currentY, screenX + dim.w, currentY + inputH, bgC);
        graphics.renderOutline(screenX, currentY, dim.w, inputH, brdC);

        String displayText = value;
        int txtC = parseColor(textColor);
        if (displayText.isEmpty() && !hint.isEmpty()) {
            displayText = hint;
            txtC = parseColor(hintColor);
        }

        int textX = screenX + 4;
        int textY = currentY + (inputH - font.lineHeight) / 2;
        graphics.drawString(font, displayText, textX, textY, txtC);

        if (isFocused) {
            long elapsed = System.currentTimeMillis() - cursorBlinkStart;
            if ((elapsed / 500) % 2 == 0) {
                int cursorX = textX + font.width(value);
                graphics.fill(cursorX, currentY + 2, cursorX + 1, currentY + inputH - 2,
                        parseColor(textColor));
            }
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

        Minecraft mc = Minecraft.getInstance();
        int currentY = screenY;
        if (label != null && !label.isEmpty()) {
            currentY += mc.font.lineHeight + 4;
        }
        int inputH = dim.h - (currentY - screenY);

        boolean hit = mouseX >= screenX && mouseX <= screenX + dim.w
                && mouseY >= currentY && mouseY <= currentY + inputH;
        if (hit) {
            setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              RenderContext context, int x, int y, int width, int height) {
        if (!isFocused) return false;
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (!value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }
            cursorBlinkStart = System.currentTimeMillis();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers,
                             RenderContext context, int x, int y, int width, int height) {
        if (!isFocused) return false;
        if (codePoint >= 32 && codePoint != 127) {
            if (value.length() < maxLength) {
                value += codePoint;
                cursorBlinkStart = System.currentTimeMillis();
            }
        }
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        this.isFocused = focused;
        if (focused) {
            cursorBlinkStart = System.currentTimeMillis();
            playFocusSound();
        }
    }

    @Override
    public boolean isWidgetFocused() {
        return isFocused;
    }
}