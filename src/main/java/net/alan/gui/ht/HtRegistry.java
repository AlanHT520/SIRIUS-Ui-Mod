

package net.alan.gui.ht;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
public final class HtRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("sirius_ui/ht");
    public static final ResourceLocation REGISTER_LOCATION =
            ResourceLocation.withDefaultNamespace("alanht/register.ht");
    private static volatile Map<String, ResourceLocation> mappings = Map.of();
    private static volatile boolean loaded;
    private HtRegistry() {
    }
    public static Map<String, ResourceLocation> load(ResourceManager manager) {
        Map<String, ResourceLocation> result = new LinkedHashMap<>();
        if (manager.getResource(REGISTER_LOCATION).isEmpty()) {
            loaded = true;
            mappings = Map.of();
            return mappings;
        }
        try {
            HtProgram program = HtProgram.load(REGISTER_LOCATION.toString(),
                    HtLoader.sourceLoader(manager));
            for (String error : program.errors()) {
                LOGGER.error("register.ht 依赖有误（相关屏幕会被跳过）：{}", error);
            }
            for (Map.Entry<String, String> entry : program.register().entrySet()) {
                HtProgram.ScreenEntry screen = program.screens().get(entry.getValue());
                if (screen == null) {
                    LOGGER.error("register.ht: screen_id {} 指向的 screen '{}' 不存在"
                                    + "（需要 import 对应文件）",
                            entry.getKey(), entry.getValue());
                    continue;
                }
                try {
                    result.put(entry.getKey(), ResourceLocation.parse(screen.fileId()));
                } catch (Exception e) {
                    LOGGER.error("register.ht: {} 的路径非法：{}",
                            entry.getKey(), screen.fileId(), e);
                }
            }
            LOGGER.info("Loaded {} HT screen mappings", result.size());
        } catch (Exception e) {
            LOGGER.error("register.ht 解析失败，所有屏幕将回退到原版实现", e);
        }
        mappings = Map.copyOf(result);
        loaded = true;
        return mappings;
    }
    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        load(minecraft.getResourceManager());
    }
    public static Optional<ResourceLocation> getScreenPath(String screenId) {
        ensureLoaded();
        return Optional.ofNullable(mappings.get(screenId));
    }
    public static boolean isLoaded() {
        return loaded;
    }
    public static Map<String, ResourceLocation> getMappings() {
        return new LinkedHashMap<>(mappings);
    }
    public static void clear() {
        mappings = Map.of();
        loaded = false;
    }
}
