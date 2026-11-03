

package net.alan.gui.ht;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.screen.ScreenConfig;
import net.alan.gui.data.screen.ScreenLayout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
public final class HtLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("sirius_ui/ht");
    private HtLoader() {
    }
    public static HtProgram.SourceLoader sourceLoader(ResourceManager manager) {
        return id -> {
            ResourceLocation location;
            try {
                location = ResourceLocation.parse(id);
            } catch (Exception e) {
                throw new IOException("非法的资源路径：" + id, e);
            }
            Optional<Resource> resource = manager.getResource(location);
            if (resource.isEmpty()) {
                return null;
            }
            try (InputStream input = resource.get().open()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        };
    }
    public static ScreenLayout loadScreenLayout(ResourceManager manager, ResourceLocation id) {
        try {
            HtProgram program = HtProgram.load(id.toString(), sourceLoader(manager));
            for (String error : program.errors()) {
                LOGGER.error("[{}] 依赖文件有误：{}", id, error);
            }
            HtInterpreter.Result result = HtInterpreter.run(program);
            for (String warning : result.warnings()) {
                LOGGER.warn("[{}] {}", id, warning);
            }
            ScreenConfig config = buildConfig(result);
            String screenId = ScreenVariableRegistry.extractScreenId(id);
            ScreenVariableRegistry.registerScreenMember(screenId, config.getMember());
            ScreenLayout layout = new ScreenLayout();
            layout.setScreen(config);
            LOGGER.debug("Loaded HT screen {} ({} elements)", id, result.elements().size());
            return layout;
        } catch (HtException e) {
            LOGGER.error("HT 编译失败，屏幕将回退到默认实现：{}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("HT 加载异常：{}", id, e);
            return null;
        }
    }
    public static HtValue.Obj loadCardObject(ResourceManager manager, ResourceLocation id) {
        try {
            HtProgram program = HtProgram.load(id.toString(), sourceLoader(manager));
            return HtInterpreter.runObject(program);
        } catch (HtException e) {
            LOGGER.error("卡片定义编译失败：{}", e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("卡片定义加载异常：{}", id, e);
            return null;
        }
    }
    private static ScreenConfig buildConfig(HtInterpreter.Result result) {
        ScreenConfig config = new ScreenConfig();
        if (result.panorama() != null) {
            config.setPanorama(HtData.panorama(result.panorama()));
        }
        if (!result.graphicsDraw().isEmpty()) {
            config.setBackgrounds(HtData.backgroundLayers(result.graphicsDraw()));
        }
        config.setElements(new ArrayList<>(result.elements()));
        config.setVariables(new java.util.LinkedHashMap<>(result.variables()));
        config.setMember(new java.util.LinkedHashMap<>(result.member()));
        config.setSharedState(new java.util.LinkedHashMap<>(result.sharedState()));
        return config;
    }
}
