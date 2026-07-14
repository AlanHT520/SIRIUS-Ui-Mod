package net.alan.gui.data.props;

import net.alan.gui.data.style.TextureSet;
import net.alan.gui.widget.ListWidget;

import java.util.List;

public record ListProps(
    int gap,
    SearchDef search,
    ScrollbarDef scrollbar,
    String backgroundColor,
    TextureSet backgroundTexture,
    List<ListWidget.RowDef> rows
) {

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
        ThumbDef thumb
    ) {}

    public record TrackDef(
        TextureSet texture,
        String color
    ) {}

    public record ThumbDef(
        TextureSet texture,
        String color
    ) {}
}