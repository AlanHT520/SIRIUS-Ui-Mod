package net.alan.gui.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NineSliceHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(NineSliceHelper.class);

    private static final Map<ResourceLocation, NineSliceInfo> CACHE = new ConcurrentHashMap<>();

    public record NineSliceInfo(int border, int width, int height) {}

    public static NineSliceInfo loadNineSlice(ResourceLocation textureId) {
        if (textureId == null) return null;
        return CACHE.computeIfAbsent(textureId, NineSliceHelper::loadFromMcmeta);
    }

    private static NineSliceInfo loadFromMcmeta(ResourceLocation textureId) {
        try {
            String mcmetaPath = textureId.getPath() + ".mcmeta";
            ResourceLocation mcmetaId = new ResourceLocation(textureId.getNamespace(), mcmetaPath);

            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(mcmetaId);
            if (resource.isEmpty()) return null;

            try (Reader reader = resource.get().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("gui")) return null;

                JsonObject gui = root.getAsJsonObject("gui");
                if (!gui.has("scaling")) return null;

                JsonObject scaling = gui.getAsJsonObject("scaling");
                String type = scaling.has("type") ? scaling.get("type").getAsString() : "";
                if (!"nine_slice".equals(type)) return null;

                int border = scaling.has("border") ? scaling.get("border").getAsInt() : 0;
                int width = scaling.has("width") ? scaling.get("width").getAsInt() : 0;
                int height = scaling.has("height") ? scaling.get("height").getAsInt() : 0;

                if (border <= 0) return null;
                return new NineSliceInfo(border, width, height);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load nine_slice for {}: {}", textureId, e.getMessage());
            return null;
        }
    }

    public static void blitNineSliced(GuiGraphics graphics, ResourceLocation texture,
                                       int x, int y, int width, int height,
                                       NineSliceInfo info) {
        if (info == null || texture == null) return;

        int border = info.border;
        int texW = info.width > 0 ? info.width : 256;
        int texH = info.height > 0 ? info.height : 256;

        if (width < border * 2) width = border * 2;
        if (height < border * 2) height = border * 2;

        int edgeW = texW - border * 2;
        int edgeH = texH - border * 2;
        int fillW = width - border * 2;
        int fillH = height - border * 2;

        if (edgeW <= 0) edgeW = 1;
        if (edgeH <= 0) edgeH = 1;

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 四角：使用 9 参数 blit，源/目标尺寸相同，uOffset/vOffset 为像素值
        // 左上角：从纹理像素 (0, 0) 开始，取 border x border 区域
        graphics.blit(texture, x, y, 0, 0, border, border, texW, texH);
        // 右上角：从纹理像素 (texW - border, 0) 开始
        graphics.blit(texture, x + width - border, y, texW - border, 0, border, border, texW, texH);
        // 左下角：从纹理像素 (0, texH - border) 开始
        graphics.blit(texture, x, y + height - border, 0, texH - border, border, border, texW, texH);
        // 右下角：从纹理像素 (texW - border, texH - border) 开始
        graphics.blit(texture, x + width - border, y + height - border, texW - border, texH - border, border, border, texW, texH);

        // 四边及中心：使用 11 参数 blit
        // 注意：11 参数版本的 uOffset/vOffset 是像素值，float 参数会被内部方法再次除以 textureWidth/textureHeight 归一化
        // 参考 GuiGraphics.blit 12 参数版本：
        //   (uOffset + 0.0F) / (float)textureWidth
        //   (uOffset + (float)uWidth) / (float)textureWidth

        // 上边：纹理区域 (border, 0, edgeW, border) → 目标 (x+border, y, fillW, border)
        graphics.blit(texture, x + border, y, fillW, border, (float)border, 0.0f, edgeW, border, texW, texH);
        // 下边：纹理区域 (border, texH-border, edgeW, border) → 目标 (x+border, y+height-border, fillW, border)
        graphics.blit(texture, x + border, y + height - border, fillW, border, (float)border, (float)(texH - border), edgeW, border, texW, texH);
        // 左边：纹理区域 (0, border, border, edgeH) → 目标 (x, y+border, border, fillH)
        graphics.blit(texture, x, y + border, border, fillH, 0.0f, (float)border, border, edgeH, texW, texH);
        // 右边：纹理区域 (texW-border, border, border, edgeH) → 目标 (x+width-border, y+border, border, fillH)
        graphics.blit(texture, x + width - border, y + border, border, fillH, (float)(texW - border), (float)border, border, edgeH, texW, texH);

        // 中心：纹理区域 (border, border, edgeW, edgeH) → 目标 (x+border, y+border, fillW, fillH)
        graphics.blit(texture, x + border, y + border, fillW, fillH, (float)border, (float)border, edgeW, edgeH, texW, texH);
    }
}