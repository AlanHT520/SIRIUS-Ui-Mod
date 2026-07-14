package net.alan.gui.context;

import net.alan.gui.widget.BaseWidget.WidgetDimension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WidgetDimensionRegistry {
    private static final Map<String, WidgetDimension> TEMPLATE_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, WidgetDimension>> SCREEN_DIMENSIONS = new ConcurrentHashMap<>();

    private WidgetDimensionRegistry() {}

    public static void registerTemplate(String name, WidgetDimension dim) {
        TEMPLATE_DIMENSIONS.put(name, dim);
    }

    public static void registerScreenWidget(String screenId, String elementId, WidgetDimension dim) {
        SCREEN_DIMENSIONS.computeIfAbsent(screenId, k -> new ConcurrentHashMap<>())
                .put(elementId, dim);
    }

    public static WidgetDimension getTemplate(String name) {
        return TEMPLATE_DIMENSIONS.get(name);
    }

    public static WidgetDimension getScreenWidget(String screenId, String elementId) {
        Map<String, WidgetDimension> screen = SCREEN_DIMENSIONS.get(screenId);
        return screen != null ? screen.get(elementId) : null;
    }

    public static Integer resolveProperty(String dottedName) {
        String[] parts = dottedName.split("\\.");
        if (parts.length < 2) return null;

        String property = parts[parts.length - 1];
        String elementName = parts[parts.length - 2];
        String screenId = parts.length >= 3 ? parts[0] : null;

        WidgetDimension dim = null;
        if (screenId != null) {
            dim = getScreenWidget(screenId, elementName);
        }
        if (dim == null) {
            dim = getTemplate(elementName);
        }

        if (dim == null) return null;

        return switch (property) {
            case "width", "w" -> dim.w;
            case "height", "h" -> dim.h;
            case "x" -> dim.x;
            case "y" -> dim.y;
            default -> null;
        };
    }

    public static void clear() {
        TEMPLATE_DIMENSIONS.clear();
        SCREEN_DIMENSIONS.clear();
    }
}