package net.alan.gui.render.popup;

import net.alan.gui.data.Action;
import net.alan.gui.render.ActionExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class PopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopupManager.class);

    private final Deque<PopupInstance> stack = new LinkedList<>();
    private final Map<String, PopupDefinition> registeredPopups = new HashMap<>();
    private final Minecraft minecraft;
    private final ActionExecutor executor;

    private ResourceLocation buttonTextureNormal;
    private ResourceLocation buttonTextureHighlighted;

    public PopupManager(Minecraft minecraft, ActionExecutor executor) {
        this.minecraft = minecraft;
        this.executor = executor;
        loadTheme();
    }

    public void loadTheme() {
        ResourceManager rm = minecraft.getResourceManager();
        PopupTheme.load(rm);
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        String tn = theme.dialog.button.textureNormal;
        String th = theme.dialog.button.textureHighlighted;
        if (tn != null) buttonTextureNormal = ResourceLocation.parse(tn);
        if (th != null) buttonTextureHighlighted = ResourceLocation.parse(th);
    }

    public void showPopup(String popupId, String instanceId, Map<String, String> params) {
        PopupDefinition def = registeredPopups.get(popupId);
        if (def == null) {
            def = PopupRegistry.get(minecraft.getResourceManager(), popupId);
        }
        if (def == null) {
            LOGGER.error("Cannot show popup: definition not found for {}", popupId);
            return;
        }
        showPopupWithDef(def, instanceId, params);
    }

    public void registerPopupDefinition(String id, PopupDefinition def) {
        registeredPopups.put(id, def);
        LOGGER.debug("Registered popup definition: {}", id);
    }

    public void clearRegisteredPopups() {
        registeredPopups.clear();
        LOGGER.debug("Cleared registered popup definitions");
    }

    public void showPopupFromJson(com.google.gson.JsonObject popupObj, String instanceId,
                                  Map<String, String> params) {
        PopupDefinition def = PopupRegistry.createFromJson(popupObj);
        if (def == null) {
            LOGGER.error("Cannot show popup: failed to parse inline definition");
            return;
        }
        showPopupWithDef(def, instanceId, params);
    }

    private void showPopupWithDef(PopupDefinition def, String instanceId, Map<String, String> params) {
        PopupInstance instance = new PopupInstance(instanceId, def, params,
                minecraft.getResourceManager(), executor);
        dismiss(instanceId);
        stack.push(instance);
        legacyStack.push(null);
        if (def.isHasInputs() && !def.getInputs().isEmpty()) {
            instance.setFocusedInputFieldId(def.getInputs().get(0).getId());
        }
    }

    public void showDialog(String id, String title, String message,
                           java.util.List<PopupOverlay.Button> buttons) {
        showDialog(id, title, message, buttons, null);
    }

    public void showDialog(String id, String title, String message,
                           java.util.List<PopupOverlay.Button> buttons, Runnable onDismiss) {
        PopupOverlay overlay = new PopupOverlay.Builder(id, PopupOverlay.Type.DIALOG)
                .title(title)
                .message(message)
                .buttons(buttons)
                .modal(true)
                .onDismiss(onDismiss)
                .build();
        pushLegacy(overlay);
    }

    public void showInputDialog(String id, String title,
                                java.util.List<PopupOverlay.InputField> inputFields,
                                java.util.List<PopupOverlay.Button> buttons) {
        PopupOverlay overlay = new PopupOverlay.Builder(id, PopupOverlay.Type.INPUT_DIALOG)
                .title(title)
                .inputFields(inputFields)
                .buttons(buttons)
                .modal(true)
                .build();
        pushLegacy(overlay);
    }

    public void showToast(String id, String message, int durationMs) {
        PopupOverlay overlay = new PopupOverlay.Builder(id, PopupOverlay.Type.TOAST)
                .message(message)
                .durationMs(durationMs)
                .modal(false)
                .build();
        pushLegacy(overlay);
    }

    public void showLoading(String id, String message) {
        PopupOverlay overlay = new PopupOverlay.Builder(id, PopupOverlay.Type.LOADING)
                .message(message)
                .modal(true)
                .build();
        pushLegacy(overlay);
    }

    public void showTooltip(String id, String message, int x, int y) {
        PopupOverlay overlay = new PopupOverlay.Builder(id, PopupOverlay.Type.TOOLTIP)
                .message(message)
                .position(x, y, 0, 0)
                .modal(false)
                .build();
        pushLegacy(overlay);
    }

    private final Deque<PopupOverlay> legacyStack = new LinkedList<>();

    private void pushLegacy(PopupOverlay overlay) {
        dismiss(overlay.getId());
        stack.push(null);
        legacyStack.push(overlay);
    }

    public void dismiss(String id) {
        Iterator<PopupInstance> it = stack.iterator();
        Iterator<PopupOverlay> lit = legacyStack.iterator();
        while (it.hasNext()) {
            PopupInstance instance = it.next();
            PopupOverlay legacy = lit.next();
            String overlayId = instance != null ? instance.getId() : (legacy != null ? legacy.getId() : null);
            if (id.equals(overlayId)) {
                it.remove();
                lit.remove();
                if (instance != null && instance.getOnDismiss() != null) {
                    instance.getOnDismiss().run();
                }
                if (legacy != null && legacy.getOnDismiss() != null) {
                    legacy.getOnDismiss().run();
                }
                return;
            }
        }
    }

    public void dismissAll() {
        while (!stack.isEmpty()) {
            PopupInstance instance = stack.pop();
            PopupOverlay legacy = legacyStack.pop();
            if (instance != null && instance.getOnDismiss() != null) {
                instance.getOnDismiss().run();
            }
            if (legacy != null && legacy.getOnDismiss() != null) {
                legacy.getOnDismiss().run();
            }
        }
    }

    public boolean isModalActive() {
        Iterator<PopupInstance> it = stack.iterator();
        Iterator<PopupOverlay> lit = legacyStack.iterator();
        while (it.hasNext()) {
            PopupInstance instance = it.next();
            PopupOverlay legacy = lit.next();
            if (instance != null && instance.isModal()) return true;
            if (legacy != null && legacy.isModal()) return true;
        }
        return false;
    }

    public boolean hasActive() {
        return !stack.isEmpty();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (stack.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        Iterator<PopupInstance> it = stack.iterator();
        Iterator<PopupOverlay> lit = legacyStack.iterator();
        while (it.hasNext()) {
            PopupInstance instance = it.next();
            PopupOverlay legacy = lit.next();

            if (instance != null && instance.isExpired()) {
                it.remove();
                lit.remove();
                if (instance.getOnDismiss() != null) {
                    instance.getOnDismiss().run();
                }
                continue;
            }
            if (legacy != null && legacy.isExpired()) {
                it.remove();
                lit.remove();
                if (legacy.getOnDismiss() != null) {
                    legacy.getOnDismiss().run();
                }
                continue;
            }

            if (instance != null) {
                instance.render(graphics, mouseX, mouseY, delta);
            } else if (legacy != null) {
                renderLegacy(graphics, legacy, mouseX, mouseY);
            }
        }

        graphics.pose().popPose();
    }

    private void renderLegacy(GuiGraphics graphics, PopupOverlay overlay, int mouseX, int mouseY) {
        switch (overlay.getType()) {
            case DIALOG -> renderLegacyDialog(graphics, overlay, mouseX, mouseY);
            case INPUT_DIALOG -> renderLegacyInputDialog(graphics, overlay, mouseX, mouseY);
            case TOAST -> renderLegacyToast(graphics, overlay);
            case LOADING -> renderLegacyLoading(graphics, overlay);
            case TOOLTIP -> renderLegacyTooltip(graphics, overlay, mouseX, mouseY);
        }
    }

    private void renderLegacyDialog(GuiGraphics graphics, PopupOverlay overlay, int mouseX, int mouseY) {
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, sw, sh, dt.parseColor(dt.overlayColor));

        Font font = minecraft.font;
        int titleHeight = overlay.getTitle() != null ? font.lineHeight + dt.padding : 0;
        int messageHeight = calcLegacyMessageHeight(overlay, dt);
        int buttonAreaHeight = overlay.getButtons().isEmpty() ? 0 : dt.button.height + dt.padding * 2;
        int totalHeight = titleHeight + messageHeight + buttonAreaHeight + dt.padding;
        totalHeight = Math.max(totalHeight, dt.minHeight);

        int dx = (sw - dt.width) / 2;
        int dy = (sh - totalHeight) / 2;

        renderLegacyBox(graphics, dx, dy, dt.width, totalHeight, dt);

        if (overlay.getTitle() != null) {
            int titleBarEnd = dy + font.lineHeight + dt.padding;
            graphics.fill(dx, dy, dx + dt.width, titleBarEnd, dt.parseColor(dt.titleBarColor));
            graphics.fill(dx, titleBarEnd, dx + dt.width, titleBarEnd + 1, dt.parseColor(dt.separatorColor));
            graphics.drawCenteredString(font, overlay.getTitle(), sw / 2,
                    dy + dt.padding / 2, dt.parseColor(dt.titleColor));
        }

        int currentY = dy + dt.padding;
        if (overlay.getTitle() != null) {
            currentY += font.lineHeight + dt.padding;
        }

        if (overlay.getMessage() != null) {
            graphics.drawWordWrap(font,
                    Component.literal(overlay.getMessage()),
                    dx + dt.padding, currentY, dt.width - dt.padding * 2, dt.parseColor(dt.messageColor));
            currentY += messageHeight;
        }

        renderLegacyButtons(graphics, overlay, font, dx, dy, totalHeight, sw, mouseX, mouseY, dt);
    }

    private void renderLegacyInputDialog(GuiGraphics graphics, PopupOverlay overlay, int mouseX, int mouseY) {
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, sw, sh, dt.parseColor(dt.overlayColor));

        Font font = minecraft.font;
        int titleHeight = overlay.getTitle() != null ? font.lineHeight + dt.padding : 0;
        int inputAreaHeight = calcLegacyInputAreaHeight(overlay, dt);
        int buttonAreaHeight = overlay.getButtons().isEmpty() ? 0 : dt.button.height + dt.padding * 2;
        int totalHeight = titleHeight + inputAreaHeight + buttonAreaHeight + dt.padding;
        totalHeight = Math.max(totalHeight, dt.minHeight);

        int dx = (sw - dt.width) / 2;
        int dy = (sh - totalHeight) / 2;

        renderLegacyBox(graphics, dx, dy, dt.width, totalHeight, dt);

        if (overlay.getTitle() != null) {
            int titleBarEnd = dy + font.lineHeight + dt.padding;
            graphics.fill(dx, dy, dx + dt.width, titleBarEnd, dt.parseColor(dt.titleBarColor));
            graphics.fill(dx, titleBarEnd, dx + dt.width, titleBarEnd + 1, dt.parseColor(dt.separatorColor));
            graphics.drawCenteredString(font, overlay.getTitle(), sw / 2,
                    dy + dt.padding / 2, dt.parseColor(dt.titleColor));
        }

        int currentY = dy + dt.padding;
        if (overlay.getTitle() != null) {
            currentY += font.lineHeight + dt.padding;
        }

        int inputX = dx + dt.padding;
        int inputW = dt.width - dt.padding * 2;
        PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = dt.input;

        for (PopupOverlay.InputField field : overlay.getInputFields()) {
            if (field.getLabel() != null) {
                graphics.drawString(font, field.getLabel(), inputX, currentY, dt.parseColor(itheme.labelColor));
                currentY += font.lineHeight + itheme.labelGap;
            }

            int inputY = currentY;
            boolean isFocused = field.getId().equals(legacyFocusedInputFieldId);
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
            int textY = inputY + (itheme.height - font.lineHeight) / 2;
            graphics.drawString(font, displayText, textX, textY, textColor);

            if (isFocused && !field.getValue().isEmpty()) {
                long elapsed = System.currentTimeMillis() - legacyCursorBlinkStart;
                if ((elapsed / 500) % 2 == 0) {
                    int cursorX = textX + font.width(displayText);
                    graphics.fill(cursorX, inputY + 2, cursorX + 1, inputY + itheme.height - 2,
                            dt.parseColor(itheme.textColor));
                }
            }

            currentY += itheme.height + itheme.gap;
        }

        renderLegacyButtons(graphics, overlay, font, dx, dy, totalHeight, sw, mouseX, mouseY, dt);
    }

    private void renderLegacyBox(GuiGraphics graphics, int x, int y, int w, int h,
                                 PopupTheme.PopupThemeData.DialogTheme dt) {
        int shadowSize = dt.shadowSize;
        int shadowColor = dt.parseColor(dt.shadowColor);
        for (int i = shadowSize; i > 0; i--) {
            int alpha = (int)(((shadowSize - i + 1) / (float)(shadowSize + 1)) * 0x60);
            int c = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            graphics.fill(x + i, y + i + 2, x + w + i, y + h + i + 2, c);
        }

        graphics.fill(x, y, x + w, y + h, dt.parseColor(dt.boxColor));

        graphics.fill(x, y, x + w, y + 1, 0x30FFFFFF);
        graphics.fill(x, y + 1, x + w, y + 2, 0x10FFFFFF);
        graphics.fill(x, y, x + 1, y + h, 0x20FFFFFF);
        graphics.fill(x + w - 1, y, x + w, y + h, 0x40000000);

        graphics.renderOutline(x, y, w, h, dt.parseColor(dt.borderColor));
    }

    private void renderLegacyButtons(GuiGraphics graphics, PopupOverlay overlay, Font font,
                                     int dx, int dy, int totalHeight, int sw,
                                     int mouseX, int mouseY, PopupTheme.PopupThemeData.DialogTheme dt) {
        if (overlay.getButtons().isEmpty()) return;

        PopupTheme.PopupThemeData.DialogTheme.ButtonTheme bt = dt.button;
        int totalBtnW = overlay.getButtons().size() * bt.width + (overlay.getButtons().size() - 1) * bt.gap;
        int startX = sw / 2 - totalBtnW / 2;
        int btnY = dy + totalHeight - dt.padding - bt.height;

        for (int i = 0; i < overlay.getButtons().size(); i++) {
            PopupOverlay.Button btn = overlay.getButtons().get(i);
            int bx = startX + i * (bt.width + bt.gap);
            boolean hovered = mouseX >= bx && mouseX <= bx + bt.width
                    && mouseY >= btnY && mouseY <= btnY + bt.height;

            if (buttonTextureNormal != null) {
                ResourceLocation tex = (hovered && buttonTextureHighlighted != null)
                        ? buttonTextureHighlighted : buttonTextureNormal;
                graphics.blitSprite(tex, bx, btnY, bt.width, bt.height);
            } else {
                int bgColor = dt.parseColor(hovered ? "0xFF555555" : "0xFF3A3A3A");
                graphics.fill(bx, btnY, bx + bt.width, btnY + bt.height, bgColor);

                graphics.fill(bx, btnY, bx + bt.width, btnY + 1, 0x30FFFFFF);
                graphics.fill(bx, btnY + bt.height - 1, bx + bt.width, btnY + bt.height, 0x40000000);

                int borderCol = dt.parseColor(hovered ? "0xFF888888" : "0xFF505050");
                graphics.renderOutline(bx, btnY, bt.width, bt.height, borderCol);

                if (hovered) {
                    graphics.fill(bx, btnY, bx + bt.width, btnY + bt.height, 0x10FFFFFF);
                }
            }

            String btnText = btn.getText();
            int textColor = dt.parseColor(bt.textColor);
            graphics.drawCenteredString(font, btnText,
                    bx + bt.width / 2,
                    btnY + (bt.height - font.lineHeight) / 2, textColor);
        }
    }

    private void renderLegacyToast(GuiGraphics graphics, PopupOverlay overlay) {
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.ToastTheme tt = theme.toast;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        Font font = minecraft.font;
        int tw = Math.min(tt.width, sw - 20);
        int tx = (sw - tw) / 2;
        int ty = 10;

        float alpha = overlay.getRemainingAlpha();
        int bgColor = applyAlpha(tt.parseColor(tt.backgroundColor), alpha);
        int borderColor = applyAlpha(tt.parseColor(tt.borderColor), alpha);
        int textColor = applyAlpha(tt.parseColor(tt.textColor), alpha);

        int shadowAlpha = (int)(alpha * 0x50);
        int shadowC = (shadowAlpha << 24) | 0x000000;
        graphics.fill(tx + 2, ty + 3, tx + tw + 2, ty + tt.height + 3, shadowC);

        graphics.fill(tx, ty, tx + tw, ty + tt.height, bgColor);
        graphics.fill(tx, ty, tx + tw, ty + 1, applyAlpha(0x40FFFFFF, alpha));
        graphics.renderOutline(tx, ty, tw, tt.height, borderColor);

        String msg = overlay.getMessage() != null ? overlay.getMessage() : "";
        if (font.width(msg) > tw - 10) {
            msg = font.plainSubstrByWidth(msg, tw - 20) + "...";
        }
        graphics.drawCenteredString(font, msg, sw / 2,
                ty + (tt.height - font.lineHeight) / 2, textColor);
    }

    private void renderLegacyLoading(GuiGraphics graphics, PopupOverlay overlay) {
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.LoadingTheme lt = theme.loading;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;

        graphics.fill(0, 0, sw, sh, lt.parseColor(lt.backgroundColor));

        String msg = overlay.getMessage() != null ? overlay.getMessage() : "Loading...";
        int textWidth = font.width(msg);
        int lw = textWidth + 60;
        int lh = 40;
        int lx = (sw - lw) / 2;
        int ly = (sh - lh) / 2;

        int shadowSize = 3;
        for (int i = shadowSize; i > 0; i--) {
            int alpha = (int)(((shadowSize - i + 1) / (float)(shadowSize + 1)) * 0x40);
            int c = (alpha << 24) | 0x000000;
            graphics.fill(lx + i, ly + i + 2, lx + lw + i, ly + lh + i + 2, c);
        }

        graphics.fill(lx, ly, lx + lw, ly + lh, lt.parseColor(lt.boxColor));
        graphics.fill(lx, ly, lx + lw, ly + 1, 0x40FFFFFF);
        graphics.renderOutline(lx, ly, lw, lh, lt.parseColor(lt.borderColor));

        long elapsed = System.currentTimeMillis() - overlay.getCreateTime();
        int dotCount = (int) ((elapsed / 300) % 4);
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < dotCount; i++) {
            dots.append(".");
        }

        graphics.drawCenteredString(font, msg + dots, sw / 2,
                ly + (lh - font.lineHeight) / 2, lt.parseColor(lt.textColor));
    }

    private void renderLegacyTooltip(GuiGraphics graphics, PopupOverlay overlay, int mouseX, int mouseY) {
        Font font = minecraft.font;
        String msg = overlay.getMessage();
        if (msg == null || msg.isEmpty()) return;

        int tw = font.width(msg) + 12;
        int th = font.lineHeight + 8;
        int tx = overlay.getX() > 0 ? overlay.getX() : mouseX + 12;
        int ty = overlay.getY() > 0 ? overlay.getY() : mouseY - th;

        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        if (tx + tw > sw) tx = sw - tw - 4;
        if (ty < 4) ty = 4;
        if (ty + th > sh) ty = sh - th - 4;

        graphics.fill(tx, ty, tx + tw, ty + th, 0xCC1A1A1A);
        graphics.fill(tx, ty, tx + tw, ty + 1, 0x40FFFFFF);
        graphics.renderOutline(tx, ty, tw, th, 0xFF555555);
        graphics.drawString(font, msg, tx + 6, ty + 4, 0xFFFFFFFF);
    }

    private String legacyFocusedInputFieldId;
    private long legacyCursorBlinkStart;

    public boolean mouseClicked(double mx, double my, int button) {
        if (stack.isEmpty()) return false;

        PopupInstance top = stack.peek();
        PopupOverlay legacy = legacyStack.peek();

        if (top != null) {
            return top.mouseClicked(mx, my, button);
        }

        if (legacy != null) {
            return legacyMouseClicked(legacy, mx, my, button);
        }

        return false;
    }

    private boolean legacyMouseClicked(PopupOverlay overlay, double mx, double my, int button) {
        if (overlay.getType() == PopupOverlay.Type.INPUT_DIALOG) {
            if (handleLegacyInputClick(overlay, mx, my)) return true;
        }

        if (overlay.getType() == PopupOverlay.Type.DIALOG || overlay.getType() == PopupOverlay.Type.INPUT_DIALOG) {
            PopupTheme.PopupThemeData theme = PopupTheme.get();
            PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
            int sw = minecraft.getWindow().getGuiScaledWidth();
            int sh = minecraft.getWindow().getGuiScaledHeight();
            int totalHeight = calcLegacyDialogHeight(overlay, dt);
            int buttonY = (sh - totalHeight) / 2 + totalHeight - dt.padding - dt.button.height;

            if (!overlay.getButtons().isEmpty()) {
                int totalBtnW = overlay.getButtons().size() * dt.button.width
                        + (overlay.getButtons().size() - 1) * dt.button.gap;
                int startX = sw / 2 - totalBtnW / 2;
                for (int i = 0; i < overlay.getButtons().size(); i++) {
                    PopupOverlay.Button btn = overlay.getButtons().get(i);
                    int bx = startX + i * (dt.button.width + dt.button.gap);
                    if (mx >= bx && mx <= bx + dt.button.width
                            && my >= buttonY && my <= buttonY + dt.button.height) {
                        handleLegacyButtonClick(btn);
                        return true;
                    }
                }
            }
            return true;
        }

        if (overlay.isModal()) {
            return true;
        }

        return false;
    }

    private boolean handleLegacyInputClick(PopupOverlay overlay, double mx, double my) {
        PopupTheme.PopupThemeData theme = PopupTheme.get();
        PopupTheme.PopupThemeData.DialogTheme dt = theme.dialog;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;

        int titleHeight = overlay.getTitle() != null ? font.lineHeight + dt.padding : 0;
        int totalHeight = calcLegacyDialogHeight(overlay, dt);
        int dy = (sh - totalHeight) / 2;
        int inputX = (sw - dt.width) / 2 + dt.padding;
        int inputW = dt.width - dt.padding * 2;
        int currentY = dy + dt.padding + titleHeight;
        PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = dt.input;

        for (PopupOverlay.InputField field : overlay.getInputFields()) {
            if (field.getLabel() != null) {
                currentY += font.lineHeight + itheme.labelGap;
            }
            if (mx >= inputX && mx <= inputX + inputW
                    && my >= currentY && my <= currentY + itheme.height) {
                legacyFocusedInputFieldId = field.getId();
                legacyCursorBlinkStart = System.currentTimeMillis();
                return true;
            }
            currentY += itheme.height + itheme.gap;
        }
        return false;
    }

    private void handleLegacyButtonClick(PopupOverlay.Button btn) {
        if (btn.getAction() != null && !btn.getAction().isEmpty()) {
            Action action = new Action();
            action.setType(btn.getAction());
            executor.execute(action);
        }
        if (btn.isClosePopup()) {
            dismissAll();
        }
    }

    private int calcLegacyDialogHeight(PopupOverlay overlay, PopupTheme.PopupThemeData.DialogTheme dt) {
        Font font = minecraft.font;
        int titleHeight = overlay.getTitle() != null ? font.lineHeight + dt.padding : 0;
        int contentHeight;

        if (overlay.getType() == PopupOverlay.Type.INPUT_DIALOG) {
            contentHeight = calcLegacyInputAreaHeight(overlay, dt);
        } else {
            contentHeight = calcLegacyMessageHeight(overlay, dt);
        }

        int buttonAreaHeight = overlay.getButtons().isEmpty() ? 0 : dt.button.height + dt.padding * 2;
        int totalHeight = titleHeight + contentHeight + buttonAreaHeight + dt.padding;
        return Math.max(totalHeight, dt.minHeight);
    }

    private int calcLegacyInputAreaHeight(PopupOverlay overlay, PopupTheme.PopupThemeData.DialogTheme dt) {
        PopupTheme.PopupThemeData.DialogTheme.InputTheme itheme = dt.input;
        int height = overlay.getInputFields().size() * itheme.height
                + (overlay.getInputFields().size() - 1) * itheme.gap;
        for (PopupOverlay.InputField field : overlay.getInputFields()) {
            if (field.getLabel() != null) {
                height += minecraft.font.lineHeight + itheme.labelGap;
            }
        }
        return height > 0 ? height + dt.padding : 0;
    }

    private int calcLegacyMessageHeight(PopupOverlay overlay, PopupTheme.PopupThemeData.DialogTheme dt) {
        if (overlay.getMessage() == null) return 0;
        Font font = minecraft.font;
        return font.split(
                Component.literal(overlay.getMessage()),
                dt.width - dt.padding * 2
        ).size() * font.lineHeight + dt.padding;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (stack.isEmpty()) return false;

        PopupInstance top = stack.peek();
        PopupOverlay legacy = legacyStack.peek();

        if (top != null) {
            if (top.getDefinition().isHasInputs() && top.getFocusedInputFieldId() != null) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    dismissAll();
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_TAB) {
                    cycleInputFocus(top);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                    PopupOverlay.InputField field = top.findInputField(top.getFocusedInputFieldId());
                    if (field != null) {
                        field.deleteLastChar();
                        top.setFocusedInputFieldId(top.getFocusedInputFieldId());
                    }
                    return true;
                }
                return true;
            }
            if (top.isModal()) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    dismissAll();
                    return true;
                }
                return top.keyPressed(keyCode, scanCode, modifiers);
            }
            return top.keyPressed(keyCode, scanCode, modifiers);
        }

        if (legacy != null) {
            return legacyKeyPressed(legacy, keyCode, scanCode, modifiers);
        }

        return false;
    }

    private boolean legacyKeyPressed(PopupOverlay overlay, int keyCode, int scanCode, int modifiers) {
        if (overlay.getType() == PopupOverlay.Type.INPUT_DIALOG && legacyFocusedInputFieldId != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                dismissAll();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                cycleLegacyInputFocus(overlay);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                PopupOverlay.InputField field = findLegacyInputField(overlay, legacyFocusedInputFieldId);
                if (field != null) {
                    field.deleteLastChar();
                    legacyCursorBlinkStart = System.currentTimeMillis();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!overlay.getButtons().isEmpty()) {
                    handleLegacyButtonClick(overlay.getButtons().get(0));
                }
                return true;
            }
            return true;
        }

        if (overlay.isModal()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                dismissAll();
                return true;
            }
            return true;
        }

        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (stack.isEmpty()) return false;

        PopupInstance top = stack.peek();
        PopupOverlay legacy = legacyStack.peek();

        if (top != null) {
            if (top.getDefinition().isHasInputs() && top.getFocusedInputFieldId() != null) {
                PopupOverlay.InputField field = top.findInputField(top.getFocusedInputFieldId());
                if (field != null && codePoint >= 32 && codePoint != 127) {
                    field.appendChar(codePoint);
                    top.setFocusedInputFieldId(top.getFocusedInputFieldId());
                }
                return true;
            }
            if (top.isModal()) {
                return top.charTyped(codePoint, modifiers);
            }
            return top.charTyped(codePoint, modifiers);
        }

        if (legacy != null) {
            return legacyCharTyped(legacy, codePoint, modifiers);
        }

        return false;
    }

    private boolean legacyCharTyped(PopupOverlay overlay, char codePoint, int modifiers) {
        if (overlay.getType() == PopupOverlay.Type.INPUT_DIALOG && legacyFocusedInputFieldId != null) {
            PopupOverlay.InputField field = findLegacyInputField(overlay, legacyFocusedInputFieldId);
            if (field != null && codePoint >= 32 && codePoint != 127) {
                field.appendChar(codePoint);
                legacyCursorBlinkStart = System.currentTimeMillis();
            }
            return true;
        }

        if (overlay.isModal()) {
            return true;
        }

        return false;
    }

    private void cycleInputFocus(PopupInstance instance) {
        java.util.List<PopupOverlay.InputField> fields = instance.getInputFields();
        if (fields.isEmpty()) return;
        String current = instance.getFocusedInputFieldId();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getId().equals(current)) {
                instance.setFocusedInputFieldId(fields.get((i + 1) % fields.size()).getId());
                return;
            }
        }
        instance.setFocusedInputFieldId(fields.get(0).getId());
    }

    private void cycleLegacyInputFocus(PopupOverlay overlay) {
        java.util.List<PopupOverlay.InputField> fields = overlay.getInputFields();
        if (fields.isEmpty()) return;
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getId().equals(legacyFocusedInputFieldId)) {
                legacyFocusedInputFieldId = fields.get((i + 1) % fields.size()).getId();
                legacyCursorBlinkStart = System.currentTimeMillis();
                return;
            }
        }
        legacyFocusedInputFieldId = fields.get(0).getId();
        legacyCursorBlinkStart = System.currentTimeMillis();
    }

    private PopupOverlay.InputField findLegacyInputField(PopupOverlay overlay, String id) {
        for (PopupOverlay.InputField field : overlay.getInputFields()) {
            if (field.getId().equals(id)) return field;
        }
        return null;
    }

    public String getInputValue(String overlayId, String fieldId) {
        for (PopupInstance instance : stack) {
            if (instance != null && instance.getId().equals(overlayId)) {
                PopupOverlay.InputField field = instance.findInputField(fieldId);
                return field != null ? field.getValue() : null;
            }
        }
        for (PopupOverlay legacy : legacyStack) {
            if (legacy != null && legacy.getId().equals(overlayId)) {
                PopupOverlay.InputField field = findLegacyInputField(legacy, fieldId);
                return field != null ? field.getValue() : null;
            }
        }
        return null;
    }

    private static int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}