package net.alan.gui.render.popup;

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

public class PopupRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PopupRegistry.class);
    private static final ConcurrentHashMap<String, PopupDefinition> cache = new ConcurrentHashMap<>();

    public static PopupDefinition get(ResourceManager manager, String popupId) {
        return cache.computeIfAbsent(popupId, id -> load(manager, id));
    }

    public static PopupDefinition createFromJson(JsonObject popupObj) {
        return parsePopupObject(popupObj);
    }

    public static void clearCache() {
        cache.clear();
    }

    private static PopupDefinition load(ResourceManager manager, String popupId) {
        ResourceLocation location = ResourceLocation.parse(popupId);
        if (!location.getPath().endsWith(".json")) {
            location = location.withPath(location.getPath() + ".json");
        }
        Optional<Resource> optional = manager.getResource(location);
        if (optional.isEmpty()) {
            LOGGER.error("Popup definition not found: {}", popupId);
            return createDefault();
        }
        try (Reader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("popup") || !root.get("popup").isJsonObject()) {
                LOGGER.error("Missing 'popup' object in {}", popupId);
                return createDefault();
            }
            JsonObject popupObj = root.getAsJsonObject("popup");
            PopupDefinition def = parsePopupObject(popupObj);
            LOGGER.info("Loaded popup definition: {}", popupId);
            return def;
        } catch (Exception e) {
            LOGGER.error("Failed to load popup {}: {}", popupId, e.getMessage(), e);
            return createDefault();
        }
    }

    private static PopupDefinition parsePopupObject(JsonObject popupObj) {
        PopupDefinition def = new PopupDefinition();

        if (popupObj.has("type")) def.setType(popupObj.get("type").getAsString());
        if (popupObj.has("modal")) def.setModal(popupObj.get("modal").getAsBoolean());
        if (popupObj.has("width")) def.setWidth(popupObj.get("width").getAsInt());
        if (popupObj.has("min_height")) def.setMinHeight(popupObj.get("min_height").getAsInt());
        if (popupObj.has("padding")) def.setPadding(popupObj.get("padding").getAsInt());
        if (popupObj.has("overlay")) def.setOverlayColor(popupObj.get("overlay").getAsString());
        if (popupObj.has("background")) def.setBoxColor(popupObj.get("background").getAsString());
        if (popupObj.has("border")) def.setBorderColor(popupObj.get("border").getAsString());
        if (popupObj.has("has_inputs")) def.setHasInputs(popupObj.get("has_inputs").getAsBoolean());

        if (popupObj.has("variables") && popupObj.get("variables").isJsonObject()) {
            Map<String, String> vars = new LinkedHashMap<>();
            JsonObject varsObj = popupObj.getAsJsonObject("variables");
            for (var entry : varsObj.entrySet()) {
                vars.put(entry.getKey(), entry.getValue().getAsString());
            }
            def.setVariables(vars);
        }

        if (popupObj.has("inputs") && popupObj.get("inputs").isJsonArray()) {
            List<PopupDefinition.PopupInputDef> inputs = new ArrayList<>();
            JsonArray inputsArr = popupObj.getAsJsonArray("inputs");
            for (JsonElement elem : inputsArr) {
                if (elem.isJsonObject()) {
                    JsonObject inputObj = elem.getAsJsonObject();
                    PopupDefinition.PopupInputDef inputDef = new PopupDefinition.PopupInputDef();
                    if (inputObj.has("id")) inputDef.setId(inputObj.get("id").getAsString());
                    if (inputObj.has("label")) inputDef.setLabel(inputObj.get("label").getAsString());
                    if (inputObj.has("default")) inputDef.setDefaultValue(inputObj.get("default").getAsString());
                    if (inputObj.has("hint")) inputDef.setHint(inputObj.get("hint").getAsString());
                    if (inputObj.has("max_length")) inputDef.setMaxLength(inputObj.get("max_length").getAsInt());
                    inputs.add(inputDef);
                }
            }
            def.setInputs(inputs);
        }

        if (popupObj.has("elements") && popupObj.get("elements").isJsonArray()) {
            List<JsonElement> elements = new ArrayList<>();
            JsonArray elementsArr = popupObj.getAsJsonArray("elements");
            for (JsonElement elem : elementsArr) {
                elements.add(elem);
            }
            def.setElements(elements);
        }

        return def;
    }

    private static PopupDefinition createDefault() {
        return new PopupDefinition();
    }
}