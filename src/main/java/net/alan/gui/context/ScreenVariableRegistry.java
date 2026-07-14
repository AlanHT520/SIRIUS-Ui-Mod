package net.alan.gui.context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局 Screen 成员变量注册表。
 * 允许其他 screen 通过 screenId.xxx 引用某个 screen 定义的变量。
 */
public class ScreenVariableRegistry {
    private static final Map<String, Map<String, String>> SCREEN_MEMBERS = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Map<String, String>>> ELEMENT_MEMBERS = new ConcurrentHashMap<>();

    /**
     * 注册一个 screen 的 member 变量定义（原始表达式，未求值）。
     */
    public static void registerScreenMember(String screenId, Map<String, String> member) {
        if (member != null && !member.isEmpty()) {
            SCREEN_MEMBERS.put(screenId, new LinkedHashMap<>(member));
        }
    }

    /**
     * 注册一个元素的 member 变量。
     */
    public static void registerElementMember(String screenId, String elementId, Map<String, String> member) {
        if (elementId == null || member == null || member.isEmpty()) return;
        ELEMENT_MEMBERS
                .computeIfAbsent(screenId, k -> new ConcurrentHashMap<>())
                .put(elementId, new LinkedHashMap<>(member));
    }

    /**
     * 获取指定 screen 的 member 变量定义。
     */
    public static Map<String, String> getScreenMember(String screenId) {
        return SCREEN_MEMBERS.get(screenId);
    }

    /**
     * 获取指定元素的 member 变量定义。
     */
    public static Map<String, String> getElementMember(String screenId, String elementId) {
        Map<String, Map<String, String>> screenElements = ELEMENT_MEMBERS.get(screenId);
        return screenElements != null ? screenElements.get(elementId) : null;
    }

    /**
     * 获取所有已注册的 screen ID。
     */
    public static java.util.Set<String> getRegisteredScreenIds() {
        return SCREEN_MEMBERS.keySet();
    }

    /**
     * 获取指定 screen 下所有已注册 element 的 id。
     */
    public static java.util.Set<String> getElementIds(String screenId) {
        Map<String, Map<String, String>> screenElements = ELEMENT_MEMBERS.get(screenId);
        return screenElements != null ? screenElements.keySet() : java.util.Set.of();
    }

    /**
     * 从 ResourceLocation 中提取 screen ID。
     * 例如 "sirius_ui:screens/options_screen.json" -> "options_screen"
     */
    public static String extractScreenId(net.minecraft.resources.ResourceLocation location) {
        String path = location.getPath();
        // 取最后一个 / 之后的部分
        int lastSlash = path.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        // 去掉 .json 后缀
        if (fileName.endsWith(".json")) {
            fileName = fileName.substring(0, fileName.length() - 5);
        }
        return fileName;
    }
}