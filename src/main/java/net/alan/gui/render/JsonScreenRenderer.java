package net.alan.gui.render;

import com.google.gson.JsonElement;
import net.alan.gui.context.RenderContext;
import net.alan.gui.context.WidgetDimensionRegistry;
import net.alan.gui.data.background.BackgroundLayer;
import net.alan.gui.data.background.PanoramaConfig;
import net.alan.gui.data.config.ScreenConfig;
import net.alan.gui.data.config.ScreenLayout;
import net.alan.gui.data.props.LayoutProps;
import net.alan.gui.factory.WidgetFactory;
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

    public JsonScreenRenderer(Minecraft minecraft, Screen parentScreen, ScreenLayout layout, String screenId) {
        this.minecraft = minecraft;
        this.screenId = screenId;
        this.backgroundRenderer = new BackgroundRenderer(minecraft);

        ScreenConfig config = layout.getScreen();
        this.panoramaConfig = config.getPanoramaConfig();
        this.backgroundLayers = config.getBackgrounds();

        // 创建 ActionExecutor 并构建 Widget 树
        ActionExecutor executor = new ActionExecutor(minecraft, parentScreen);
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
        this.renderContext = new RenderContext(sw, sh, screenVariables, screenMembers);
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

        // 合并动态变量到 renderContext（每帧刷新，确保值最新）
        Map<String, String> mergedVars = new HashMap<>(renderContext.variables());
        if (!dynamicVars.isEmpty()) mergedVars.putAll(dynamicVars);
        if (renderContext.screenWidth() != sw || renderContext.screenHeight() != sh) {
            renderContext = new RenderContext(sw, sh, mergedVars, renderContext.screenMembers());
        } else {
            renderContext = new RenderContext(sw, sh, mergedVars, renderContext.screenMembers());
        }

        backgroundRenderer.render(graphics, sw, sh, panoramaConfig, backgroundLayers, delta);

        if (rootWidget != null) {
            rootWidget.render(graphics, 0, 0, sw, sh, renderContext, mouseX, mouseY, delta);
            rootWidget.mouseMoved(mouseX, mouseY, renderContext, 0, 0, sw, sh);
            if (!dimensionsRegistered) {
                registerWidgetDimensions();
                dimensionsRegistered = true;
            }
        }
    }

    private void registerWidgetDimensions() {
        collectWidgetDimensions(rootWidget, 0, 0,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    private void collectWidgetDimensions(Widget widget, int parentX, int parentY,
                                         int parentW, int parentH) {
        if (widget == null) return;
        WidgetDimension dim = widget.computeLayout(renderContext, parentW, parentH);
        String id = widget.getId();
        if (id != null && !id.isEmpty() && !"root".equals(id)) {
            WidgetDimensionRegistry.registerScreenWidget(screenId, id,
                    new WidgetDimension(dim.x, dim.y, dim.w, dim.h));
        }
        for (Widget child : widget.getChildren()) {
            collectWidgetDimensions(child, dim.x, dim.y, dim.w, dim.h);
        }
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseClicked(mx, my, btn, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseReleased(mx, my, btn, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseScrolled(mx, my, sx, sy, renderContext, 0, 0, sw, sh);
    }

    public boolean mouseDragged(double mx, double my, int btn, double dragX, double dragY) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.mouseDragged(mx, my, btn, dragX, dragY, renderContext, 0, 0, sw, sh);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.keyPressed(keyCode, scanCode, modifiers, renderContext, 0, 0, sw, sh);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (rootWidget == null) return false;
        int sw = minecraft.getWindow().getGuiScaledWidth();
        int sh = minecraft.getWindow().getGuiScaledHeight();
        return rootWidget.charTyped(codePoint, modifiers, renderContext, 0, 0, sw, sh);
    }
}