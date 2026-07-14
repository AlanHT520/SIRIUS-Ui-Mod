package net.alan.gui.data.background;

public class Slide {
    private String texture;
    private int time;
    private String transition;
    private int transitionDuration;

    public Slide() {}

    public String getTexture() { return texture; }
    public void setTexture(String texture) { this.texture = texture; }
    public int getTime() { return time; }
    public void setTime(int time) { this.time = time; }
    public String getTransition() { return transition; }
    public void setTransition(String transition) { this.transition = transition; }
    public int getTransitionDuration() { return transitionDuration; }
    public void setTransitionDuration(int transitionDuration) { this.transitionDuration = transitionDuration; }
}