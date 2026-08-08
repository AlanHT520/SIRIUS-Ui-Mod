package net.alan.gui.render.screen;

import com.google.gson.JsonElement;
import net.alan.gui.context.RenderContext;
import net.alan.gui.context.WidgetDimensionRegistry;
import net.alan.gui.data.screen.BackgroundLayer;
import net.alan.gui.data.screen.PanoramaConfig;
import net.alan.gui.data.screen.ScreenConfig;
import net.alan.gui.data.screen.ScreenLayout;
import net.alan.gui.data.widget.LayoutProps;
import net.alan.gui.render.ActionExecutor;
import net.alan.gui.render.card.CardManager;
import net.alan.gui.widget.WidgetFactory;
import net.alan.gui.widget.ContainerWidget;
import net.alan.gui.widget.Widget;
import net.alan.gui.widget.BaseWidget.WidgetDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonScreenRenderer {
    private final Minecraft minecraft;
    private final BackgroundRenderer backgroundRenderer;
    private final PanoramaConfig panoramaConfig;
    private final List<BackgroundLayer> backgroundLayers;
    private final Widget rootWidget;
    private final String screenId;
    private RenderContext renderContext;
    private final Map<String, String> dynamicVars;
    private boolean dimensionsRegistered;
    private final CardManager cardManager;

    public JsonScreenRenderer(Minecraft minecraft, Screen parentScreen, ScreenLayout layout, String screenId) {
        this.minecraft = minecraft;
        this.screenId = screenId;
        this.backgroundRenderer = new BackgroundRenderer(minecraft);

        ScreenConfig config = layout.getScreen();
        this.panoramaConfig = config.getPanoramaConfig();
        this.backgroundLayers = config.getBackgrounds();

        Map<String, String> sharedState = new HashMap<>();
        if (config.getSharedState() != null) {
            sharedState.putAll(config.getSharedState());
        }

        ActionExecutor executor = new ActionExecutor(minecraft, parentScreen);
        executor.setSharedState(sharedState);
        this.cardManager = new CardManager(minecraft, executor);
        executor.setCardManager(this.cardManager);
        List<Widget> widgets = new ArrayList<>();
        List<JsonElement> elements = config.getElements();
        if (elements != null) {
            for (JsonElement elem : elements) {
                if (elem.isJsonObject()) {
                    Widget w = WidgetFactory.create(elem.getAsJsonObject(),
                            minecraft.getResourceManager(), executor);
                    if (w != null) widgets.add(w);
                }
            }
        }

        this.rootWidget = new ContainerWidget("root",
                new LayoutProps("0", "0", "screen.width", "screen.height", true, true),
                new HashMap<>(),
                new HashMap<>(),
                widgets
        );

        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        this.dynamicVars = new HashMap<>();
        Map<String, String> screenVariables = config.getVariables() != null
                ? config.getVariables() : new HashMap<>();
        Map<String, String> screenMembers = config.getMember() != null
                ? config.getMember() : Map.of();
        this.renderContext = new RenderContext(sw, sh, screenVariables, screenMembers, sharedState);
    }

    public void putDynamicVar(String key, String value) {
        this.dynamicVars.put(key, value);
    }

    public void setDynamicVars(Map<String, String> vars) {
        this.dynamicVars.clear();
        if (vars != null) this.dynamicVars.putAll(vars);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();

        renderContext.setScreenSize(sw, sh);
        if (!dynamicVars.isEmpty()) {
            renderContext.putAllVars(dynamicVars);
        }

        backgroundRenderer.render(graphics, sw, sh, panoramaConfig, backgroundLayers, delta);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, sw, sh, renderContext, mouseX, mouseY, delta);
            if (!cardManager.isModalActive()) {
                rootWidget.mouseMoved(mouseX, mouseY, renderContext, 0, 0, sw, sh);
            }
            if (!dimensionsRegistered) {
                registerWidgetDimensions();
                dimensionsRegistered = true;
            }
        }

        cardManager.render(graphics, mouseX, mouseY, delta);
    }

    private void registerWidgetDimensions() {
        collectWidgetDimensions(rootWidget, 0, 0,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                renderContext);
    }

    private void collectWidgetDimensions(Widget widget, int parentX, int parentY,
                                         int parentW, int parentH, RenderContext ctx) {
        if (widget == null) return;
        ctx = widget.mergeRenderContext(ctx);
        WidgetDimension dim = widget.computeLayout(ctx, parentW, parentH);
        String id = widget.getId();
        if (id != null && !id.isEmpty() && !"root".equals(id)) {
            WidgetDimensionRegistry.registerScreenWidget(screenId, id,
                    new WidgetDimension(dim.x, dim.y, dim.w, dim.h));
        }
        for (Widget child : widget.getChildren()) {
            collectWidgetDimensions(child, dim.x, dim.y, dim.w, dim.h, ctx);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (cardManager.mouseClicked(mx, my, btn)) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseClicked(mx, my, btn, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        if (cardManager.mouseReleased(mx, my, btn)) return true;
        if (cardManager.isModalActive()) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseReleased(mx, my, btn, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (cardManager.isModalActive()) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseScrolled(mx, my, sx, sy, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseDragged(double mx, double my, int btn, double dragX, double dragY) {
        if (cardManager.mouseDragged(mx, my, btn, dragX, dragY)) return true;
        if (cardManager.isModalActive()) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseDragged(mx, my, btn, dragX, dragY, renderContext, 0, 0, sw, sh);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (cardManager.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.keyPressed(keyCode, scanCode, modifiers, renderContext, 0, 0, sw, sh);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (cardManager.charTyped(codePoint, modifiers)) return true;
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.charTyped(codePoint, modifiers, renderContext, 0, 0, sw, sh);
    }
}