

package net.alan.gui.context;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class ScreenVariableRegistry {
    private static final Map<String, Map<String, String>> SCREEN_MEMBERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Map<String, String>>> ELEMENT_MEMBERS = new ConcurrentHashMap<>();
    public static void registerScreenMember(String screenId, Map<String, String> member) {
        if (member != null && !member.isEmpty()) {
            SCREEN_MEMBERS.put(screenId, new LinkedHashMap<>(member));
        }
    }
    public static void registerElementMember(String screenId, String elementId, Map<String, String> member) {
        if (elementId == null || member == null || member.isEmpty()) return;
        ELEMENT_MEMBERS
                .computeIfAbsent(screenId, k -> new ConcurrentHashMap<>())
                .put(elementId, new LinkedHashMap<>(member));
    }
    public static Map<String, String> getScreenMember(String screenId) {
        return SCREEN_MEMBERS.get(screenId);
    }
    public static Map<String, String> getElementMember(String screenId, String elementId) {
        Map<String, Map<String, String>> screenElements = ELEMENT_MEMBERS.get(screenId);
        return screenElements != null ? screenElements.get(elementId) : null;
    }
    public static java.util.Set<String> getRegisteredScreenIds() {
        return SCREEN_MEMBERS.keySet();
    }
    public static java.util.Set<String> getElementIds(String screenId) {
        Map<String, Map<String, String>> screenElements = ELEMENT_MEMBERS.get(screenId);
        return screenElements != null ? screenElements.keySet() : java.util.Set.of();
    }
    public static String extractScreenId(net.minecraft.resources.ResourceLocation location) {
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        if (fileName.endsWith(".ht")) {
            fileName = fileName.substring(0, fileName.length() - 3);
        }
        return fileName;
    }
}
