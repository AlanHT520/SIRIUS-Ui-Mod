package net.alan.gui.ht;

import net.alan.gui.data.Action;
import net.alan.gui.data.screen.BackgroundLayer;
import net.alan.gui.data.screen.PanoramaConfig;
import net.alan.gui.data.screen.Slide;
import net.alan.gui.data.screen.SlideGroup;
import net.alan.gui.data.widget.DynamicListStyleConfig;
import net.alan.gui.data.widget.Position;
import net.alan.gui.data.widget.Size;
import net.alan.gui.data.widget.TextureSet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link HtValue} → 控件层 POJO 的直接映射。
 *
 * <p>这取代了原先散落在 {@code WidgetFactory} / {@code JsonLoader} 里的一批
 * {@code new Gson().fromJson(...)}：HT 解释出的值树直接喂给控件层，
 * 不再经过 Gson，也不再序列化成 {@code JsonObject}。</p>
 *
 * <p>缺键时返回 null / 保持 POJO 默认值，与 Gson 反序列化的行为一致。</p>
 */
public final class HtData {

    private HtData() {
    }

    /** 字符串表：{@code variables} / {@code member} / {@code sharedState} / {@code params}。 */
    public static Map<String, String> stringMap(HtValue value) {
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, HtValue> entry : obj.fields().entrySet()) {
            map.put(entry.getKey(), text(entry.getValue()));
        }
        return map;
    }

    /** 字符串数组：{@code translation_args} / {@code play_groups} / {@code splashes}。 */
    public static List<String> stringList(HtValue value) {
        if (!(value instanceof HtValue.Arr arr)) {
            return null;
        }
        List<String> list = new ArrayList<>(arr.items().size());
        for (HtValue item : arr.items()) {
            list.add(text(item));
        }
        return list;
    }

    public static TextureSet textureSet(HtValue value) {
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        TextureSet set = new TextureSet();
        if (obj.has("normal")) {
            set.setNormal(text(obj.get("normal")));
        }
        if (obj.has("highlighted")) {
            set.setHighlighted(text(obj.get("highlighted")));
        }
        if (obj.has("disabled")) {
            set.setDisabled(text(obj.get("disabled")));
        }
        if (obj.has("ing")) {
            set.setIng(text(obj.get("ing")));
        }
        if (obj.has("selected")) {
            set.setSelected(text(obj.get("selected")));
        }
        if (obj.has("filled")) {
            set.setFilled(text(obj.get("filled")));
        }
        return set;
    }

    public static Position position(HtValue value) {
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        Position position = new Position();
        if (obj.has("x")) {
            position.setX(text(obj.get("x")));
        }
        if (obj.has("y")) {
            position.setY(text(obj.get("y")));
        }
        return position;
    }

    public static Size size(HtValue value) {
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        Size size = new Size();
        if (obj.has("width")) {
            size.setWidth(text(obj.get("width")));
        }
        if (obj.has("height")) {
            size.setHeight(text(obj.get("height")));
        }
        return size;
    }

    public static Action action(HtValue value) {
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        Action action = new Action();
        if (obj.has("type")) {
            action.setType(text(obj.get("type")));
        }
        if (obj.has("screenId")) {
            action.setScreenId(text(obj.get("screenId")));
        }
        if (obj.has("url")) {
            action.setUrl(text(obj.get("url")));
        }
        if (obj.has("target")) {
            action.setTarget(text(obj.get("target")));
        }
        if (obj.has("content")) {
            action.setContent(text(obj.get("content")));
        }
        if (obj.has("varName")) {
            action.setVarName(text(obj.get("varName")));
        }
        if (obj.has("varValue")) {
            action.setVarValue(text(obj.get("varValue")));
        }
        if (obj.has("boxId")) {
            action.setBoxId(text(obj.get("boxId")));
        }
        if (obj.has("targetId")) {
            action.setTargetId(text(obj.get("targetId")));
        }
        if (obj.has("confirm_with")) {
            action.setConfirmWith(text(obj.get("confirm_with")));
        }
        if (obj.has("card")) {
            action.setCard(obj.get("card"));
        }
        if (obj.has("params")) {
            action.setParams(stringMap(obj.get("params")));
        }
        return action;
    }

    /** {@link Action} → HtValue（动态列表在运行时改写行内按钮动作时需要）。 */
    public static HtValue.Obj toValue(Action action) {
        HtValue.Obj obj = HtValue.obj();
        if (action == null) {
            return obj;
        }
        put(obj, "type", action.getType());
        put(obj, "screenId", action.getScreenId());
        put(obj, "url", action.getUrl());
        put(obj, "target", action.getTarget());
        put(obj, "content", action.getContent());
        put(obj, "varName", action.getVarName());
        put(obj, "varValue", action.getVarValue());
        put(obj, "boxId", action.getBoxId());
        put(obj, "targetId", action.getTargetId());
        put(obj, "confirm_with", action.getConfirmWith());
        if (action.getCard() != null) {
            obj.fields().put("card", action.getCard());
        }
        if (action.getParams() != null) {
            HtValue.Obj params = HtValue.obj();
            for (Map.Entry<String, String> entry : action.getParams().entrySet()) {
                params.fields().put(entry.getKey(), HtValue.str(entry.getValue()));
            }
            obj.fields().put("params", params);
        }
        return obj;
    }

    private static void put(HtValue.Obj obj, String key, String value) {
        if (value != null) {
            obj.fields().put(key, HtValue.str(value));
        }
    }
    public static DynamicListStyleConfig dynamicListStyle(HtValue.Obj json) {
        DynamicListStyleConfig config = new DynamicListStyleConfig();
        if (json.get("row_style") instanceof HtValue.Obj row) {
            DynamicListStyleConfig.RowStyleConfig style = new DynamicListStyleConfig.RowStyleConfig();
            style.background_color = string(row, "background_color", style.background_color);
            style.background_color_alt = string(row, "background_color_alt", style.background_color_alt);
            style.hover_background_color = string(row, "hover_background_color",
                    style.hover_background_color);
            style.selected_background_color = string(row, "selected_background_color", null);
            style.background_texture = string(row, "background_texture", null);
            style.text_color = string(row, "text_color", style.text_color);
            style.text_color_alt = string(row, "text_color_alt", style.text_color_alt);
            style.shadow = row.has("shadow") ? row.get("shadow").getAsBoolean() : style.shadow;
            config.setRowStyle(style);
        }
        if (json.get("scrollbar") instanceof HtValue.Obj bar) {
            DynamicListStyleConfig.ScrollbarConfig style =
                    new DynamicListStyleConfig.ScrollbarConfig();
            style.track_color = string(bar, "track_color", style.track_color);
            style.thumb_color = string(bar, "thumb_color", style.thumb_color);
            style.width = bar.has("width") ? bar.get("width").getAsInt() : style.width;
            config.setScrollbar(style);
        }
        if (json.get("divider") instanceof HtValue.Obj divider) {
            DynamicListStyleConfig.DividerConfig style =
                    new DynamicListStyleConfig.DividerConfig();
            style.height = divider.has("height")
                    ? divider.get("height").getAsInt() : style.height;
            style.color = string(divider, "color", style.color);
            config.setDivider(style);
        }
        if (json.has("scroll_speed")) {
            config.setScrollSpeed(json.get("scroll_speed").getAsInt());
        }
        if (json.has("scrollbar_min_height")) {
            config.setScrollbarMinHeight(json.get("scrollbar_min_height").getAsInt());
        }
        if (json.has("scrollbar_padding")) {
            config.setScrollbarPadding(json.get("scrollbar_padding").getAsInt());
        }
        if (json.has("search_gap")) {
            config.setSearchGap(json.get("search_gap").getAsInt());
        }
        return config;
    }

    /** {@code panorama = {...}} 或 {@code panorama = "vanilla"}。 */
    public static PanoramaConfig panorama(HtValue value) {
        PanoramaConfig config = new PanoramaConfig();
        if (value instanceof HtValue.Str type) {
            config.setType(type.value());
            return config;
        }
        if (!(value instanceof HtValue.Obj obj)) {
            return null;
        }
        if (obj.has("type")) {
            config.setType(text(obj.get("type")));
        }
        if (obj.has("texture")) {
            config.setTexture(text(obj.get("texture")));
        }
        if (obj.has("defaultTime")) {
            config.setDefaultTime(obj.get("defaultTime").getAsInt());
        }
        if (obj.has("playGroups")) {
            config.setPlayGroups(stringList(obj.get("playGroups")));
        }
        if (obj.has("playAfter")) {
            config.setPlayAfter(text(obj.get("playAfter")));
        }
        if (obj.get("groups") instanceof HtValue.Arr groups) {
            List<SlideGroup> parsed = new ArrayList<>();
            for (HtValue groupValue : groups.items()) {
                if (!(groupValue instanceof HtValue.Obj group)) {
                    continue;
                }
                SlideGroup slideGroup = new SlideGroup();
                if (group.has("id")) {
                    slideGroup.setId(text(group.get("id")));
                }
                if (group.has("playCount")) {
                    slideGroup.setPlayCount(group.get("playCount").getAsInt());
                }
                if (group.get("slides") instanceof HtValue.Arr slides) {
                    List<Slide> parsedSlides = new ArrayList<>();
                    for (HtValue slideValue : slides.items()) {
                        if (!(slideValue instanceof HtValue.Obj slide)) {
                            continue;
                        }
                        Slide parsedSlide = new Slide();
                        if (slide.has("texture")) {
                            parsedSlide.setTexture(text(slide.get("texture")));
                        }
                        if (slide.has("time")) {
                            parsedSlide.setTime(slide.get("time").getAsInt());
                        }
                        if (slide.has("transition")) {
                            parsedSlide.setTransition(text(slide.get("transition")));
                        }
                        if (slide.has("transition_duration")) {
                            parsedSlide.setTransitionDuration(
                                    slide.get("transition_duration").getAsInt());
                        }
                        parsedSlides.add(parsedSlide);
                    }
                    slideGroup.setSlides(parsedSlides);
                }
                parsed.add(slideGroup);
            }
            config.setGroups(parsed);
        }
        return config;
    }

    /** 屏幕级 {@code graphicsDraw = [ {x,y,width,height,color} ]}。 */
    public static List<BackgroundLayer> backgroundLayers(List<HtValue> values) {
        List<BackgroundLayer> layers = new ArrayList<>();
        if (values == null) {
            return layers;
        }
        for (HtValue value : values) {
            if (!(value instanceof HtValue.Obj obj)) {
                continue;
            }
            BackgroundLayer layer = new BackgroundLayer();
            if (obj.has("x")) {
                layer.setX(text(obj.get("x")));
            }
            if (obj.has("y")) {
                layer.setY(text(obj.get("y")));
            }
            if (obj.has("width")) {
                layer.setWidth(text(obj.get("width")));
            }
            if (obj.has("height")) {
                layer.setHeight(text(obj.get("height")));
            }
            if (obj.has("color")) {
                layer.setColor(text(obj.get("color")));
            }
            layers.add(layer);
        }
        return layers;
    }

    // ------------------------------------------------------------------ 内部

    /** 任意标量转字符串；颜色统一成 0xAARRGGBB。 */
    private static String text(HtValue value) {
        if (value == null) {
            return null;
        }
        return value instanceof HtValue.Color color
                ? String.format("0x%08X", color.argb()) : value.asString();
    }

    private static String string(HtValue.Obj obj, String key, String fallback) {
        return obj.has(key) ? text(obj.get(key)) : fallback;
    }
}
