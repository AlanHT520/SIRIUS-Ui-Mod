package net.alan.gui.data.widget;

import net.alan.gui.data.widget.TextureSet;
import net.alan.gui.widget.ListWidget;

import java.util.List;

public record ListProps(
    int gap,
    SearchDef search,
    ScrollbarDef scrollbar,
    String backgroundColor,
    TextureSet backgroundTexture,
    List<ListWidget.RowDef> rows,
    int scrollSpeed
) {

    public int scrollSpeed() {
        return scrollSpeed > 0 ? scrollSpeed : 36;
    }

    public record SearchDef(
        int maxLength,
        boolean bordered,
        String hint,
        String textColor,
        String initialValue,
        String x,
        String y,
        String width,
        String height
    ) {}

    public record ScrollbarDef(
        int width,
        String x,
        String y,
        TrackDef track,
        ThumbDef thumb,
        int minHeight,
        int padding
    ) {
        public int width() {
            return width > 0 ? width : 6;
        }

        public int minHeight() {
            return minHeight > 0 ? minHeight : 16;
        }

        public int padding() {
            return padding > 0 ? padding : 8;
        }
    }

    public record TrackDef(
        TextureSet texture,
        String color
    ) {}

    public record ThumbDef(
        TextureSet texture,
        String color
    ) {}
}