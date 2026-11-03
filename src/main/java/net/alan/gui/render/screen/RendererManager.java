

package net.alan.gui.render.screen;
import net.minecraft.client.gui.screens.Screen;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class RendererManager {
    private static final Map<Screen, HtScreenRenderer> RENDERERS = new ConcurrentHashMap<>();
    public static void register(Screen screen, HtScreenRenderer renderer) {
        RENDERERS.put(screen, renderer);
    }
    public static void unregister(Screen screen) { RENDERERS.remove(screen); }
    public static HtScreenRenderer getRenderer(Screen screen) { return RENDERERS.get(screen); }
}
