package net.alan.gui.data.background;

import java.util.List;

public class SlideGroup {
    private String id;
    private int playCount = 1;
    private List<Slide> slides;

    public SlideGroup() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public List<Slide> getSlides() { return slides; }
    public void setSlides(List<Slide> slides) { this.slides = slides; }
}