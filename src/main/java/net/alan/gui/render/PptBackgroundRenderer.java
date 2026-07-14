package net.alan.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.alan.gui.data.background.PanoramaConfig;
import net.alan.gui.data.background.Slide;
import net.alan.gui.data.background.SlideGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PptBackgroundRenderer implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_BG_WIDTH = 1920;
    private static final int DEFAULT_BG_HEIGHT = 1080;

    private final Minecraft minecraft;
    private final PanoramaConfig config;
    private final List<String> playSequence;
    private final Map<String, SlideGroup> groupMap;
    private final int defaultTime;
    private int groupIndex;
    private SlideGroup currentGroup;
    private int slideIndex;
    private int currentLoopCount;
    private long slideStartTime;
    private ResourceLocation previousTexture;
    private boolean finished;

    public PptBackgroundRenderer(Minecraft minecraft, PanoramaConfig config) {
        this.minecraft = minecraft;
        this.config = config;
        this.groupMap = new LinkedHashMap<>();
        List<SlideGroup> groups = config.getGroups();
        if (groups != null) {
            for (SlideGroup g : groups) {
                if (g.getId() != null) groupMap.put(g.getId(), g);
            }
        }
        this.playSequence = new ArrayList<>();
        if (config.getPlayGroups() != null && !config.getPlayGroups().isEmpty()) {
            for (String id : config.getPlayGroups()) {
                if (groupMap.containsKey(id)) playSequence.add(id);
                else LOGGER.warn("playGroups references unknown group id: {}", id);
            }
        } else {
            if (groups != null) {
                for (SlideGroup g : groups) {
                    if (g.getId() != null && groupMap.containsKey(g.getId())) {
                        playSequence.add(g.getId());
                    }
                }
            }
        }
        this.defaultTime = config.getDefaultTime();
        this.groupIndex = 0;
        this.slideIndex = 0;
        this.currentLoopCount = 0;
        this.currentGroup = playSequence.isEmpty() ? null : groupMap.get(playSequence.get(0));
        this.slideStartTime = System.currentTimeMillis();
        this.previousTexture = null;
        this.finished = false;
        if (currentGroup != null && (currentGroup.getSlides() == null || currentGroup.getSlides().isEmpty())) {
            LOGGER.warn("Current group '{}' has no slides, skipping to next group.", currentGroup.getId());
            advanceGroup();
        }
    }

    public void render(GuiGraphics graphics, float delta, int screenWidth, int screenHeight) {
        if (finished || currentGroup == null) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }
        updatePlayback();
        if (finished || currentGroup == null) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }
        List<Slide> slides = currentGroup.getSlides();
        if (slides == null || slides.isEmpty()) {
            if (!advanceGroup()) finished = true;
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }
        if (slideIndex >= slides.size()) slideIndex = 0;
        Slide currentSlide = slides.get(slideIndex);
        String texturePath = currentSlide.getTexture();
        if (texturePath == null || texturePath.isEmpty()) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }
        ResourceLocation textureId = ResourceLocation.tryParse(texturePath);
        if (textureId == null) {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);
            return;
        }

        String transition = currentSlide.getTransition();
        int transDuration = currentSlide.getTransitionDuration();
        long elapsed = System.currentTimeMillis() - slideStartTime;

        if (transition != null && !transition.isEmpty() && transDuration > 0 && elapsed < transDuration) {
            float progress = Math.min(1.0f, (float) elapsed / transDuration);
            renderWithTransition(graphics, textureId, screenWidth, screenHeight, transition, progress);
        } else {
            renderScaled(graphics, textureId, screenWidth, screenHeight);
        }
    }

    private void renderWithTransition(GuiGraphics graphics, ResourceLocation texture, int sw, int sh,
                                       String transition, float progress) {
        int[] dims = calcDims(sw, sh);
        int rw = dims[0], rh = dims[1], ox = dims[2], oy = dims[3];
        ResourceLocation prev = previousTexture;

        RenderSystem.enableBlend();

        switch (transition) {
            case "fade" -> {
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox, oy, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, progress);
                graphics.blit(texture, ox, oy, 0, 0, rw, rh, rw, rh);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            case "slide_left" -> {
                int offset = (int) (progress * sw);
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox - offset, oy, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, ox + sw - offset, oy, 0, 0, rw, rh, rw, rh);
            }
            case "slide_right" -> {
                int offset = (int) (progress * sw);
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox + offset, oy, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, ox - sw + offset, oy, 0, 0, rw, rh, rw, rh);
            }
            case "slide_up" -> {
                int offset = (int) (progress * sh);
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox, oy - offset, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, ox, oy + sh - offset, 0, 0, rw, rh, rw, rh);
            }
            case "slide_down" -> {
                int offset = (int) (progress * sh);
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox, oy + offset, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, ox, oy - sh + offset, 0, 0, rw, rh, rw, rh);
            }
            case "zoom_in" -> {
                if (prev != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    graphics.blit(prev, ox, oy, 0, 0, rw, rh, rw, rh);
                } else {
                    graphics.fill(0, 0, sw, sh, 0xFF000000);
                }
                float s = progress;
                int dw = (int) (rw * s), dh = (int) (rh * s);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, (sw - dw) / 2, (sh - dh) / 2, 0, 0, dw, dh, rw, rh);
            }
            case "zoom_out" -> {
                graphics.fill(0, 0, sw, sh, 0xFF000000);
                float s = 2.0f - progress;
                int dw = (int) (rw * s), dh = (int) (rh * s);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, (sw - dw) / 2, (sh - dh) / 2, 0, 0, dw, dh, rw, rh);
            }
            default -> {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                graphics.blit(texture, ox, oy, 0, 0, rw, rh, rw, rh);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private int[] calcDims(int sw, int sh) {
        float texAspect = (float) DEFAULT_BG_WIDTH / DEFAULT_BG_HEIGHT;
        float screenAspect = (float) sw / sh;
        int rw, rh;
        if (screenAspect > texAspect) { rw = sw; rh = (int)(sw / texAspect); }
        else { rh = sh; rw = (int)(sh * texAspect); }
        int ox = (sw - rw) / 2, oy = (sh - rh) / 2;
        return new int[]{rw, rh, ox, oy};
    }

    private void updatePlayback() {
        if (finished || currentGroup == null) return;
        int duration = getCurrentSlideDuration();
        long now = System.currentTimeMillis();
        if (now - slideStartTime >= duration) {
            boolean advanced = advanceSlide();
            if (!advanced) {
                if (!advanceGroup()) finished = true;
                else slideStartTime = System.currentTimeMillis();
            } else {
                slideStartTime = System.currentTimeMillis();
            }
        }
    }

    private boolean advanceSlide() {
        if (currentGroup == null) return false;
        List<Slide> slides = currentGroup.getSlides();
        if (slides == null || slides.isEmpty()) return false;

        if (slideIndex < slides.size()) {
            Slide cur = slides.get(slideIndex);
            if (cur.getTexture() != null && !cur.getTexture().isEmpty()) {
                previousTexture = ResourceLocation.tryParse(cur.getTexture());
            }
        }

        int nextSlide = slideIndex + 1;
        if (nextSlide < slides.size()) {
            slideIndex = nextSlide;
            return true;
        } else {
            currentLoopCount++;
            int playCount = currentGroup.getPlayCount();
            if (playCount == -1) { slideIndex = 0; return true; }
            if (currentLoopCount >= playCount) return false;
            else { slideIndex = 0; return true; }
        }
    }

    private boolean advanceGroup() {
        if (currentGroup != null && currentGroup.getId().equals(config.getPlayAfter())) {
            currentLoopCount = 0;
            slideIndex = 0;
            return true;
        }
        if (groupIndex + 1 >= playSequence.size()) {
            finished = true;
            return false;
        }
        groupIndex++;
        String nextGroupId = playSequence.get(groupIndex);
        SlideGroup nextGroup = groupMap.get(nextGroupId);
        if (nextGroup == null) { finished = true; return false; }
        this.currentGroup = nextGroup;
        this.slideIndex = 0;
        this.currentLoopCount = 0;
        return true;
    }

    private int getCurrentSlideDuration() {
        List<Slide> slides = currentGroup.getSlides();
        if (slides == null || slideIndex >= slides.size()) return defaultTime;
        Slide slide = slides.get(slideIndex);
        return slide.getTime() > 0 ? slide.getTime() : defaultTime;
    }

    private void renderScaled(GuiGraphics graphics, ResourceLocation texture, int sw, int sh) {
        int[] dims = calcDims(sw, sh);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, dims[2], dims[3], 0, 0, dims[0], dims[1], dims[0], dims[1]);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override public void close() {}
    public boolean isFinished() { return finished; }
}