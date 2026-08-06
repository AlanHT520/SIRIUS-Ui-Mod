package net.alan.gui.render.popup;

import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.render.ActionExecutor;
import net.alan.gui.widget.WidgetFactory;
import net.alan.gui.widget.ContainerWidget;
import net.alan.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PopupInstance {
    private final String id;
    private final PopupDefinition definition;
    private final Map<String, String> params;
    private final Widget rootWidget;
    private final RenderContext renderContext;
    private final long createTime;
    private final int durationMs;
    private final List<PopupOverlay.InputField> inputFields;
    private final ActionExecutor executor;
    private Runnable onDismiss;

    private int popupX;
    private int popupY;
    private int popupW;
    private int popupH;
    private int contentW;
    private int contentH;
    private int inputAreaHeight;

    public PopupInstance(String id, PopupDefinition definition, Map<String, String> params,
                         ResourceManager resourceManager, ActionExecutor executor) {
        this.id = id;
        this.definition = definition;
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
        this.createTime = System.currentTimeMillis();
        this.durationMs = 0;
        this.executor = executor;

        Map<String, String> mergedVars = new LinkedHashMap<>();
        if (definition.getVariables() != null) {
            mergedVars.putAll(definition.getVariables());
        }
        if (params != null) {
            mergedVars.putAll(params);
        }

        this.contentW = definition.getWidth() - definition.getPadding() * 2;
        this.contentH = definition.getMinHeight() - definition.getPadding() * 2;

        this.inputFields = new ArrayList<>();
        if (definition.isHasInputs() && definition.getInputs() != null) {
            for (PopupDefinition.PopupInputDef inputDef : definition.getInputs()) {
                String defaultVal = inputDef.getDefaultValue();
                if (params != null && params.containsKey(inputDef.getId())) {
                    defaultVal = params.get(inputDef.getId());
                }
                PopupOverlay.InputField field = new PopupOverlay.InputField(
                        inputDef.getId(),
                        inputDef.getLabel(),
                        defaultVal,
                        inputDef.getHint(),
                        inputDef.getMaxLength()
                );
                inputFields.add(field);
            }
        }

        this.renderContext = new RenderContext(contentW, contentH, mergedVars);

        List<Widget> widgets = new ArrayList<>();
        if (definition.getElements() != null) {
            for (var elem : definition.getElements()) {
                if (elem.isJsonObject()) {
                    Widget w = WidgetFactory.create(elem.getAsJsonObject(), resourceManager, executor);
                    if (w != null) widgets.add(w);
                }
            }
        }

        this.rootWidget = new ContainerWidget("popup_root",
                new LayoutProps("0", "0", "screen.width", "screen.height", true, true),
                new HashMap<>(),
                new HashMap<>(),
                widgets
        );
    }

    public String getId() { return id; }
    public PopupDefinition getDefinition() { return definition; }
    public Map<String, String> getParams() { return params; }
    public long getCreateTime() { return createTime; }
    public List<PopupOverlay.InputField> getInputFields() { return inputFields; }
    public Runnable getOnDismiss() { return onDismiss; }
    public void setOnDismiss(Runnable onDismiss) { this.onDismiss = onDismiss; }

    public boolean isModal() {
        return definition.isModal();
    }

    public boolean isExpired() {
        if (durationMs <= 0) return false;
        return System.currentTimeMillis() - createTime > durationMs;
    }

    public void layout(int screenW, int screenH) {
        PopupDefinition def = definition;
        int padding = def.getPadding();

        contentW = def.getWidth() - padding * 2;
        contentH = def.getMinHeight() - padding * 2;

        inputAreaHeight = 0;
        if (def.isHasInputs() && !inputFields.isEmpty()) {
            PopupTheme.PopupThemeData theme = PopupTheme.get();
            PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = theme.dialog.input;
            Minecraft mc = Minecraft.getInstance();
            for (PopupOverlay.InputField field : inputFields) {
                if (field.getLabel() != null) {
                    inputAreaHeight += mc.font.lineHeight + itheme.labelGap;
                }
                inputAreaHeight += itheme.height;
            }
            if (inputFields.size() > 1) {
                inputAreaHeight += (inputFields.size() - 1) * itheme.gap;
            }
        }

        renderContext.setScreenSize(contentW, contentH);

        this.popupW = def.getWidth();
        this.popupH = contentH + padding * 2 + inputAreaHeight;
        this.popupX = (screenW - popupW) / 2;
        this.popupY = (screenH - popupH) / 2;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        PopupDefinition def = definition;
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        layout(sw, sh);

        graphics.fill(0, 0, sw, sh, def.parseColor(def.getOverlayColor()));

        renderBox(graphics);

        graphics.pose().pushPose();
        graphics.pose().translate(popupX + def.getPadding(), popupY + def.getPadding(), 0);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, contentW, contentH, renderContext,
                    mouseX - popupX - def.getPadding(),
                    mouseY - popupY - def.getPadding(),
                    delta);
            rootWidget.mouseMoved(
                    mouseX - popupX - def.getPadding(),
                    mouseY - popupY - def.getPadding(),
                    renderContext, 0, 0, contentW, contentH);
        }

        if (def.isHasInputs() && !inputFields.isEmpty()) {
            renderInputFields(graphics, mouseX, mouseY);
        }

        graphics.pose().popPose();
    }

    private void renderBox(GuiGraphics graphics) {
        PopupDefinition def = definition;
        int x = popupX;
        int y = popupY;
        int w = popupW;
        int h = popupH;

        int shadowSize = 4;
        for (int i = shadowSize; i > 0; i--) {
            int alpha = (int)(((shadowSize - i + 1) / (float)(shadowSize + 1)) * 0x50);
            int c = (alpha << 24) | 0x000000;
            graphics.fill(x + i, y + i + 2, x + w + i, y + h + i + 2, c);
        }

        graphics.fill(x, y, x + w, y + h, def.parseColor(def.getBoxColor()));

        graphics.fill(x, y, x + w, y + 1, 0x30FFFFFF);
        graphics.fill(x, y + 1, x + w, y + 2, 0x10FFFFFF);
        graphics.fill(x, y, x + 1, y + h, 0x20FFFFFF);
        graphics.fill(x + w - 1, y, x + w, y + h, 0x40000000);

        graphics.renderOutline(x, y, w, h, def.parseColor(def.getBorderColor()));
    }

    private void renderInputFields(GuiGraphics graphics, int mouseX, int mouseY) {
        if (inputAreaHeight <= 0) return;
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
        PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = dt.input;
        Minecraft mc = Minecraft.getInstance();
        int inputX = popupX + definition.getPadding();
        int inputW = popupW - definition.getPadding() * 2;

        int currentY = popupY + definition.getPadding() + contentH;

        for (PopupOverlay.InputField field : inputFields) {
            if (field.getLabel() != null) {
                graphics.drawString(mc.font, field.getLabel(), inputX, currentY,
                        dt.parseColor(itheme.labelColor));
                currentY += mc.font.lineHeight + itheme.labelGap;
            }

            int inputY = currentY;
            boolean isFocused = field.getId().equals(focusedInputFieldId);
            int borderColor = dt.parseColor(isFocused ? itheme.focusBorderColor : itheme.borderColor);

            graphics.fill(inputX, inputY, inputX + inputW, inputY + itheme.height,
                    dt.parseColor(itheme.backgroundColor));
            graphics.renderOutline(inputX, inputY, inputW, itheme.height, borderColor);

            String displayText = field.getValue();
            int textColor = dt.parseColor(itheme.textColor);
            if (displayText.isEmpty() && field.getHint() != null) {
                displayText = field.getHint();
                textColor = dt.parseColor(itheme.hintColor);
            }

            int textX = inputX + 4;
            int textY = inputY + (itheme.height - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font, displayText, textX, textY, textColor);

            if (isFocused && !field.getValue().isEmpty()) {
                long elapsed = System.currentTimeMillis() - cursorBlinkStart;
                if ((elapsed / 500) % 2 == 0) {
                    int cursorX = textX + mc.font.width(displayText);
                    graphics.fill(cursorX, inputY + 2, cursorX + 1, inputY + itheme.height - 2,
                            dt.parseColor(itheme.textColor));
                }
            }

            currentY += itheme.height + itheme.gap;
        }
    }

    private String focusedInputFieldId;
    private long cursorBlinkStart;

    public void setFocusedInputFieldId(String id) {
        this.focusedInputFieldId = id;
        this.cursorBlinkStart = System.currentTimeMillis();
    }

    public String getFocusedInputFieldId() {
        return focusedInputFieldId;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        PopupDefinition def = definition;
        int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        layout(sw, sh);

        if (def.isHasInputs() && !inputFields.isEmpty() && inputAreaHeight > 0) {
            PopupTheme.PopupThemeData theme = PopupTheme.get();
            PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
            PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = dt.input;
            Minecraft mc = Minecraft.getInstance();
            int inputX = popupX + def.getPadding();
            int inputW = popupW - def.getPadding() * 2;

            int currentY = popupY + def.getPadding() + contentH;

            for (PopupOverlay.InputField field : inputFields) {
                if (field.getLabel() != null) {
                    currentY += mc.font.lineHeight + itheme.labelGap;
                }
                if (mx >= inputX && mx <= inputX + inputW
                        && my >= currentY && my <= currentY + itheme.height) {
                    setFocusedInputFieldId(field.getId());
                    return true;
                }
                currentY += itheme.height + itheme.gap;
            }
        }

        if (rootWidget != null) {
            return rootWidget.mouseClicked(
                    mx - popupX - def.getPadding(),
                    my - popupY - def.getPadding(),
                    button, renderContext, 0, 0, contentW, contentH);
        }

        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (rootWidget != null) {
            return rootWidget.mouseReleased(
                    mx - popupX - definition.getPadding(),
                    my - popupY - definition.getPadding(),
                    button, renderContext, 0, 0, contentW, contentH);
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rootWidget != null) {
            return rootWidget.keyPressed(keyCode, scanCode, modifiers,
                    renderContext, 0, 0, contentW, contentH);
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (rootWidget != null) {
            return rootWidget.charTyped(codePoint, modifiers,
                    renderContext, 0, 0, contentW, contentH);
        }
        return false;
    }

    public PopupOverlay.InputField findInputField(String id) {
        for (PopupOverlay.InputField field : inputFields) {
            if (field.getId().equals(id)) return field;
        }
        return null;
    }
}