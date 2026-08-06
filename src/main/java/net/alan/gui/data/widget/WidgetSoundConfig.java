package net.alan.gui.data.widget;

public class WidgetSoundConfig {
    private SoundEventConfig click;
    private SoundEventConfig hover;
    private SoundEventConfig valueChange;
    private SoundEventConfig dragStart;
    private SoundEventConfig dragEnd;
    private SoundEventConfig focus;
    private SoundEventConfig release;

    public WidgetSoundConfig() {
    }

    public SoundEventConfig getClick() {
        return click;
    }

    public void setClick(SoundEventConfig click) {
        this.click = click;
    }

    public SoundEventConfig getHover() {
        return hover;
    }

    public void setHover(SoundEventConfig hover) {
        this.hover = hover;
    }

    public SoundEventConfig getValueChange() {
        return valueChange;
    }

    public void setValueChange(SoundEventConfig valueChange) {
        this.valueChange = valueChange;
    }

    public SoundEventConfig getDragStart() {
        return dragStart;
    }

    public void setDragStart(SoundEventConfig dragStart) {
        this.dragStart = dragStart;
    }

    public SoundEventConfig getDragEnd() {
        return dragEnd;
    }

    public void setDragEnd(SoundEventConfig dragEnd) {
        this.dragEnd = dragEnd;
    }

    public SoundEventConfig getFocus() {
        return focus;
    }

    public void setFocus(SoundEventConfig focus) {
        this.focus = focus;
    }

    public SoundEventConfig getRelease() {
        return release;
    }

    public void setRelease(SoundEventConfig release) {
        this.release = release;
    }

    public boolean hasClick() {
        return click != null && click.isValid();
    }

    public boolean hasHover() {
        return hover != null && hover.isValid();
    }

    public boolean hasValueChange() {
        return valueChange != null && valueChange.isValid();
    }

    public boolean hasDragStart() {
        return dragStart != null && dragStart.isValid();
    }

    public boolean hasDragEnd() {
        return dragEnd != null && dragEnd.isValid();
    }

    public boolean hasFocus() {
        return focus != null && focus.isValid();
    }

    public boolean hasRelease() {
        return release != null && release.isValid();
    }
}