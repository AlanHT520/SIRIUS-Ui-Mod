

package net.alan.gui.ht;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
public final class HtJson {
    private HtJson() {
    }
    public static JsonElement toJson(HtValue value) {
        return switch (value) {
            case HtValue.Null ignored -> JsonNull.INSTANCE;
            case HtValue.Bool b -> new JsonPrimitive(b.value());
            case HtValue.Num n -> new JsonPrimitive(n.value());
            case HtValue.Real r -> new JsonPrimitive(r.value());
            case HtValue.Str s -> new JsonPrimitive(s.value());
            case HtValue.Color c -> new JsonPrimitive(String.format("0x%08X", c.argb()));
            case HtValue.Arr a -> toJsonArray(a.items());
            case HtValue.Obj o -> toJsonObject(o);
        };
    }
    public static JsonObject toJsonObject(HtValue.Obj obj) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, HtValue> entry : obj.fields().entrySet()) {
            json.add(entry.getKey(), toJson(entry.getValue()));
        }
        return json;
    }
    public static JsonArray toJsonArray(List<HtValue> values) {
        JsonArray array = new JsonArray();
        for (HtValue value : values) {
            array.add(toJson(value));
        }
        return array;
    }
}
