package net.alan.gui.data.geometry;

public class Size {
    private String width;
    private String height;

    public Size() {}
    public Size(String width, String height) { this.width = width; this.height = height; }

    public static Size of(int width, int height) { return new Size(String.valueOf(width), String.valueOf(height)); }

    public String getWidth() { return width; }
    public void setWidth(String width) { this.width = width; }
    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }
}