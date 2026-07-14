package net.alan.gui.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.alan.gui.data.CycleValue;
import net.alan.gui.data.DynamicListData;
import net.alan.gui.data.action.Action;
import net.alan.gui.data.geometry.Position;
import net.alan.gui.data.geometry.Size;
import net.alan.gui.data.props.*;
import net.alan.gui.data.style.TextureSet;
import net.alan.gui.data.source.ServerListDataSource;
import net.alan.gui.data.source.WorldSaveDataSource;
import net.alan.gui.data.source.RealmsDataSource;
import net.alan.gui.data.source.PackDataSource;
import net.alan.gui.render.ActionExecutor;
import net.alan.gui.widget.*;
import net.alan.gui.widget.ListWidget.RowDef;
import net.alan.gui.widget.StringEditBoxWidget;
import net.alan.gui.widget.cycle.ArrowSwitchWidget;
import net.alan.gui.widget.cycle.CycleButtonWidget;
import net.alan.gui.widget.cycle.DropdownWidget;
import net.alan.gui.widget.cycle.SelectorWidget;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class WidgetFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(WidgetFactory.class);

    @Deprecated
    public static Widget create(JsonObject json, ResourceManager manager) {
        LOGGER.warn("Using deprecated WidgetFactory.create without ActionExecutor. Button actions may fail.");
        return create(json, manager, null);
    }

    public static Widget create(JsonObject json, ResourceManager manager, ActionExecutor executor) {
        if (!json.has("type") && json.has("widget") && json.get("widget").isJsonObject()) {
            JsonObject innerJson = json.get("widget").getAsJsonObject();
            if (json.has("id") && !innerJson.has("id")) {
                innerJson.addProperty("id", json.get("id").getAsString());
            }
            return create(innerJson, manager, executor);
        }
        if (!json.has("type") && json.has("ref") && json.get("ref").isJsonPrimitive()) {
            return loadWidgetFromRef(json.get("ref").getAsString(), manager, executor, json);
        }
        String type = json.get("type").getAsString();
        String id = json.has("id") ? json.get("id").getAsString() : null;
        LayoutProps layout = parseLayout(json);
        Map<String, String> variables = json.has("variables")
                ? new com.google.gson.Gson().fromJson(json.get("variables"), new TypeToken<Map<String, String>>(){}.getType())
                : new HashMap<>();
        Map<String, String> member = json.has("member")
                ? new com.google.gson.Gson().fromJson(json.get("member"), new TypeToken<Map<String, String>>(){}.getType())
                : null;

        injectTextWidth(json, variables);

        Widget widget = switch (type) {
            case "container" -> {
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new ContainerWidget(id, layout, variables, member, children);
            }
            case "text" -> {
                TextProps textProps = json.has("text") && json.get("text").isJsonObject()
                        ? parseText(json.get("text").getAsJsonObject())
                        : parseText(json);
                yield new TextWidget(id, layout, variables, member, textProps);
            }
            case "image" -> {
                StyleProps style = parseStyle(json);
                yield new ImageWidget(id, layout, variables, member, style);
            }
            case "button" -> {
                StyleProps style = parseStyle(json);
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = parseText(json);
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                Action action = json.has("action")
                        ? new com.google.gson.Gson().fromJson(json.get("action"), Action.class)
                        : null;
                int ingDuration = json.has("ing_duration") ? json.get("ing_duration").getAsInt() : 0;
                Map<String, Map<String, String>> stateVariables = parseStateVariables(json);
                yield new ButtonWidget(id, layout, variables, member, style, textProps, action, executor, children, ingDuration, stateVariables);
            }
            case "button_content" -> {
                StyleProps style = parseStyle(json);
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = parseText(json);
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                String boxId = json.has("box_id") ? json.get("box_id").getAsString() : null;
                String targetId = json.has("target_id") ? json.get("target_id").getAsString() : id;
                Map<String, Map<String, String>> stateVariables = parseStateVariables(json);
                yield new ButtonContentWidget(id, layout, variables, member, style, textProps, executor, children, boxId, targetId, stateVariables);
            }
            case "slider" -> {
                SliderProps sliderProps = parseSlider(json);
                TextureSet trackTex = json.has("track_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("track_texture"), TextureSet.class)
                        : null;
                TextureSet handleTex = json.has("handle_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("handle_texture"), TextureSet.class)
                        : null;
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = new TextProps();
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new SliderWidget(id, layout, variables, member, sliderProps, trackTex, handleTex, textProps, children);
            }
            case "cycle_button" -> {
                CycleButtonProps cycleProps = parseCycleButton(json);
                TextureSet tex = json.has("texture")
                        ? new com.google.gson.Gson().fromJson(json.get("texture"), TextureSet.class)
                        : null;
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = new TextProps();
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new CycleButtonWidget(id, layout, variables, member, cycleProps, tex, textProps, children);
            }
            case "selector" -> {
                String optionKey = json.has("option_key") ? json.get("option_key").getAsString() : null;
                List<SelectorWidget.SegmentDef> segments = new ArrayList<>();
                if (json.has("values") && json.get("values").isJsonArray()) {
                    JsonArray arr = json.get("values").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            JsonObject valObj = elem.getAsJsonObject();
                            String key = valObj.has("key") ? valObj.get("key").getAsString() : "";
                            String textKey = valObj.has("text_key") ? valObj.get("text_key").getAsString() : "";
                            TextureSet segTex = valObj.has("texture")
                                    ? new com.google.gson.Gson().fromJson(valObj.get("texture"), TextureSet.class)
                                    : null;
                            int w = valObj.has("width") ? valObj.get("width").getAsInt() : 0;
                            segments.add(new SelectorWidget.SegmentDef(key, textKey, segTex, w));
                        }
                    }
                }
                TextureSet tex = json.has("texture")
                        ? new com.google.gson.Gson().fromJson(json.get("texture"), TextureSet.class)
                        : null;
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = new TextProps();
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new SelectorWidget(id, layout, variables, member, optionKey, segments, tex, textProps, children);
            }
            case "arrow_switch" -> {
                String optionKey = json.has("option_key") ? json.get("option_key").getAsString() : null;
                List<CycleValue> values = parseCycleValues(json);
                TextureSet leftTex = json.has("left_button_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("left_button_texture"), TextureSet.class)
                        : null;
                TextureSet rightTex = json.has("right_button_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("right_button_texture"), TextureSet.class)
                        : null;
                TextureSet centerTex = json.has("center_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("center_texture"), TextureSet.class)
                        : json.has("texture")
                        ? new com.google.gson.Gson().fromJson(json.get("texture"), TextureSet.class)
                        : null;
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = new TextProps();
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new ArrowSwitchWidget(id, layout, variables, member, optionKey, values, leftTex, rightTex, centerTex, textProps, children);
            }
            case "dropdown" -> {
                String optionKey = json.has("option_key") ? json.get("option_key").getAsString() : null;
                List<CycleValue> values = parseCycleValues(json);
                TextureSet buttonTex = json.has("button_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("button_texture"), TextureSet.class)
                        : json.has("texture")
                        ? new com.google.gson.Gson().fromJson(json.get("texture"), TextureSet.class)
                        : null;
                TextureSet dropdownTex = json.has("dropdown_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("dropdown_texture"), TextureSet.class)
                        : null;
                TextureSet itemNormalTex = json.has("item_normal_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("item_normal_texture"), TextureSet.class)
                        : null;
                TextureSet itemHighlightedTex = json.has("item_highlighted_texture")
                        ? new com.google.gson.Gson().fromJson(json.get("item_highlighted_texture"), TextureSet.class)
                        : null;
                TextProps textProps;
                if (json.has("text") && json.get("text").isJsonObject()) {
                    JsonObject textObj = json.get("text").getAsJsonObject();
                    textProps = parseText(textObj);
                    if (textObj.has("position")) {
                        Position pos = new com.google.gson.Gson().fromJson(textObj.get("position"), Position.class);
                        textProps = withOffset(textProps, pos.getX(), pos.getY());
                    }
                } else {
                    textProps = new TextProps();
                }
                List<Widget> children = new ArrayList<>();
                if (json.has("children") && json.get("children").isJsonArray()) {
                    JsonArray arr = json.get("children").getAsJsonArray();
                    for (JsonElement elem : arr) {
                        if (elem.isJsonObject()) {
                            children.add(create(elem.getAsJsonObject(), manager, executor));
                        }
                    }
                }
                yield new DropdownWidget(id, layout, variables, member, optionKey, values, buttonTex, dropdownTex, itemNormalTex, itemHighlightedTex, textProps, children);
            }
            case "edit_box" -> {
                EditBoxProps editProps = parseEditBox(json);
                yield new StringEditBoxWidget(id, layout, variables, member, editProps);
            }
            case "list" -> {
                ListProps listProps = parseList(json, manager, executor);
                yield new ListWidget(id, layout, variables, member, listProps);
            }
            case "box" -> {
                yield parseBox(json, layout, variables, member, id, manager, executor);
            }
            case "dynamic_list" -> {
                String dsType = json.has("data_source") ? json.get("data_source").getAsString() : "world_saves";
                Supplier<List<DynamicListData>> source = switch (dsType) {
                    case "servers" -> ServerListDataSource::load;
                    case "realms" -> RealmsDataSource::load;
                    default -> WorldSaveDataSource::load;
                };
                int rowH = json.has("row_height") ? json.get("row_height").getAsInt() : 36;
                int gap = json.has("gap") ? json.get("gap").getAsInt() : 2;
                String bg = json.has("background_color") ? json.get("background_color").getAsString() : null;
                JsonObject rowTemplate = null;
                JsonObject editBtnTemplate = null;
                JsonObject joinBtnTemplate = null;
                JsonObject deleteBtnTemplate = null;
                if (json.has("row_template") && json.get("row_template").isJsonObject()) {
                    JsonObject rowTemplateJson = json.get("row_template").getAsJsonObject();
                    rowTemplate = rowTemplateJson;
                    editBtnTemplate = resolveButtonTemplate(rowTemplateJson, "edit_button", manager);
                    joinBtnTemplate = resolveButtonTemplate(rowTemplateJson, "join_button", manager);
                    deleteBtnTemplate = resolveButtonTemplate(rowTemplateJson, "delete_button", manager);
                }
                DynamicListStyleConfig styleConfig = null;
                if (json.has("row_style") || json.has("scrollbar") || json.has("divider")) {
                    styleConfig = new com.google.gson.Gson().fromJson(
                            json, DynamicListStyleConfig.class);
                }
                yield new DynamicListWidget(id, layout, variables, member, source, executor,
                        rowH, gap, bg, rowTemplate, styleConfig,
                        editBtnTemplate, joinBtnTemplate, deleteBtnTemplate);
            }
            case "pack_list" -> {
                String sourceId = json.has("source") ? json.get("source").getAsString() : "resource_packs";
                String mode = json.has("mode") ? json.get("mode").getAsString() : "all";
                int entryH = json.has("entry_height") ? json.get("entry_height").getAsInt() : 48;
                int entryGap = json.has("entry_gap") ? json.get("entry_gap").getAsInt() : 2;
                String bg = json.has("background_color") ? json.get("background_color").getAsString() : null;
                String ordering = json.has("ordering") ? json.get("ordering").getAsString() : "active_first";

                PackListWidget.RowStyle activeRow = parseRowStyle(json, "active_row",
                    PackListWidget.RowStyle.DEFAULT_ACTIVE);
                PackListWidget.RowStyle unactiveRow = parseRowStyle(json, "unactive_row",
                    PackListWidget.RowStyle.DEFAULT_UNACTIVE);
                PackListWidget.DividerStyle divider = parseDividerStyle(json);

                PackDataSource dataSource = PackDataSource.getOrCreate(sourceId);
                yield new PackListWidget(id, layout, variables, member, dataSource, mode, executor,
                        entryH, entryGap, bg, activeRow, unactiveRow, divider, ordering);
            }
            case "content" -> {
                yield parseContent(json, layout, variables, member, id, manager, executor);
            }
            case "entity_display" -> {
                String entityType = json.has("entity_type") ? json.get("entity_type").getAsString() : "player";
                float scale = json.has("scale") ? json.get("scale").getAsFloat() : 30.0f;
                boolean lookAtMouse = !json.has("look_at_mouse") || json.get("look_at_mouse").getAsBoolean();
                String animState = "idle";
                float walkSpeed = 0.5F;
                boolean attackEnabled = false;
                if (json.has("animation")) {
                    JsonObject anim = json.get("animation").getAsJsonObject();
                    animState = anim.has("state") ? anim.get("state").getAsString() : "idle";
                    walkSpeed = anim.has("walk_speed") ? anim.get("walk_speed").getAsFloat() : 0.5F;
                    if (anim.has("attack")) {
                        JsonObject attack = anim.get("attack").getAsJsonObject();
                        attackEnabled = attack.has("enabled") && attack.get("enabled").getAsBoolean();
                    }
                }
                yield new EntityDisplayWidget(id, layout, variables, member, entityType, scale, lookAtMouse, animState, walkSpeed, attackEnabled);
            }
            default -> throw new IllegalArgumentException("Unknown widget type: " + type);
        };

        if (widget != null && json.has("state_v") && json.get("state_v").isJsonObject()) {
            JsonObject sv = json.get("state_v").getAsJsonObject();
            widget.setStateV(parseStateVNested(sv));
            widget.setStateVProps(parseStateVFlat(sv));
        }
        return widget;
    }

    private static void injectTextWidth(JsonObject json, Map<String, String> variables) {
        if (variables.containsKey("text_width")) return;

        String textKey = null;
        String rawText = null;

        if (json.has("text") && json.get("text").isJsonObject()) {
            JsonObject textObj = json.get("text").getAsJsonObject();
            if (textObj.has("text_key")) {
                textKey = textObj.get("text_key").getAsString();
            } else if (textObj.has("text")) {
                rawText = textObj.get("text").getAsString();
            }
        } else if (json.has("text_key")) {
            textKey = json.get("text_key").getAsString();
        } else if (json.has("text") && json.get("text").isJsonPrimitive()) {
            rawText = json.get("text").getAsString();
        }

        if (textKey != null) {
            variables.put("__text_key", textKey);
        } else if (rawText != null) {
            variables.put("__text_raw", rawText);
        }

        String resolvedText = null;
        if (textKey != null) {
            resolvedText = resolveTextWithVars(textKey, variables);
        } else if (rawText != null) {
            resolvedText = replaceVarPlaceholders(rawText, variables);
        }

        if (resolvedText != null && !resolvedText.isEmpty()) {
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.font != null) {
                    int width = mc.font.width(resolvedText);
                    variables.put("text_width", String.valueOf(width));
                    variables.put("text_height", String.valueOf(mc.font.lineHeight));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static String resolveTextWithVars(String key, Map<String, String> variables) {
        String resolvedKey = replaceVarPlaceholders(key, variables);
        try {
            return net.minecraft.network.chat.Component.translatable(resolvedKey).getString();
        } catch (Exception e) {
            return resolvedKey;
        }
    }

    private static String replaceVarPlaceholders(String input, Map<String, String> variables) {
        if (input == null || !input.contains("${")) return input;
        String result = input;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static LayoutProps parseLayout(JsonObject json) {
        String x = "0", y = "0", w = "auto", h = "auto";
        boolean enabled = true, visible = true;
        String condition = null;
        if (json.has("position")) {
            Position pos = new com.google.gson.Gson().fromJson(json.get("position"), Position.class);
            x = pos.getX() != null ? pos.getX() : "0";
            y = pos.getY() != null ? pos.getY() : "0";
        }
        if (json.has("size")) {
            Size size = new com.google.gson.Gson().fromJson(json.get("size"), Size.class);
            w = size.getWidth() != null ? size.getWidth() : "auto";
            h = size.getHeight() != null ? size.getHeight() : "auto";
        }
        if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
        if (json.has("visible")) visible = json.get("visible").getAsBoolean();
        if (json.has("condition")) condition = json.get("condition").getAsString();
        return new LayoutProps(x, y, w, h, enabled, visible, condition);
    }

    private static TextProps parseText(JsonObject json) {
        String text = json.has("text") ? json.get("text").getAsString() : null;
        String key = json.has("text_key") ? json.get("text_key").getAsString() : null;
        String keyDyn = json.has("text_key.d") ? json.get("text_key.d").getAsString() : null;
        String keyOpt = json.has("text_key.option") ? json.get("text_key.option").getAsString() : null;
        List<String> args = json.has("translation_args")
                ? new com.google.gson.Gson().fromJson(json.get("translation_args"), new TypeToken<List<String>>(){}.getType())
                : null;
        String color = json.has("color") ? json.get("color").getAsString() : "0xFFFFFF";
        String highlightedColor = json.has("highlighted_text_color") ? json.get("highlighted_text_color").getAsString() : "0xFFFFFF";
        String disabledColor = json.has("disabled_text_color") ? json.get("disabled_text_color").getAsString() : "0x808080";
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : 1.0f;
        boolean shadow = json.has("shadow") && json.get("shadow").getAsBoolean();
        String dynamicType = json.has("dynamicType") ? json.get("dynamicType").getAsString() : null;
        String ox = json.has("text_offset_x") ? json.get("text_offset_x").getAsString() : "0";
        String oy = json.has("text_offset_y") ? json.get("text_offset_y").getAsString() : "0";
        String hox = json.has("highlighted_text_offset_x") ? json.get("highlighted_text_offset_x").getAsString() : "0";
        String hoy = json.has("highlighted_text_offset_y") ? json.get("highlighted_text_offset_y").getAsString() : "0";
        String dox = json.has("disabled_text_offset_x") ? json.get("disabled_text_offset_x").getAsString() : "0";
        String doy = json.has("disabled_text_offset_y") ? json.get("disabled_text_offset_y").getAsString() : "0";
        String textAnimation = json.has("text_animation") ? json.get("text_animation").getAsString() : null;
        float animationSpeed = json.has("animation_speed") ? json.get("animation_speed").getAsFloat() : 0.0f;
        List<AnimationStep> animations = null;
        if (json.has("animations") && json.get("animations").isJsonArray()) {
            animations = new ArrayList<>();
            for (JsonElement elem : json.getAsJsonArray("animations")) {
                if (!elem.isJsonObject()) continue;
                JsonObject stepObj = elem.getAsJsonObject();
                String stepType = stepObj.has("type") ? stepObj.get("type").getAsString() : null;
                int stepDuration = stepObj.has("duration") ? stepObj.get("duration").getAsInt() : 0;
                int stepDelay = stepObj.has("delay") ? stepObj.get("delay").getAsInt() : 0;
                float stepSpeed = stepObj.has("speed") ? stepObj.get("speed").getAsFloat() : 0.0f;
                boolean stepLoop = stepObj.has("loop") && stepObj.get("loop").getAsBoolean();
                int stepStart = stepObj.has("start") ? stepObj.get("start").getAsInt() : -1;
                int stepEnd = stepObj.has("end") ? stepObj.get("end").getAsInt() : -1;
                animations.add(new AnimationStep(stepType, stepDuration, stepDelay, stepSpeed, stepLoop, stepStart, stepEnd));
            }
        }
        return new TextProps(text, key, keyDyn, args, keyOpt, color, highlightedColor, disabledColor,
                scale, shadow, dynamicType, ox, oy, hox, hoy, dox, doy, textAnimation, animationSpeed, animations);
    }

    private static StyleProps parseStyle(JsonObject json) {
        TextureSet tex = null;
        if (json.has("texture")) {
            tex = new com.google.gson.Gson().fromJson(json.get("texture"), TextureSet.class);
        } else if (json.has("image_texture")) {
            String img = json.get("image_texture").getAsString();
            tex = new TextureSet(img, img, img);
        } else if (json.has("icon_source")) {
            String icon = json.get("icon_source").getAsString();
            tex = new TextureSet(icon, icon, icon);
        }
        String bg = json.has("background_color") ? json.get("background_color").getAsString() : null;
        return new StyleProps(tex, bg);
    }

    private static SliderProps parseSlider(JsonObject json) {
        String key = json.has("option_key") ? json.get("option_key").getAsString() : null;
        double min = json.has("slider_min") ? json.get("slider_min").getAsDouble() : 0.0;
        double max = json.has("slider_max") ? json.get("slider_max").getAsDouble() : 1.0;
        double step = json.has("slider_step") ? json.get("slider_step").getAsDouble() : 0.01;
        return new SliderProps(key, min, max, step);
    }

    private static TextProps withOffset(TextProps p, String x, String y) {
        if (x == null) x = "0";
        if (y == null) y = "0";
        return new TextProps(p.text(), p.textKey(), p.textKeyDynamic(),
                p.translationArgs(), p.textKeyOption(),
                p.color(), p.highlightedColor(), p.disabledColor(),
                p.scale(), p.shadow(), p.dynamicType(),
                x, y, p.highlightedOffsetX(), p.highlightedOffsetY(),
                p.disabledOffsetX(), p.disabledOffsetY(), p.textAnimation(), p.animationSpeed(), p.animations());
    }

    private static CycleButtonProps parseCycleButton(JsonObject json) {
        String key = json.has("option_key") ? json.get("option_key").getAsString() : null;
        boolean displayOnly = json.has("display_only_value") && json.get("display_only_value").getAsBoolean();
        List<CycleValue> values = new ArrayList<>();
        if (json.has("values") && json.get("values").isJsonArray()) {
            JsonArray arr = json.get("values").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    String vKey = obj.has("key") ? obj.get("key").getAsString() : "";
                    String vTextKey = obj.has("text_key") ? obj.get("text_key").getAsString() : "";
                    values.add(new CycleValue(vKey, vTextKey));
                }
            }
        }
        return new CycleButtonProps(key, values, displayOnly);
    }

    private static List<CycleValue> parseCycleValues(JsonObject json) {
        List<CycleValue> values = new ArrayList<>();
        if (json.has("values") && json.get("values").isJsonArray()) {
            JsonArray arr = json.get("values").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    String vKey = obj.has("key") ? obj.get("key").getAsString() : "";
                    String vTextKey = obj.has("text_key") ? obj.get("text_key").getAsString() : "";
                    values.add(new CycleValue(vKey, vTextKey));
                }
            }
        }
        return values;
    }

    private static EditBoxProps parseEditBox(JsonObject json) {
        int maxLength = json.has("max_length") ? json.get("max_length").getAsInt() : 32;
        boolean bordered = !json.has("bordered") || json.get("bordered").getAsBoolean();
        String hint = json.has("hint") ? json.get("hint").getAsString() : null;
        String initialValue = json.has("initial_value") ? json.get("initial_value").getAsString() : "";
        String textColor = json.has("text_color") ? json.get("text_color").getAsString() : "0xE0E0E0";
        return new EditBoxProps(maxLength, bordered, hint, initialValue, textColor);
    }

    private static ListProps parseList(JsonObject json, ResourceManager manager, ActionExecutor executor) {
        int gap = json.has("gap") ? json.get("gap").getAsInt() : 0;
        List<RowDef> rows = new ArrayList<>();

        if (json.has("rows") && json.get("rows").isJsonArray()) {
            JsonArray arr = json.get("rows").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject rowObj = elem.getAsJsonObject();
                    int height = rowObj.has("height") ? rowObj.get("height").getAsInt() : 36;
                    String filterKey = rowObj.has("filter_key") ? rowObj.get("filter_key").getAsString() : null;
                    List<Widget> children = new ArrayList<>();
                    if (rowObj.has("children") && rowObj.get("children").isJsonArray()) {
                        JsonArray childArr = rowObj.get("children").getAsJsonArray();
                        for (JsonElement childElem : childArr) {
                            if (childElem.isJsonObject()) {
                                JsonObject childObj = childElem.getAsJsonObject();
                                Widget w;
                                if (childObj.has("ref") && childObj.get("ref").isJsonPrimitive()) {
                                    w = loadWidgetFromRef(childObj.get("ref").getAsString(), manager, executor, childObj);
                                } else {
                                    w = create(childObj, manager, executor);
                                }
                                if (w != null) children.add(w);
                            }
                        }
                    }
                    rows.add(new RowDef(height, filterKey, children));
                }
            }
        } else if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray arr = json.get("items").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject rowObj = elem.getAsJsonObject();
                    Widget w = create(rowObj, manager, executor);
                    if (w != null) {
                        rows.add(new RowDef(36, null, List.of(w)));
                    }
                }
            }
        }

        ListProps.ScrollbarDef scrollbar = null;
        if (json.has("scrollbar") && json.get("scrollbar").isJsonObject()) {
            JsonObject sb = json.get("scrollbar").getAsJsonObject();
            int sbWidth = sb.has("width") ? sb.get("width").getAsInt() : 6;

            ListProps.TrackDef track = new ListProps.TrackDef(null, null);
            if (sb.has("track") && sb.get("track").isJsonObject()) {
                JsonObject trk = sb.get("track").getAsJsonObject();
                TextureSet trackTex = trk.has("texture")
                        ? new com.google.gson.Gson().fromJson(trk.get("texture"), TextureSet.class)
                        : null;
                String trackColor = trk.has("color") ? trk.get("color").getAsString() : null;
                track = new ListProps.TrackDef(trackTex, trackColor);
            }

            ListProps.ThumbDef thumb = new ListProps.ThumbDef(null, null);
            if (sb.has("thumb") && sb.get("thumb").isJsonObject()) {
                JsonObject thb = sb.get("thumb").getAsJsonObject();
                TextureSet thumbTex = thb.has("texture")
                        ? new com.google.gson.Gson().fromJson(thb.get("texture"), TextureSet.class)
                        : null;
                String thumbColor = thb.has("color") ? thb.get("color").getAsString() : null;
                thumb = new ListProps.ThumbDef(thumbTex, thumbColor);
            }

            String sbX = sb.has("x") ? sb.get("x").getAsString() : null;
            String sbY = sb.has("y") ? sb.get("y").getAsString() : null;
            scrollbar = new ListProps.ScrollbarDef(sbWidth, sbX, sbY, track, thumb);
        }

        String backgroundColor = json.has("background_color")
                ? json.get("background_color").getAsString() : null;
        TextureSet backgroundTexture = json.has("background_texture")
                ? new com.google.gson.Gson().fromJson(json.get("background_texture"), TextureSet.class)
                : null;

        return new ListProps(gap, null, scrollbar, backgroundColor, backgroundTexture, rows);
    }

    private static Widget parseBox(JsonObject json, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                                   String id, ResourceManager manager, ActionExecutor executor) {
        String backgroundColor = json.has("background_color")
                ? json.get("background_color").getAsString() : null;
        String borderColor = json.has("border_color")
                ? json.get("border_color").getAsString() : null;
        TextureSet frameTexture = json.has("frame_texture")
                ? new com.google.gson.Gson().fromJson(json.get("frame_texture"), TextureSet.class)
                : null;
        int padTop = 0, padBottom = 0, padLeft = 0, padRight = 0;
        if (json.has("screen_padding") && json.get("screen_padding").isJsonObject()) {
            JsonObject sp = json.get("screen_padding").getAsJsonObject();
            padTop = sp.has("top") ? sp.get("top").getAsInt() : 0;
            padBottom = sp.has("bottom") ? sp.get("bottom").getAsInt() : 0;
            padLeft = sp.has("left") ? sp.get("left").getAsInt() : 0;
            padRight = sp.has("right") ? sp.get("right").getAsInt() : 0;
        }
        if (json.has("padding_top")) padTop = json.get("padding_top").getAsInt();
        if (json.has("padding_bottom")) padBottom = json.get("padding_bottom").getAsInt();
        if (json.has("padding_left")) padLeft = json.get("padding_left").getAsInt();
        if (json.has("padding_right")) padRight = json.get("padding_right").getAsInt();

        Map<String, Widget> elements = new LinkedHashMap<>();
        String defaultId = json.has("default_id") ? json.get("default_id").getAsString() : null;

        if (json.has("elements") && json.get("elements").isJsonArray()) {
            JsonArray arr = json.get("elements").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject elObj = elem.getAsJsonObject();
                    String elId = elObj.has("id") ? elObj.get("id").getAsString() : null;
                    Widget widget;

                    if (elObj.has("ref") && elObj.get("ref").isJsonPrimitive()) {
                        widget = loadWidgetFromRef(elObj.get("ref").getAsString(), manager, executor, elObj);
                    } else if (elObj.has("widget") && elObj.get("widget").isJsonObject()) {
                        widget = create(elObj.get("widget").getAsJsonObject(), manager, executor);
                    } else {
                        widget = create(elObj, manager, executor);
                    }
                    if (widget != null) {
                        elements.put(elId, widget);
                    }
                }
            }
        }

        if (defaultId == null) {
            LOGGER.warn("Box {} has no elements", id);
            defaultId = "";
        }

        BoxWidget boxWidget = new BoxWidget(id, layout, variables, member, elements, defaultId,
                backgroundColor, borderColor, frameTexture,
                padTop, padBottom, padLeft, padRight);

        if (executor != null && id != null) {
            executor.registerBox(id, boxWidget);
        }

        return boxWidget;
    }

    private static Widget loadWidgetFromRef(String ref, ResourceManager manager, ActionExecutor executor, JsonObject override) {
        try {
            net.minecraft.resources.ResourceLocation resLoc = net.minecraft.resources.ResourceLocation.tryParse(ref);
            if (resLoc == null) {
                LOGGER.warn("Invalid widget reference: {}", ref);
                return null;
            }
            net.minecraft.server.packs.resources.Resource resource = manager.getResource(resLoc).orElse(null);
            if (resource == null) {
                LOGGER.warn("Widget reference not found: {}", ref);
                return null;
            }
            java.io.InputStream inputStream = resource.open();
            String jsonStr = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            inputStream.close();
            JsonObject jsonObj = new com.google.gson.Gson().fromJson(jsonStr, JsonObject.class);

            if (override != null) {
                if (override.has("position")) jsonObj.add("position", override.get("position"));
                if (override.has("size")) jsonObj.add("size", override.get("size"));
                if (override.has("enabled")) jsonObj.addProperty("enabled", override.get("enabled").getAsBoolean());
                if (override.has("visible")) jsonObj.addProperty("visible", override.get("visible").getAsBoolean());
                if (override.has("box_id")) jsonObj.addProperty("box_id", override.get("box_id").getAsString());
                if (override.has("target_id")) jsonObj.addProperty("target_id", override.get("target_id").getAsString());
                if (override.has("action")) jsonObj.add("action", override.get("action"));
                if (override.has("id")) jsonObj.addProperty("id", override.get("id").getAsString());
                if (override.has("text")) {
                    if (jsonObj.has("text") && jsonObj.get("text").isJsonObject()
                            && override.get("text").isJsonObject()) {
                        JsonObject existing = jsonObj.get("text").getAsJsonObject();
                        JsonObject overrideText = override.get("text").getAsJsonObject();
                        for (String key : overrideText.keySet()) {
                            existing.add(key, overrideText.get(key));
                        }
                    } else {
                        jsonObj.add("text", override.get("text"));
                    }
                }
                if (override.has("variables") && override.get("variables").isJsonObject()) {
                    if (jsonObj.has("variables") && jsonObj.get("variables").isJsonObject()) {
                        JsonObject existing = jsonObj.get("variables").getAsJsonObject();
                        JsonObject overrideVars = override.get("variables").getAsJsonObject();
                        for (String key : overrideVars.keySet()) {
                            existing.add(key, overrideVars.get(key));
                        }
                    } else {
                        jsonObj.add("variables", override.get("variables"));
                    }
                }
                if (override.has("member") && override.get("member").isJsonObject()) {
                    if (jsonObj.has("member") && jsonObj.get("member").isJsonObject()) {
                        JsonObject existing = jsonObj.get("member").getAsJsonObject();
                        JsonObject overrideMember = override.get("member").getAsJsonObject();
                        for (String key : overrideMember.keySet()) {
                            existing.add(key, overrideMember.get(key));
                        }
                    } else {
                        jsonObj.add("member", override.get("member"));
                    }
                }
            }

            return create(jsonObj, manager, executor);
        } catch (Exception e) {
            LOGGER.error("Failed to load widget from ref: {}", ref, e);
            return null;
        }
    }

    private static Widget parseContent(JsonObject json, LayoutProps layout, Map<String, String> variables, Map<String, String> member,
                                       String id, ResourceManager manager, ActionExecutor executor) {
        int gap = json.has("gap") ? json.get("gap").getAsInt() : 2;
        String backgroundColor = json.has("background_color")
                ? json.get("background_color").getAsString() : null;
        String borderColor = json.has("border_color")
                ? json.get("border_color").getAsString() : null;

        ListProps.ScrollbarDef scrollbar = null;
        if (json.has("scrollbar") && json.get("scrollbar").isJsonObject()) {
            JsonObject sb = json.get("scrollbar").getAsJsonObject();
            int sbWidth = sb.has("width") ? sb.get("width").getAsInt() : 6;

            ListProps.TrackDef track = new ListProps.TrackDef(null, null);
            if (sb.has("track") && sb.get("track").isJsonObject()) {
                JsonObject trk = sb.get("track").getAsJsonObject();
                TextureSet trackTex = trk.has("texture")
                        ? new com.google.gson.Gson().fromJson(trk.get("texture"), TextureSet.class)
                        : null;
                String trackColor = trk.has("color") ? trk.get("color").getAsString() : null;
                track = new ListProps.TrackDef(trackTex, trackColor);
            }

            ListProps.ThumbDef thumb = new ListProps.ThumbDef(null, null);
            if (sb.has("thumb") && sb.get("thumb").isJsonObject()) {
                JsonObject thb = sb.get("thumb").getAsJsonObject();
                TextureSet thumbTex = thb.has("texture")
                        ? new com.google.gson.Gson().fromJson(thb.get("texture"), TextureSet.class)
                        : null;
                String thumbColor = thb.has("color") ? thb.get("color").getAsString() : null;
                thumb = new ListProps.ThumbDef(thumbTex, thumbColor);
            }

            String sbX = sb.has("x") ? sb.get("x").getAsString() : null;
            String sbY = sb.has("y") ? sb.get("y").getAsString() : null;
            scrollbar = new ListProps.ScrollbarDef(sbWidth, sbX, sbY, track, thumb);
        }

        List<Widget> buttons = new ArrayList<>();
        if (json.has("buttons") && json.get("buttons").isJsonArray()) {
            JsonArray arr = json.get("buttons").getAsJsonArray();
            for (JsonElement elem : arr) {
                if (elem.isJsonObject()) {
                    JsonObject btnObj = elem.getAsJsonObject();
                    if (btnObj.has("ref") && btnObj.get("ref").isJsonPrimitive()) {
                        Widget w = loadWidgetFromRef(btnObj.get("ref").getAsString(), manager, executor, btnObj);
                        if (w != null) buttons.add(w);
                    } else {
                        buttons.add(create(btnObj, manager, executor));
                    }
                }
            }
        }

        String direction = json.has("direction") ? json.get("direction").getAsString() : "vertical";

        TextureSet backgroundTexture = json.has("background_texture")
                ? new com.google.gson.Gson().fromJson(json.get("background_texture"), TextureSet.class)
                : null;

        return new ContentWidget(id, layout, variables, member, buttons, gap, scrollbar, backgroundColor, borderColor, backgroundTexture, direction);
    }

    private static Map<String, Map<String, String>> parseStateVariables(JsonObject json) {
        if (!json.has("state_variables") || !json.get("state_variables").isJsonObject()) return null;
        JsonObject states = json.get("state_variables").getAsJsonObject();
        Map<String, Map<String, String>> result = new HashMap<>();
        for (String state : states.keySet()) {
            JsonObject vars = states.get(state).getAsJsonObject();
            Map<String, String> map = new HashMap<>();
            for (String key : vars.keySet()) {
                map.put(key, vars.get(key).getAsString());
            }
            result.put(state, map);
        }
        return result;
    }

    private static Map<String, Map<String, Map<String, String>>> parseStateVNested(JsonObject stateV) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<>();
        for (String state : stateV.keySet()) {
            JsonObject stateObj = stateV.get(state).getAsJsonObject();
            Map<String, Map<String, String>> sectionMap = new HashMap<>();
            for (String section : stateObj.keySet()) {
                if (stateObj.get(section).isJsonObject()) {
                    JsonObject props = stateObj.get(section).getAsJsonObject();
                    Map<String, String> propMap = new HashMap<>();
                    for (String prop : props.keySet()) {
                        propMap.put(prop, props.get(prop).getAsString());
                    }
                    sectionMap.put(section, propMap);
                }
            }
            result.put(state, sectionMap);
        }
        return result;
    }

    private static Map<String, Map<String, String>> parseStateVFlat(JsonObject stateV) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (String state : stateV.keySet()) {
            JsonObject stateObj = stateV.get(state).getAsJsonObject();
            Map<String, String> flatMap = new HashMap<>();
            for (String key : stateObj.keySet()) {
                if (stateObj.get(key).isJsonPrimitive()) {
                    flatMap.put(key, stateObj.get(key).getAsString());
                }
            }
            result.put(state, flatMap);
        }
        return result;
    }

    private static PackListWidget.RowStyle parseRowStyle(JsonObject json, String key,
                                                          PackListWidget.RowStyle defaultStyle) {
        if (!json.has(key) || !json.get(key).isJsonObject()) return defaultStyle;
        JsonObject obj = json.get(key).getAsJsonObject();
        String bg = obj.has("background_color") ? obj.get("background_color").getAsString() : defaultStyle.backgroundColor;
        String tex = obj.has("background_texture") ? obj.get("background_texture").getAsString() : defaultStyle.backgroundTexture;
        String text = obj.has("text_color") ? obj.get("text_color").getAsString() : defaultStyle.textColor;
        String hover = obj.has("hover_color") ? obj.get("hover_color").getAsString() : defaultStyle.hoverColor;
        String toggle = obj.has("toggle_color") ? obj.get("toggle_color").getAsString() : defaultStyle.toggleColor;
        String toggleHover = obj.has("toggle_hover_color") ? obj.get("toggle_hover_color").getAsString() : defaultStyle.toggleHoverColor;
        return new PackListWidget.RowStyle(bg, tex, text, hover, toggle, toggleHover);
    }

    private static PackListWidget.DividerStyle parseDividerStyle(JsonObject json) {
        if (!json.has("divider") || !json.get("divider").isJsonObject())
            return PackListWidget.DividerStyle.DEFAULT;
        JsonObject obj = json.get("divider").getAsJsonObject();
        int h = obj.has("height") ? obj.get("height").getAsInt() : PackListWidget.DividerStyle.DEFAULT.height;
        String color = obj.has("color") ? obj.get("color").getAsString() : PackListWidget.DividerStyle.DEFAULT.color;
        String texture = obj.has("texture") ? obj.get("texture").getAsString() : PackListWidget.DividerStyle.DEFAULT.texture;
        return new PackListWidget.DividerStyle(h, color, texture);
    }

    private static JsonObject resolveButtonTemplate(JsonObject rowTemplateJson, String key,
                                                     ResourceManager manager) {
        if (!rowTemplateJson.has(key)) return null;
        JsonElement btnElem = rowTemplateJson.get(key);
        if (!btnElem.isJsonObject()) return null;
        JsonObject btnObj = btnElem.getAsJsonObject();

        if (btnObj.has("ref")) {
            String refPath = btnObj.get("ref").getAsString();
            try {
                net.minecraft.resources.ResourceLocation refId =
                        net.minecraft.resources.ResourceLocation.parse(refPath);
                java.util.Optional<net.minecraft.server.packs.resources.Resource> res =
                        manager.getResource(refId);
                if (res.isPresent()) {
                    try (java.io.InputStream in = res.get().open()) {
                        String content = new String(in.readAllBytes(),
                                java.nio.charset.StandardCharsets.UTF_8);
                        JsonObject refObj = JsonParser.parseString(content).getAsJsonObject();
                        for (String k : btnObj.keySet()) {
                            if (!k.equals("ref")) {
                                refObj.add(k, btnObj.get(k).deepCopy());
                            }
                        }
                        return refObj;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to resolve button ref '{}' for key '{}'", refPath, key, e);
            }
            return null;
        }
        return btnObj;
    }
}