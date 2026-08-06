package net.alan.gui.data.widget;

public record SliderProps(
        String optionKey,
        double min,
        double max,
        double step
) {
    public SliderProps() { this(null, 0.0, 1.0, 0.01); }
}