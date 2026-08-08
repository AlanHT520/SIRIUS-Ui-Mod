package net.alan.gui.render.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CardRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CardRegistry.class);
    private static final ConcurrentHashMap<String, CardDefinition> cache = new ConcurrentHashMap<>();

    public static CardDefinition get(ResourceManager manager, String cardId) {
        return cache.computeIfAbsent(cardId, id -> load(manager, id));
    }

    public static CardDefinition createFromJson(JsonObject cardObj) {
        return parseCardObject(cardObj);
    }

    public static void clearCache() {
        cache.clear();
    }

    private static CardDefinition load(ResourceManager manager, String cardId) {
        ResourceLocation location = ResourceLocation.parse(cardId);
        if (!location.getPath().endsWith(".json")) {
            location = location.withPath(location.getPath() + ".json");
        }
        Optional<Resource> optional = manager.getResource(location);
        if (optional.isEmpty()) {
            LOGGER.error("Card definition not found: {}", cardId);
            return createDefault();
        }
        try (Reader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("card") || !root.get("card").isJsonObject()) {
                LOGGER.error("Missing 'card' object in {}", cardId);
                return createDefault();
            }
            JsonObject cardObj = root.getAsJsonObject("card");
            CardDefinition def = parseCardObject(cardObj);
            LOGGER.info("Loaded card definition: {}", cardId);
            return def;
        } catch (Exception e) {
            LOGGER.error("Failed to load card {}: {}", cardId, e.getMessage(), e);
            return createDefault();
        }
    }

    private static CardDefinition parseCardObject(JsonObject cardObj) {
        CardDefinition def = new CardDefinition();

        if (cardObj.has("type")) def.setType(cardObj.get("type").getAsString());
        if (cardObj.has("modal")) def.setModal(cardObj.get("modal").getAsBoolean());
        if (cardObj.has("width")) def.setWidth(cardObj.get("width").getAsInt());
        if (cardObj.has("min_height")) def.setMinHeight(cardObj.get("min_height").getAsInt());
        if (cardObj.has("padding")) def.setPadding(cardObj.get("padding").getAsInt());
        if (cardObj.has("duration_ms")) def.setDurationMs(cardObj.get("duration_ms").getAsInt());
        if (cardObj.has("fade_ms")) def.setFadeMs(cardObj.get("fade_ms").getAsInt());
        if (cardObj.has("pos_x")) def.setPosX(cardObj.get("pos_x").getAsInt());
        if (cardObj.has("pos_y")) def.setPosY(cardObj.get("pos_y").getAsInt());
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

        if (cardObj.has("title_bar") && !cardObj.get("title_bar").isJsonNull()) {
            def.setTitleBar(parseTitleBar(cardObj.getAsJsonObject("title_bar")));
        }

        if (cardObj.has("variables") && cardObj.get("variables").isJsonObject()) {
            Map<String, String> vars = new LinkedHashMap<>();
            JsonObject varsObj = cardObj.getAsJsonObject("variables");
            for (var entry : varsObj.entrySet()) {
                vars.put(entry.getKey(), entry.getValue().getAsString());
            }
            def.setVariables(vars);
        }

        if (cardObj.has("elements") && cardObj.get("elements").isJsonArray()) {
            List<JsonElement> elements = new ArrayList<>();
            JsonArray elementsArr = cardObj.getAsJsonArray("elements");
            for (JsonElement elem : elementsArr) {
                elements.add(elem);
            }
            def.setElements(elements);
        }

        return def;
    }

    private static String parseColorString(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return element.getAsString();
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("color") && !obj.get("color").isJsonNull()) {
                return obj.get("color").getAsString();
            }
        }
        return null;
    }

    private static BackgroundDef parseBackground(JsonElement element) {
        if (element == null || element.isJsonNull()) return new BackgroundDef();
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new BackgroundDef(element.getAsString());
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            BackgroundDef bg = new BackgroundDef();
            if (obj.has("color") && !obj.get("color").isJsonNull()) {
                bg.setColor(obj.get("color").getAsString());
            }
            if (obj.has("texture") && !obj.get("texture").isJsonNull()) {
                bg.setTexture(obj.get("texture").getAsString());
            }
            if (obj.has("texture_mode") && !obj.get("texture_mode").isJsonNull()) {
                bg.setTextureMode(obj.get("texture_mode").getAsString());
            }
            return bg;
        }
        return new BackgroundDef();
    }

    private static TitleBarDef parseTitleBar(JsonObject obj) {
        TitleBarDef tb = new TitleBarDef();
        if (obj.has("height")) tb.setHeight(obj.get("height").getAsInt());
        if (obj.has("draggable")) tb.setDraggable(obj.get("draggable").getAsBoolean());
        if (obj.has("background")) {
            tb.setBackground(parseBackground(obj.get("background")));
        }
        if (obj.has("elements") && obj.get("elements").isJsonArray()) {
            List<JsonElement> elements = new ArrayList<>();
            JsonArray elementsArr = obj.getAsJsonArray("elements");
            for (JsonElement elem : elementsArr) {
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