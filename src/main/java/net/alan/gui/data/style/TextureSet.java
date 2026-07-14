package net.alan.gui.data.style;

public class TextureSet {
    private String normal;
    private String highlighted;
    private String disabled;
    private String ing;
    private String selected;

    public TextureSet() {}
    public TextureSet(String normal, String highlighted, String disabled) {
        this.normal = normal;
        this.highlighted = highlighted;
        this.disabled = disabled;
    }

    public String getNormal() { return normal; }
    public void setNormal(String normal) { this.normal = normal; }
    public String getHighlighted() { return highlighted; }
    public void setHighlighted(String highlighted) { this.highlighted = highlighted; }
    public String getDisabled() { return disabled; }
    public void setDisabled(String disabled) { this.disabled = disabled; }
    public String getIng() { return ing; }
    public void setIng(String ing) { this.ing = ing; }
    public String getSelected() { return selected; }
    public void setSelected(String selected) { this.selected = selected; }
}