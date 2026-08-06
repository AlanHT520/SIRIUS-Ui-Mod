package net.alan.gui.context;

import java.util.HashMap;
import java.util.Map;

public class RenderContext {
    private int screenWidth;
    private int screenHeight;
    private final Map<String, String> variables;
    private Map<String, String> screenMembers;
    private Map<String, String> sharedState;

    public RenderContext(int screenWidth, int screenHeight, Map<String, String> variables) {
        this(screenWidth, screenHeight, variables, Map.of(), null);
    }

    public RenderContext(int screenWidth, int screenHeight, Map<String, String> variables, Map<String, String> screenMembers) {
        this(screenWidth, screenHeight, variables, screenMembers, null);
    }

    public RenderContext(int screenWidth, int screenHeight, Map<String, String> variables, Map<String, String> screenMembers, Map<String, String> sharedState) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.screenMembers = screenMembers != null ? screenMembers : Map.of();
        this.sharedState = sharedState;
    }

    public int screenWidth() { return screenWidth; }
    public int screenHeight() { return screenHeight; }
    public Map<String, String> variables() { return variables; }
    public Map<String, String> screenMembers() { return screenMembers; }
    public Map<String, String> sharedState() { return sharedState; }

    public void setScreenSize(int w, int h) {
        this.screenWidth = w;
        this.screenHeight = h;
    }

    public void setScreenMembers(Map<String, String> members) {
        this.screenMembers = members != null ? members : Map.of();
    }

    public void replaceVariables(Map<String, String> newVars) {
        variables.clear();
        if (newVars != null) variables.putAll(newVars);
    }

    public void putAllVars(Map<String, String> newVars) {
        if (newVars != null) variables.putAll(newVars);
    }

    public RenderContext withVar(String key, String value) {
        variables.put(key, value);
        return this;
    }

    public RenderContext withVars(Map<String, String> newVars) {
        if (newVars != null) variables.putAll(newVars);
        return this;
    }

    public RenderContext withScreenMembers(Map<String, String> screenMembers) {
        this.screenMembers = screenMembers != null ? screenMembers : Map.of();
        return this;
    }

    public RenderContext copy() {
        return new RenderContext(screenWidth, screenHeight, new HashMap<>(variables), screenMembers, sharedState);
    }
}