package net.alan.gui.render.card;

import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public class TitleBarDef {
    private int height = 28;
    private boolean draggable = true;
    private BackgroundDef background = new BackgroundDef();
    private List<JsonElement> elements = new ArrayList<>();

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public boolean isDraggable() { return draggable; }
    public void setDraggable(boolean draggable) { this.draggable = draggable; }
    public BackgroundDef getBackground() { return background; }
    public void setBackground(BackgroundDef background) { this.background = background; }
    public List<JsonElement> getElements() { return elements; }
    public void setElements(List<JsonElement> elements) { this.elements = elements; }
}