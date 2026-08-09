package net.alan.gui.widget;

import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.alan.gui.context.RenderContext;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.render.screen.BackgroundRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.Util;

public class SplashWidget extends BaseWidget {
    private final SplashConfig config;
    private final Minecraft minecraft;
    private final Random random = new Random();
    private String splashText;
    private boolean initialized;

    public SplashWidget(String id, LayoutProps layout, Map<String, String> variables,
                        Map<String, String> member, SplashConfig config) {
        super(id, layout, variables, member);
        this.config = config;
        this.minecraft = Minecraft.getInstance();
    }

    private void initSplash() {
        if (initialized) return;
        initialized = true;

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        if (config.dateSplashes != null) {
            List<WeightedText> matches = new ArrayList<>();
            for (DateSplash ds : config.dateSplashes) {
                if (ds.month == month && ds.day == day) {
                    matches.add(new WeightedText(ds.text, ds.weight));
                }
            }
            if (!matches.isEmpty()) {
                splashText = pickWeighted(matches);
                return;
            }
        }

        if (config.easterEgg != null && random.nextFloat() < config.easterEgg.probability) {
            String playerName = minecraft.getUser() != null ? minecraft.getUser().getName() : "Player";
            splashText = config.easterEgg.text.replace("{player}", playerName);
            return;
        }

        List<WeightedText> pool = new ArrayList<>();
        if (config.splashes != null) {
            pool.addAll(config.splashes);
        }
        if (config.useVanilla) {
            List<String> vanilla = loadVanillaSplashes();
            for (String s : vanilla) {
                pool.add(new WeightedText(s, 1));
            }
        }
        if (!pool.isEmpty()) {
            splashText = pickWeighted(pool);
        }
    }

    private String pickWeighted(List<WeightedText> items) {
        int totalWeight = 0;
        for (WeightedText wt : items) {
            totalWeight += wt.weight;
        }
        if (totalWeight <= 0) return items.get(0).text;
        int r = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedText wt : items) {
            cumulative += wt.weight;
            if (r < cumulative) {
                return wt.text;
            }
        }
        return items.get(items.size() - 1).text;
    }

    private List<String> loadVanillaSplashes() {
        List<String> all = new ArrayList<>();
        try {
            var rm = minecraft.getResourceManager();
            var location = new ResourceLocation("texts", "splashes.txt");
            for (Resource resource : rm.getResourceStack(location)) {
                try (BufferedReader reader = resource.openAsReader()) {
                    reader.lines()
                        .map(String::trim)
                        .filter(s -> s.hashCode() != 125780783)
                        .forEach(all::add);
                }
            }
        } catch (IOException ignored) {
        }
        return all;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       RenderContext context, int mouseX, int mouseY, float delta) {
        if (!layout.visible()) return;

        RenderContext mergedCtx = mergeContext(context);
        WidgetDimension dim = computeLayout(mergedCtx, width, height);
        Map<String, Integer> vars = buildNumericVars(mergedCtx, width, height, dim.w, dim.h);
        if (!checkCondition(vars)) return;

        if (!initialized) initSplash();
        if (splashText == null || splashText.isEmpty()) return;

        if (false) return;

        int screenX = x + dim.x;
        int screenY = y + dim.y;

        int splashColor = 16776960;
        if (config.color != null) {
            String colorStr = replaceVars(config.color, mergedCtx.variables());
            splashColor = BackgroundRenderer.parseColor(colorStr);
        }

        String[] lines = splashText.split("\\n");

        int maxWidth = 0;
        for (String line : lines) {
            int w = minecraft.font.width(line);
            if (w > maxWidth) maxWidth = w;
        }

        float pulseScale = config.pulseBaseScale
            - Mth.abs(Mth.sin((float) (Util.getMillis() % 1000L) / 1000.0F
                * (float) (Math.PI * 2) / config.pulseSpeed) * config.pulseAmplitude);
        float normalizedScale = pulseScale * 100.0F / (float) (maxWidth + 32);

        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(config.rotation));
        graphics.pose().scale(normalizedScale, normalizedScale, 1.0F);

        int lineHeight = minecraft.font.lineHeight;
        int totalHeight = (lines.length - 1) * lineHeight;
        int baseY = -8 - totalHeight / 2;

        for (int i = 0; i < lines.length; i++) {
            graphics.drawCenteredString(minecraft.font, lines[i], 0, baseY + i * lineHeight, splashColor);
        }

        graphics.pose().popPose();
    }

    public static class SplashConfig {
        public boolean useVanilla = true;
        public List<WeightedText> splashes;
        public List<DateSplash> dateSplashes;
        public EasterEgg easterEgg;
        public String color = "0xFFFF00";
        public float rotation = -20.0F;
        public float pulseBaseScale = 1.8F;
        public float pulseAmplitude = 0.1F;
        public float pulseSpeed = 1.0F;
    }

    public static class WeightedText {
        public String text;
        public int weight = 1;

        public WeightedText() {}
        public WeightedText(String text, int weight) {
            this.text = text;
            this.weight = weight;
        }
    }

    public static class DateSplash {
        public int month;
        public int day;
        public String text;
        public int weight = 1;
    }

    public static class EasterEgg {
        public float probability = 0.0238F;
        public String text = "{player} IS YOU";
    }
}