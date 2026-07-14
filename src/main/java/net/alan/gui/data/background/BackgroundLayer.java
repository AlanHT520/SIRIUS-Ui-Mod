package net.alan.gui.data.background;

public class BackgroundLayer {
    private String x;
    private String y;
    private String width;
    private String height;
    private String color;

    public BackgroundLayer() {}

    public String getX() { return x != null ? x : "0"; }
    public String getY() { return y != null ? y : "0"; }
    public String getWidth() { return width != null ? width : "0"; }
    public String getHeight() { return height != null ? height : "0"; }
    public String getColor() { return color != null ? color : "0xFF000000"; }
}