package net.alan.gui.data.props;

import java.util.List;

public record TextProps(
        String text,
        String textKey,
        String textKeyDynamic,
        List<String> translationArgs,
        String textKeyOption,
        String color,
        String highlightedColor,
        String disabledColor,
        float scale,
        boolean shadow,
        String dynamicType,
        String offsetX,
        String offsetY,
        String highlightedOffsetX,
        String highlightedOffsetY,
        String disabledOffsetX,
        String disabledOffsetY,
        String textAnimation,
        float animationSpeed,
        List<AnimationStep> animations
) {
    public TextProps() {
        this(null, null, null, null, null, "0xFFFFFF", "0xFFFFFF", "0x808080",
                1.0f, false, null, "0", "0", "0", "0", "0", "0", null, 0.0f, null);
    }
}