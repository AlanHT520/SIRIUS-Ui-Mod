package net.alan.gui.data.props;

import net.alan.gui.data.style.TextureSet;

public record StyleProps(
        TextureSet texture,
        String backgroundColor
) {
    public StyleProps() { this(null, null); }
}