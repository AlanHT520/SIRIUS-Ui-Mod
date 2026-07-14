package net.alan.gui.util;

import com.google.gson.*;
import net.alan.gui.data.background.BackgroundLayer;
import net.alan.gui.data.background.PanoramaConfig;
import net.alan.gui.data.background.Slide;
import net.alan.gui.data.background.SlideGroup;
import net.alan.gui.context.ScreenVariableRegistry;
import net.alan.gui.data.config.ScreenConfig;
import net.alan.gui.data.config.ScreenLayout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonLoader.class);
    private static final Gson GSON = createGson();

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(PanoramaConfig.class, new PanoramaConfigDeserializer())
                .create();
    }

    public static ScreenLayout loadScreenLayout(ResourceManager manager, ResourceLocation id) {
        return loadScreenLayout(manager, id, new java.util.HashSet<>());
    }

    private static ScreenLayout loadScreenLayout(ResourceManager manager, ResourceLocation id,
                                                  java.util.Set<String> visited) {
        String idStr = id.toString();
        if (!visited.add(idStr)) {
            LOGGER.error("Circular parent reference detected: {}", idStr);
            return null;
        }

        Optional<Resource> optional = manager.getResource(id);
        if (optional.isEmpty()) {
            LOGGER.error("Screen layout not found: {}", id);
            return null;
        }
        try (Reader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("screen") || !root.get("screen").isJsonObject()) {
                LOGGER.error("Missing 'screen' object in {}", id);
                return null;
            }
            JsonObject screenObj = root.getAsJsonObject("screen");

            ScreenConfig config = parseScreenConfig(screenObj, manager);

            if (screenObj.has("parent") && screenObj.get("parent").isJsonPrimitive()) {
                String parentPath = screenObj.get("parent").getAsString();
                ResourceLocation parentId = ResourceLocation.parse(parentPath);
                ScreenLayout parentLayout = loadScreenLayout(manager, parentId, visited);
                if (parentLayout != null) {
                    config = mergeScreenConfig(parentLayout.getScreen(), config);
                }
            }

            String screenId = ScreenVariableRegistry.extractScreenId(id);
            ScreenVariableRegistry.registerScreenMember(screenId, config.getMember());

            ScreenLayout layout = new ScreenLayout();
            layout.setScreen(config);
            return layout;
        } catch (Exception e) {
            LOGGER.error("Failed to load screen layout {}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    private static ScreenConfig parseScreenConfig(JsonObject screenObj, ResourceManager manager) {
        PanoramaConfig panorama = null;
        if (screenObj.has("panorama")) {
            panorama = GSON.fromJson(screenObj.get("panorama"), PanoramaConfig.class);
        }

        List<BackgroundLayer> backgrounds = new ArrayList<>();
        if (screenObj.has("graphicsDraw") && screenObj.get("graphicsDraw").isJsonArray()) {
            for (JsonElement elem : screenObj.getAsJsonArray("graphicsDraw")) {
                BackgroundLayer layer = GSON.fromJson(elem, BackgroundLayer.class);
                backgrounds.add(layer);
            }
        }

        List<JsonElement> elements = new ArrayList<>();
        if (screenObj.has("elements") && screenObj.get("elements").isJsonArray()) {
            JsonArray elementsArray = screenObj.getAsJsonArray("elements");
            JsonArray resolved = resolveElementRefs(elementsArray, manager);
            for (JsonElement elem : resolved) {
                elements.add(elem);
            }
        }

        Map<String, String> variables = new LinkedHashMap<>();
        if (screenObj.has("variables") && screenObj.get("variables").isJsonObject()) {
            JsonObject varsObj = screenObj.getAsJsonObject("variables");
            for (var entry : varsObj.entrySet()) {
                variables.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        Map<String, String> member = new LinkedHashMap<>();
        if (screenObj.has("member") && screenObj.get("member").isJsonObject()) {
            JsonObject memberObj = screenObj.getAsJsonObject("member");
            for (var entry : memberObj.entrySet()) {
                member.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        ScreenConfig config = new ScreenConfig();
        if (screenObj.has("parent") && screenObj.get("parent").isJsonPrimitive()) {
            config.setParent(screenObj.get("parent").getAsString());
        }
        config.setPanorama(panorama);
        config.setBackgrounds(backgrounds);
        config.setElements(elements);
        config.setVariables(variables);
        config.setMember(member);
        return config;
    }

    private static ScreenConfig mergeScreenConfig(ScreenConfig parent, ScreenConfig child) {
        ScreenConfig merged = new ScreenConfig();

        merged.setPanorama(child.getPanoramaConfig() != null ? child.getPanoramaConfig() : parent.getPanoramaConfig());

        List<BackgroundLayer> mergedLayers = new ArrayList<>();
        if (parent.getBackgrounds() != null) mergedLayers.addAll(parent.getBackgrounds());
        if (child.getBackgrounds() != null) mergedLayers.addAll(child.getBackgrounds());
        merged.setBackgrounds(mergedLayers);

        List<JsonElement> mergedElements = new ArrayList<>();
        if (parent.getElements() != null) mergedElements.addAll(parent.getElements());
        if (child.getElements() != null) mergedElements.addAll(child.getElements());
        merged.setElements(mergedElements);

        Map<String, String> mergedVars = new LinkedHashMap<>();
        if (parent.getVariables() != null) mergedVars.putAll(parent.getVariables());
        if (child.getVariables() != null) mergedVars.putAll(child.getVariables());
        merged.setVariables(mergedVars);

        Map<String, String> mergedMember = new LinkedHashMap<>();
        if (parent.getMember() != null) mergedMember.putAll(parent.getMember());
        if (child.getMember() != null) mergedMember.putAll(child.getMember());
        merged.setMember(mergedMember);

        if (child.getParent() != null) merged.setParent(child.getParent());

        return merged;
    }

    private static JsonArray resolveElementRefs(JsonArray elements, ResourceManager manager) {
        JsonArray resolved = new JsonArray();
        for (JsonElement elem : elements) {
            if (elem.isJsonObject()) {
                JsonObject obj = elem.getAsJsonObject();
                if (obj.has("ref")) {
                    String refPath = obj.get("ref").getAsString();
                    try {
                        ResourceLocation refId = ResourceLocation.parse(refPath);
                        JsonElement refContent = loadElement(manager, refId);
                        if (refContent == null) {
                            LOGGER.warn("Referenced element not found: {}, skipping", refPath);
                            continue;
                        }
                        if (refContent.isJsonArray()) {
                            for (JsonElement item : refContent.getAsJsonArray()) {
                                resolved.add(item);
                            }
                        } else if (refContent.isJsonObject()) {
                            JsonObject base = deepCopy(refContent.getAsJsonObject());
                            for (var entry : obj.entrySet()) {
                                if (entry.getKey().equals("ref")) continue;
                                if (entry.getKey().equals("variables") && base.has("variables")
                                        && entry.getValue().isJsonObject()) {
                                    JsonObject baseVars = base.getAsJsonObject("variables");
                                    JsonObject newVars = entry.getValue().getAsJsonObject();
                                    for (var varEntry : newVars.entrySet()) {
                                        baseVars.add(varEntry.getKey(), varEntry.getValue().deepCopy());
                                    }
                                } else if (entry.getKey().equals("member") && base.has("member")
                                        && entry.getValue().isJsonObject()) {
                                    JsonObject baseMember = base.getAsJsonObject("member");
                                    JsonObject newMember = entry.getValue().getAsJsonObject();
                                    for (var entryM : newMember.entrySet()) {
                                        baseMember.add(entryM.getKey(), entryM.getValue().deepCopy());
                                    }
                                } else if (entry.getKey().equals("text") && base.has("text")
                                        && entry.getValue().isJsonObject()) {
                                    JsonObject baseText = base.getAsJsonObject("text");
                                    JsonObject newText = entry.getValue().getAsJsonObject();
                                    for (var entryT : newText.entrySet()) {
                                        baseText.add(entryT.getKey(), entryT.getValue().deepCopy());
                                    }
                                } else {
                                    base.add(entry.getKey(), entry.getValue().deepCopy());
                                }
                            }
                            resolved.add(base);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to resolve ref '{}'", refPath, e);
                    }
                } else {
                    resolved.add(obj);
                }
            } else {
                resolved.add(elem);
            }
        }
        boolean hasRef = false;
        for (JsonElement e : resolved) {
            if (e.isJsonObject() && e.getAsJsonObject().has("ref")) {
                hasRef = true;
                break;
            }
        }
        if (hasRef) {
            return resolveElementRefs(resolved, manager);
        }
        return resolved;
    }

    private static JsonElement loadElement(ResourceManager manager, ResourceLocation id) {
        Optional<Resource> optional = manager.getResource(id);
        if (optional.isEmpty()) return null;
        try (Reader reader = new InputStreamReader(optional.get().open(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            LOGGER.error("Failed to load element {}: {}", id, e.getMessage());
            return null;
        }
    }

    private static JsonObject deepCopy(JsonObject source) {
        JsonObject copy = new JsonObject();
        for (var entry : source.entrySet()) {
            copy.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return copy;
    }

    private static class PanoramaConfigDeserializer implements JsonDeserializer<PanoramaConfig> {
        @Override
        public PanoramaConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            PanoramaConfig config = new PanoramaConfig();
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
                config.setType(json.getAsString());
                return config;
            }
            if (!json.isJsonObject()) return config;
            JsonObject obj = json.getAsJsonObject();
            config.setType(obj.has("type") ? obj.get("type").getAsString() : "none");
            if (obj.has("texture")) config.setTexture(obj.get("texture").getAsString());
            if (obj.has("defaultTime")) config.setDefaultTime(obj.get("defaultTime").getAsInt());
            if (obj.has("playGroups")) {
                List<String> list = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("playGroups")) list.add(e.getAsString());
                config.setPlayGroups(list);
            }
            if (obj.has("playAfter")) config.setPlayAfter(obj.get("playAfter").getAsString());
            if (obj.has("groups")) {
                List<SlideGroup> groups = new ArrayList<>();
                for (JsonElement gElem : obj.getAsJsonArray("groups")) {
                    if (!gElem.isJsonObject()) continue;
                    JsonObject gObj = gElem.getAsJsonObject();
                    SlideGroup group = new SlideGroup();
                    if (gObj.has("id")) group.setId(gObj.get("id").getAsString());
                    if (gObj.has("playCount")) group.setPlayCount(gObj.get("playCount").getAsInt());
                    if (gObj.has("slides")) {
                        List<Slide> slides = new ArrayList<>();
                        for (JsonElement sElem : gObj.getAsJsonArray("slides")) {
                            if (!sElem.isJsonObject()) continue;
                            JsonObject sObj = sElem.getAsJsonObject();
                            Slide slide = new Slide();
                            if (sObj.has("texture")) slide.setTexture(sObj.get("texture").getAsString());
                            if (sObj.has("time")) slide.setTime(sObj.get("time").getAsInt());
                            if (sObj.has("transition")) slide.setTransition(sObj.get("transition").getAsString());
                            if (sObj.has("transition_duration")) slide.setTransitionDuration(sObj.get("transition_duration").getAsInt());
                            slides.add(slide);
                        }
                        group.setSlides(slides);
                    }
                    groups.add(group);
                }
                config.setGroups(groups);
            }
            return config;
        }
    }
}