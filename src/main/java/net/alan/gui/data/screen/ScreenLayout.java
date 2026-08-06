package net.alan.gui.data.screen;

public class ScreenLayout {
    private ScreenConfig screen;

    public ScreenLayout() {}
    public ScreenLayout(ScreenConfig screen) { this.screen = screen; }

    public ScreenConfig getScreen() { return screen; }
    public void setScreen(ScreenConfig screen) { this.screen = screen; }
}