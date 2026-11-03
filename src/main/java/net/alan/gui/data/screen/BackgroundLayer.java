

package net.alan.gui.data.screen;
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

    public void setX(String x) { this.x = x; }
    public void setY(String y) { this.y = y; }
    public void setWidth(String width) { this.width = width; }
    public void setHeight(String height) { this.height = height; }
    public void setColor(String color) { this.color = color; }
}
