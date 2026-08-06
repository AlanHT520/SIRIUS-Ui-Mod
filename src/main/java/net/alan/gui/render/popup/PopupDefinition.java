package net.alan.gui.render.popup;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PopupDefinition {
    private String type = "dialog";
    private boolean modal = true;
    private int width = 300;
    @SerializedName("min_height") private int minHeight = 120;
    private int padding = 16;
    @SerializedName("overlay_color") private String overlayColor = "0xCC1A1A1A";
    @SerializedName("box_color") private String boxColor = "0xE6282828";
    @SerializedName("border_color") private String borderColor = "0xFF555555";
    @SerializedName("has_inputs") private boolean hasInputs;
    private Map<String, String> variables = new LinkedHashMap<>();
    private List<PopupInputDef> inputs = new ArrayList<>();
    private List<JsonElement> elements = new ArrayList<>();

    public PopupDefinition() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isModal() { return modal; }
    public void setModal(boolean modal) { this.modal = modal; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getMinHeight() { return minHeight; }
    public void setMinHeight(int minHeight) { this.minHeight = minHeight; }
    public int getPadding() { return padding; }
    public void setPadding(int padding) { this.padding = padding; }
    public String getOverlayColor() { return overlayColor; }
    public void setOverlayColor(String overlayColor) { this.overlayColor = overlayColor; }
    public String getBoxColor() { return boxColor; }
    public void setBoxColor(String boxColor) { this.boxColor = boxColor; }
    public String getBorderColor() { return borderColor; }
    public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
    public boolean isHasInputs() { return hasInputs; }
    public void setHasInputs(boolean hasInputs) { this.hasInputs = hasInputs; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public List<PopupInputDef> getInputs() { return inputs; }
    public void setInputs(List<PopupInputDef> inputs) { this.inputs = inputs; }
    public List<JsonElement> getElements() { return elements; }
    public void setElements(List<JsonElement> elements) { this.elements = elements; }

    public int parseColor(String hex) {
        if (hex == null) return 0xFFFFFFFF;
        hex = hex.replace("0x", "").replace("0X", "");
        return (int) Long.parseLong(hex, 16);
    }

    public static class PopupInputDef {
        private String id;
        private String label;
        @SerializedName("default_value") private String defaultValue = "";
        private String hint = "";
        @SerializedName("max_length") private int maxLength = 128;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    }
}