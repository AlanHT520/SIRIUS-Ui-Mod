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
    private int padding = 16;
    private String overlay = null;
    private BackgroundDef background = new BackgroundDef("0xF0282828");
    private String border = null;
    @SerializedName("title_bar") private TitleBarDef titleBar;
    @SerializedName("duration_ms") private int durationMs;
    @SerializedName("fade_ms") private int fadeMs = 500;
    @SerializedName("pos_x") private int posX = -1;
    @SerializedName("pos_y") private int posY = -1;
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