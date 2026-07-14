package net.alan.gui.data.config;

import com.google.gson.JsonElement;
import net.alan.gui.data.background.BackgroundLayer;
import net.alan.gui.data.background.PanoramaConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScreenConfig {
    private String parent;
    private PanoramaConfig panorama;
    private List<BackgroundLayer> graphicsDraw;
    private List<JsonElement> elements;
    private Map<String, String> variables = new LinkedHashMap<>();
    private Map<String, String> member = new LinkedHashMap<>();

    public ScreenConfig() {}

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }
    public PanoramaConfig getPanoramaConfig() { return panorama; }
    public void setPanorama(PanoramaConfig panorama) { this.panorama = panorama; }
    public List<BackgroundLayer> getBackgrounds() { return graphicsDraw; }
    public void setBackgrounds(List<BackgroundLayer> graphicsDraw) { this.graphicsDraw = graphicsDraw; }
    public List<JsonElement> getElements() { return elements; }
    public void setElements(List<JsonElement> elements) { this.elements = elements; }
    public Map<String, String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }
    public Map<String, String> getMember() { return member; }
    public void setMember(Map<String, String> member) { this.member = member; }
}