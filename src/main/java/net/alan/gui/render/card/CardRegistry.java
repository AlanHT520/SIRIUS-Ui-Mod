

package net.alan.gui.render.card;
import net.alan.gui.ht.HtValue;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class CardRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CardRegistry.class);
    private static final ConcurrentHashMap<String, CardDefinition> cache = new ConcurrentHashMap<>();
    public static CardDefinition get(ResourceManager manager, String cardId) {
        return cache.computeIfAbsent(cardId, id -> load(manager, id));
    }
    public static CardDefinition createFromData(HtValue.Obj cardObj) {
        return parseCardObject(cardObj);
    }
    public static void clearCache() {
        cache.clear();
    }
    private static CardDefinition load(ResourceManager manager, String cardId) {
        ResourceLocation location = ResourceLocation.parse(cardId);
        if (!location.getPath().endsWith(".ht")) {
            location = location.withPath(location.getPath() + ".ht");
        }
        HtValue.Obj cardObj = net.alan.gui.ht.HtLoader.loadCardObject(manager, location);
        if (cardObj == null) {
            LOGGER.error("Card definition not found or invalid: {}", cardId);
            return createDefault();
        }
        try {
            CardDefinition def = parseCardObject(cardObj);
            LOGGER.info("Loaded card definition: {}", cardId);
            return def;
        } catch (Exception e) {
            LOGGER.error("Failed to parse card {}: {}", cardId, e.getMessage(), e);
            return createDefault();
        }
    }
    private static CardDefinition parseCardObject(HtValue.Obj cardObj) {
        CardDefinition def = new CardDefinition();
        if (cardObj.has("type")) def.setType(cardObj.get("type").getAsString());
        if (cardObj.has("modal")) def.setModal(cardObj.get("modal").getAsBoolean());
        if (cardObj.has("width")) def.setWidth(cardObj.get("width").getAsInt());
        if (cardObj.has("min_height")) def.setMinHeight(cardObj.get("min_height").getAsInt());
        if (cardObj.has("max_height")) def.setMaxHeight(cardObj.get("max_height").getAsInt());
        if (cardObj.has("padding")) def.setPadding(cardObj.get("padding").getAsInt());
        if (cardObj.has("duration_ms")) def.setDurationMs(cardObj.get("duration_ms").getAsInt());
        if (cardObj.has("fade_ms")) def.setFadeMs(cardObj.get("fade_ms").getAsInt());
        if (cardObj.has("pos_x")) def.setPosX(cardObj.get("pos_x").getAsInt());
        if (cardObj.has("pos_y")) def.setPosY(cardObj.get("pos_y").getAsInt());
        if (cardObj.has("shadow_offset")) def.setShadowOffset(cardObj.get("shadow_offset").getAsInt());
        if (cardObj.has("shadow_alpha")) def.setShadowAlpha(cardObj.get("shadow_alpha").getAsInt());
        if (cardObj.has("z_index")) def.setZIndex(cardObj.get("z_index").getAsInt());
        if (cardObj.has("data_source")) def.setDataSource(cardObj.get("data_source").getAsString());
        if (cardObj.has("overlay")) {
            def.setOverlay(parseColorString(cardObj.get("overlay")));
        }
        if (cardObj.has("background")) {
            def.setBackground(parseBackground(cardObj.get("background")));
        }
        if (cardObj.has("border")) {
            def.setBorder(parseColorString(cardObj.get("border")));
        }
        if (cardObj.has("border_width")) def.setBorderWidth(cardObj.get("border_width").getAsInt());
        if (cardObj.has("title_bar") && !((cardObj.get("title_bar")) instanceof HtValue.Null)) {
            def.setTitleBar(parseTitleBar(cardObj.getAsJsonObject("title_bar")));
        }
        if (cardObj.has("variables") && cardObj.get("variables").isJsonObject()) {
            Map<String, String> vars = new LinkedHashMap<>();
            HtValue.Obj varsObj = cardObj.getAsJsonObject("variables");
            for (var entry : varsObj.fields().entrySet()) {
                vars.put(entry.getKey(), entry.getValue().getAsString());
            }
            def.setVariables(vars);
        }
        if (cardObj.has("elements") && cardObj.get("elements").isJsonArray()) {
            List<HtValue> elements = new ArrayList<>();
            HtValue.Arr elementsArr = cardObj.getAsJsonArray("elements");
            for (HtValue elem : elementsArr) {
                elements.add(elem);
            }
            def.setElements(elements);
        }
        return def;
    }
    private static String parseColorString(HtValue element) {
        if (element == null || ((element) instanceof HtValue.Null)) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            HtValue.Obj obj = element.getAsJsonObject();
            if (obj.has("color") && !((obj.get("color")) instanceof HtValue.Null)) {
                return obj.get("color").getAsString();
            }
        }
        return null;
    }
    private static BackgroundDef parseBackground(HtValue element) {
        if (element == null || ((element) instanceof HtValue.Null)) return new BackgroundDef();
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new BackgroundDef(element.getAsString());
        }
        if (element.isJsonObject()) {
            HtValue.Obj obj = element.getAsJsonObject();
            BackgroundDef bg = new BackgroundDef();
            if (obj.has("color") && !((obj.get("color")) instanceof HtValue.Null)) {
                bg.setColor(obj.get("color").getAsString());
            }
            if (obj.has("texture") && !((obj.get("texture")) instanceof HtValue.Null)) {
                bg.setTexture(obj.get("texture").getAsString());
            }
            if (obj.has("texture_mode") && !((obj.get("texture_mode")) instanceof HtValue.Null)) {
                bg.setTextureMode(obj.get("texture_mode").getAsString());
            }
            return bg;
        }
        return new BackgroundDef();
    }
    private static TitleBarDef parseTitleBar(HtValue.Obj obj) {
        TitleBarDef tb = new TitleBarDef();
        if (obj.has("height")) tb.setHeight(obj.get("height").getAsInt());
        if (obj.has("draggable")) tb.setDraggable(obj.get("draggable").getAsBoolean());
        if (obj.has("background")) {
            tb.setBackground(parseBackground(obj.get("background")));
        }
        if (obj.has("elements") && obj.get("elements").isJsonArray()) {
            List<HtValue> elements = new ArrayList<>();
            HtValue.Arr elementsArr = obj.getAsJsonArray("elements");
            for (HtValue elem : elementsArr) {
                elements.add(elem);
            }
            tb.setElements(elements);
        }
        return tb;
    }
    private static CardDefinition createDefault() {
        return new CardDefinition();
    }
}
