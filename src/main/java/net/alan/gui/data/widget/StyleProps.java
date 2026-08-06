package net.alan.gui.data.widget;

import net.alan.gui.data.widget.TextureSet;

public record StyleProps(
        TextureSet texture,
        String backgroundColor
) {
    public StyleProps() { this(null, null); }
}