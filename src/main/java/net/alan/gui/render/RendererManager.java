package net.alan.gui.render;

import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RendererManager {
    private static final Map<Screen, JsonScreenRenderer> RENDERERS = new ConcurrentHashMap<>();

    public static void register(Screen screen, JsonScreenRenderer renderer) {
        RENDERERS.put(screen, renderer);
    }
    public static void unregister(Screen screen) { RENDERERS.remove(screen); }
    public static JsonScreenRenderer getRenderer(Screen screen) { return RENDERERS.get(screen); }
}