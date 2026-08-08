package net.alan.gui.render.card;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardDefinition {
    public enum CardType {
        DIALOG,
        TOAST,
        LOADING,
        TOOLTIP
    }

    private String type = "dialog";
    private boolean modal = true;
    private int width = 300;
    @SerializedName("min_height") private int minHeight = 120;
    @SerializedName("max_height") private int maxHeight = 0;
    private int padding = 16;
    private String overlay = null;
    private BackgroundDef background = new BackgroundDef("0xF0282828");
    private String border = null;
    @SerializedName("border_width") private int borderWidth = 1;
    @SerializedName("title_bar") private TitleBarDef titleBar;
    @SerializedName("duration_ms") private int durationMs;
    @SerializedName("fade_ms") private int fadeMs = 500;
    @SerializedName("pos_x") private int posX = -1;
    @SerializedName("pos_y") private int posY = -1;
    @SerializedName("shadow_offset") private int shadowOffset = 4;
    @SerializedName("shadow_alpha") private int shadowAlpha = 0x50;
    @SerializedName("z_index") private int zIndex = 400;
    @SerializedName("data_source") private String dataSource;
    private Map<String, String> variables = new LinkedHashMap<>();
    private List<JsonElement> elements = new ArrayList<>();

    public CardDefinition() {}

    public CardType resolveType() {
        return switch (type) {
            case "toast" -> CardType.TOAST;
            case "loading" -> CardType.LOADING;
            case "tooltip" -> CardType.TOOLTIP;
            default -> CardType.DIALOG;
        };
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isModal() { return modal; }
    public void setModal(boolean modal) { this.modal = modal; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getMinHeight() { return minHeight; }
    public void setMinHeight(int minHeight) { this.minHeight = minHeight; }
    public int getMaxHeight() { return maxHeight; }
    public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }
    public int getPadding() { return padding; }
    public void setPadding(int padding) { this.padding = padding; }

    public String getOverlay() { return overlay; }
    public void setOverlay(String overlay) { this.overlay = overlay; }
    public boolean hasOverlay() { return overlay != null && !overlay.isBlank(); }

    public BackgroundDef getBackground() { return background; }
    public void setBackground(BackgroundDef background) { this.background = background; }

    public String getBorder() { return border; }
    public void setBorder(String border) { this.border = border; }
    public boolean hasBorder() { return border != null && !border.isBlank(); }
    public int getBorderWidth() { return borderWidth > 0 ? borderWidth : 1; }
    public void setBorderWidth(int borderWidth) { this.borderWidth = borderWidth; }

    public TitleBarDef getTitleBar() { return titleBar; }
    public void setTitleBar(TitleBarDef titleBar) { this.titleBar = titleBar; }
    public boolean hasTitleBar() { return titleBar != null; }

    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    public int getFadeMs() { return fadeMs; }
    public void setFadeMs(int fadeMs) { this.fadeMs = fadeMs; }
    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getShadowOffset() { return shadowOffset; }
    public void setShadowOffset(int shadowOffset) { this.shadowOffset = shadowOffset; }
    public int getShadowAlpha() { return shadowAlpha; }
    public void setShadowAlpha(int shadowAlpha) { this.shadowAlpha = shadowAlpha; }
    public int getZIndex() { return zIndex > 0 ? zIndex : 400; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public List<JsonElement> getElements() { return elements; }
    public void setElements(List<JsonElement> elements) { this.elements = elements; }

    public int parseColor(String hex) {
        if (hex == null) return 0xFFFFFFFF;
        hex = hex.replace("0x", "").replace("0X", "");
        return (int) Long.parseLong(hex, 16);
    }
}