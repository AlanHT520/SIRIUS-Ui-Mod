package net.alan.gui.render.popup;

import java.util.ArrayList;
import java.util.List;

public class PopupOverlay {

    public enum Type {
        DIALOG,
        INPUT_DIALOG,
        TOAST,
        LOADING,
        TOOLTIP
    }

    private final String id;
    private final Type type;
    private final boolean modal;
    private final int durationMs;
    private final String title;
    private final String message;
    private final List<Button> buttons;
    private final List<InputField> inputFields;
    private final long createTime;
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final Runnable onDismiss;

    private PopupOverlay(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.modal = builder.modal;
        this.durationMs = builder.durationMs;
        this.title = builder.title;
        this.message = builder.message;
        this.buttons = builder.buttons != null ? builder.buttons : new ArrayList<>();
        this.inputFields = builder.inputFields != null ? builder.inputFields : new ArrayList<>();
        this.createTime = System.currentTimeMillis();
        this.x = builder.x;
        this.y = builder.y;
        this.w = builder.w;
        this.h = builder.h;
        this.onDismiss = builder.onDismiss;
    }

    public String getId() { return id; }
    public Type getType() { return type; }
    public boolean isModal() { return modal; }
    public int getDurationMs() { return durationMs; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public List<Button> getButtons() { return buttons; }
    public List<InputField> getInputFields() { return inputFields; }
    public long getCreateTime() { return createTime; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getW() { return w; }
    public int getH() { return h; }
    public Runnable getOnDismiss() { return onDismiss; }

    public boolean isExpired() {
        if (durationMs <= 0) return false;
        return System.currentTimeMillis() - createTime > durationMs;
    }

    public float getRemainingAlpha() {
        if (durationMs <= 0) return 1.0f;
        long elapsed = System.currentTimeMillis() - createTime;
        long remaining = durationMs - elapsed;
        if (remaining <= 0) return 0.0f;
        if (remaining < 500) return remaining / 500.0f;
        return 1.0f;
    }

    public static class Button {
        private final String text;
        private final String action;
        private final boolean closePopup;

        public Button(String text, String action, boolean closePopup) {
            this.text = text;
            this.action = action;
            this.closePopup = closePopup;
        }

        public String getText() { return text; }
        public String getAction() { return action; }
        public boolean isClosePopup() { return closePopup; }
    }

    public static class InputField {
        private final String id;
        private final String label;
        private final String defaultValue;
        private final String hint;
        private final int maxLength;
        private String value;

        public InputField(String id, String label, String defaultValue, String hint, int maxLength) {
            this.id = id;
            this.label = label;
            this.defaultValue = defaultValue;
            this.hint = hint;
            this.maxLength = maxLength;
            this.value = defaultValue != null ? defaultValue : "";
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getDefaultValue() { return defaultValue; }
        public String getHint() { return hint; }
        public int getMaxLength() { return maxLength; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value != null ? value : ""; }
        public void appendChar(char c) {
            if (value.length() < maxLength) {
                value += c;
            }
        }
        public void deleteLastChar() {
            if (!value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }
        }
    }

    public static class Builder {
        private String id;
        private Type type = Type.DIALOG;
        private boolean modal = true;
        private int durationMs;
        private String title;
        private String message;
        private List<Button> buttons;
        private List<InputField> inputFields;
        private int x;
        private int y;
        private int w;
        private int h;
        private Runnable onDismiss;

        public Builder(String id, Type type) {
            this.id = id;
            this.type = type;
        }

        public Builder modal(boolean modal) { this.modal = modal; return this; }
        public Builder durationMs(int durationMs) { this.durationMs = durationMs; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder buttons(List<Button> buttons) { this.buttons = buttons; return this; }
        public Builder addButton(String text, String action, boolean closePopup) {
            if (this.buttons == null) this.buttons = new ArrayList<>();
            this.buttons.add(new Button(text, action, closePopup));
            return this;
        }
        public Builder inputFields(List<InputField> inputFields) { this.inputFields = inputFields; return this; }
        public Builder addInputField(String id, String label, String defaultValue, String hint, int maxLength) {
            if (this.inputFields == null) this.inputFields = new ArrayList<>();
            this.inputFields.add(new InputField(id, label, defaultValue, hint, maxLength));
            return this;
        }
        public Builder position(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            return this;
        }
        public Builder onDismiss(Runnable onDismiss) { this.onDismiss = onDismiss; return this; }
        public PopupOverlay build() { return new PopupOverlay(this); }
    }
}