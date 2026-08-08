package net.alan.gui.render.card;

public class BackgroundDef {
    private String color;
    private String texture;
    private String textureMode = "stretch";

    public BackgroundDef() {}

    public BackgroundDef(String color) {
        this.color = color;
    }

    public boolean hasTexture() {
        return texture != null && !texture.isBlank();
    }

    public boolean hasColor() {
        return color != null && !color.isBlank();
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }
    public String getTextureMode() { return textureMode; }
    public void setTextureMode(String textureMode) { this.textureMode = textureMode; }
}