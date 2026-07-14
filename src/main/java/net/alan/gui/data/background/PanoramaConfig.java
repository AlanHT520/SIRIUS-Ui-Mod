package net.alan.gui.data.background;

import java.util.List;

public class PanoramaConfig {
    private String type;
    private String texture;
    private int defaultTime = 3000;
    private List<String> playGroups;
    private String playAfter;
    private List<SlideGroup> groups;

    public PanoramaConfig() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }
    public int getDefaultTime() { return defaultTime; }
    public void setDefaultTime(int defaultTime) { this.defaultTime = defaultTime; }
    public List<String> getPlayGroups() { return playGroups; }
    public void setPlayGroups(List<String> playGroups) { this.playGroups = playGroups; }
    public String getPlayAfter() { return playAfter; }
    public void setPlayAfter(String playAfter) { this.playAfter = playAfter; }
    public List<SlideGroup> getGroups() { return groups; }
    public void setGroups(List<SlideGroup> groups) { this.groups = groups; }
}