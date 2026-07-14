package net.alan.gui.context;

import java.util.HashMap;
import java.util.Map;

public record RenderContext(
        int screenWidth,
        int screenHeight,
        Map<String, String> variables,
        Map<String, String> screenMembers
) {
    /** 向后兼容的构造函数 */
    public RenderContext(int screenWidth, int screenHeight, Map<String, String> variables) {
        this(screenWidth, screenHeight, variables, Map.of());
    }

    public RenderContext withVars(Map<String, String> newVars) {
        Map<String, String> merged = new HashMap<>(variables);
        if (newVars != null) merged.putAll(newVars);
        return new RenderContext(screenWidth, screenHeight, merged, screenMembers);
    }

    public RenderContext withVar(String key, String value) {
        Map<String, String> merged = new HashMap<>(variables);
        merged.put(key, value);
        return new RenderContext(screenWidth, screenHeight, merged, screenMembers);
    }

    public RenderContext withScreenMembers(Map<String, String> screenMembers) {
        return new RenderContext(screenWidth, screenHeight, variables, screenMembers);
    }
}