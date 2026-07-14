package net.alan.gui.render;

import net.alan.gui.data.background.BackgroundLayer;
import net.alan.gui.data.background.PanoramaConfig;
import net.alan.gui.data.background.Slide;
import net.alan.gui.data.background.SlideGroup;
import net.alan.gui.util.ExpressionEvaluator;
import net.alan.gui.util.GameStateProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundRenderer.class);
    private static final int DEFAULT_BG_WIDTH = 1920;
    private static final int DEFAULT_BG_HEIGHT = 1080;
    private final Minecraft minecraft;
    private static PptBackgroundRenderer sharedPptRenderer;
    private static String sharedPptKey;
    private static final PanoramaRenderer PANORAMA;

    static {
        PanoramaRenderer panorama = null;
        try {
            java.lang.reflect.Field field = Screen.class.getDeclaredField("PANORAMA");
            field.setAccessible(true);
            panorama = (PanoramaRenderer) field.get(null);
        } catch (Exception e) {
            LOGGER.error("Could not access Screen.PANORAMA", e);
        }
        PANORAMA = panorama;
    }

    public BackgroundRenderer(Minecraft minecraft) { this.minecraft = minecraft; }

    public void render(GuiGraphics graphics, int screenWidth, int screenHeight,
                       PanoramaConfig bgConfig, List<BackgroundLayer> layers, float delta) {
        if (bgConfig != null) {
            String rawType = bgConfig.getType();
            String type = evalStringExpr(rawType, screenWidth, screenHeight);
            switch (type) {
                case "ppt" -> renderPpt(graphics, delta, screenWidth, screenHeight, bgConfig);
                case "vanilla" -> {
                    if (minecraft.level == null) {
                        if (PANORAMA != null) PANORAMA.render(graphics, screenWidth, screenHeight, 1.0F, delta);
                        graphics.fill(0, 0, screenWidth, screenHeight, 0x40000000);
                    }
                }
                case "image" -> {
                    String rawPath = bgConfig.getTexture();
                    String path = evalStringExpr(rawPath, screenWidth, screenHeight);
                    if (path != null && !path.isEmpty()) {
                        try {
                            ResourceLocation id = ResourceLocation.tryParse(path);
                            if (id != null) renderScaled(graphics, id, screenWidth, screenHeight);
                        } catch (Exception e) {
                            LOGGER.error("Failed to render image background {}: {}", path, e.getMessage());
                            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
                        }
                    } else {
                        graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
                    }
                }
                default -> graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            }
        } else {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
        }

        if (layers != null) {
            Map<String, Integer> gameVars = injectGameStateVars();
            for (BackgroundLayer bg : layers) {
                try {
                    int w = ExpressionEvaluator.eval(bg.getWidth(), screenWidth, screenHeight, 0, 0, gameVars);
                    int h = ExpressionEvaluator.eval(bg.getHeight(), screenWidth, screenHeight, 0, 0, gameVars);
                    int x = ExpressionEvaluator.eval(bg.getX(), screenWidth, screenHeight, w, h, gameVars);
                    int y = ExpressionEvaluator.eval(bg.getY(), screenWidth, screenHeight, w, h, gameVars);
                    graphics.fill(x, y, x + w, y + h, parseColor(bg.getColor()));
                } catch (Exception e) {
                    LOGGER.warn("Failed to render background layer: {}", e.getMessage());
                }
            }
        }
    }

    private void renderPpt(GuiGraphics graphics, float delta, int sw, int sh, PanoramaConfig config) {
        String key = pptConfigKey(config);
        if (sharedPptRenderer == null || !key.equals(sharedPptKey)) {
            if (sharedPptRenderer != null) sharedPptRenderer.close();
            sharedPptRenderer = new PptBackgroundRenderer(minecraft, config);
            sharedPptKey = key;
        }
        sharedPptRenderer.render(graphics, delta, sw, sh);
    }

    private static String pptConfigKey(PanoramaConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getType() != null ? config.getType() : "").append("|");
        sb.append(config.getTexture() != null ? config.getTexture() : "").append("|");
        sb.append(config.getDefaultTime()).append("|");
        if (config.getGroups() != null) {
            for (SlideGroup g : config.getGroups()) {
                sb.append(g.getId()).append(":").append(g.getPlayCount()).append("[");
                if (g.getSlides() != null) {
                    for (Slide s : g.getSlides()) {
                        sb.append(s.getTexture()).append(",")
                          .append(s.getTime()).append(",")
                          .append(s.getTransition()).append(",")
                          .append(s.getTransitionDuration()).append(";");
                    }
                }
                sb.append("]");
            }
        }
        return sb.toString();
    }

    private void renderScaled(GuiGraphics graphics, ResourceLocation texture, int sw, int sh) {
        float texAspect = (float) DEFAULT_BG_WIDTH / DEFAULT_BG_HEIGHT;
        float screenAspect = (float) sw / sh;
        int rw, rh;
        if (screenAspect > texAspect) { rw = sw; rh = (int)(sw / texAspect); }
        else { rh = sh; rw = (int)(sh * texAspect); }
        int ox = (sw - rw) / 2, oy = (sh - rh) / 2;
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, ox, oy, 0, 0.0F, 0.0F, rw, rh, rw, rh);
    }

    public static int parseColor(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) return 0xFFFFFFFF;
        String trimmed = colorStr.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) trimmed = trimmed.substring(2);
        else if (trimmed.startsWith("#")) trimmed = trimmed.substring(1);
        try {
            long color = Long.parseLong(trimmed, 16);
            if (trimmed.length() <= 6) color |= 0xFF000000L;
            return (int) color;
        } catch (NumberFormatException e) { return 0xFFFFFFFF; }
    }

    public void close() {
    }

    public static void shutdown() {
        if (sharedPptRenderer != null) {
            sharedPptRenderer.close();
            sharedPptRenderer = null;
            sharedPptKey = null;
        }
    }

    private static Map<String, Integer> injectGameStateVars() {
        Map<String, Integer> vars = new HashMap<>();
        String[] gameKeys = {
            "game.in_level", "game.is_singleplayer", "game.is_multiplayer",
            "game.is_integrated_server", "game.is_dedicated_server",
            "game.is_hardcore", "game.is_flat", "game.is_debug",
            "game.is_demo", "game.is_paused",
            "game.is_creative", "game.is_survival", "game.is_spectator", "game.is_adventure",
            "game.game_mode", "game.difficulty", "game.is_hard_difficulty",
            "game.can_modify_difficulty", "game.is_difficulty_locked",
            "game.has_cheats", "game.is_host", "game.is_op",
            "game.is_connected", "game.is_realm", "game.player_count"
        };
        for (String key : gameKeys) {
            vars.put(key, GameStateProvider.resolve(key));
        }
        return vars;
    }

    private static String evalStringExpr(String expr, int screenWidth, int screenHeight) {
        Map<String, Integer> vars = injectGameStateVars();
        return ExpressionEvaluator.evalString(expr, screenWidth, screenHeight, 0, 0, vars);
    }
}