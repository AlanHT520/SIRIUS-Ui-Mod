package net.alan.gui.data;

public record CycleValue(
        String key,
        String textKey
) {
    public CycleValue() { this(null, null); }
}