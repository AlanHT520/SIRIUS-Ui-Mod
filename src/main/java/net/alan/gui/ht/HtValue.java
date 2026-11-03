

package net.alan.gui.ht;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
public sealed interface HtValue {
    record Null() implements HtValue {}
    record Bool(boolean value) implements HtValue {}
    record Num(long value) implements HtValue {}
    record Real(double value) implements HtValue {}
    record Str(String value) implements HtValue {}
    record Color(int argb) implements HtValue {}
    record Arr(List<HtValue> items) implements HtValue, Iterable<HtValue> {
        @Override
        public java.util.Iterator<HtValue> iterator() {
            return items.iterator();
        }

        @Override
        public HtValue deepCopy() {
            List<HtValue> copy = new ArrayList<>(items.size());
            for (HtValue item : items) {
                copy.add(item.deepCopy());
            }
            return new Arr(copy);
        }
    }
    record Obj(Map<String, HtValue> fields) implements HtValue {
        public void add(String key, HtValue value) {
            fields.put(key, value);
        }

        public void addProperty(String key, String value) {
            fields.put(key, str(value));
        }

        public void addProperty(String key, int value) {
            fields.put(key, num(value));
        }

        public void addProperty(String key, boolean value) {
            fields.put(key, bool(value));
        }

        @Override
        public HtValue deepCopy() {
            Obj copy = obj();
            for (Map.Entry<String, HtValue> entry : fields.entrySet()) {
                copy.fields.put(entry.getKey(), entry.getValue().deepCopy());
            }
            return copy;
        }
    }
    HtValue NULL = new Null();

    // ------------------------------------------------------------------
    // 元素读取 API
    //
    // HT 解释结果直接以 HtValue 树交给控件层，不再序列化成 Gson 的 JsonObject。
    // 这里的取值为「元素模型」服务：缺键返回 null / 默认值，类型不符抛 HtException。
    // ------------------------------------------------------------------

    default boolean has(String key) {
        return this instanceof Obj o && o.fields().containsKey(key);
    }

    default HtValue get(String key) {
        return this instanceof Obj o ? o.fields().get(key) : null;
    }

    default boolean isJsonObject() {
        return this instanceof Obj;
    }

    default boolean isJsonArray() {
        return this instanceof Arr;
    }

    default boolean isJsonPrimitive() {
        return this instanceof Str || this instanceof Num || this instanceof Real
                || this instanceof Bool || this instanceof Color;
    }

    default Obj getAsJsonObject() {
        if (this instanceof Obj o) {
            return o;
        }
        throw new HtException("期望对象，实际是 " + typeName());
    }

    default Obj getAsJsonObject(String key) {
        HtValue value = get(key);
        return value == null ? null : value.getAsJsonObject();
    }

    default Arr getAsJsonArray() {
        if (this instanceof Arr a) {
            return a;
        }
        throw new HtException("期望数组，实际是 " + typeName());
    }

    default Arr getAsJsonArray(String key) {
        HtValue value = get(key);
        return value == null ? null : value.getAsJsonArray();
    }

    /** 标量视图（兼容原先 Gson 的 JsonPrimitive 调用形状）。 */
    default HtValue getAsJsonPrimitive() {
        if (isJsonPrimitive()) {
            return this;
        }
        throw new HtException("期望标量，实际是 " + typeName());
    }

    default boolean isString() {
        return this instanceof Str;
    }

    default java.util.Set<String> keySet() {
        if (this instanceof Obj o) {
            return o.fields().keySet();
        }
        throw new HtException("期望对象，实际是 " + typeName());
    }

    default String getAsString() {
        return asString();
    }

    default int getAsInt() {
        return (int) asLong();
    }

    default float getAsFloat() {
        return (float) asDouble();
    }

    default double getAsDouble() {
        return asDouble();
    }

    default boolean getAsBoolean() {
        return switch (this) {
            case Bool b -> b.value();
            case Str s -> Boolean.parseBoolean(s.value().trim());
            case Num n -> n.value() != 0;
            case Real r -> r.value() != 0;
            default -> false;
        };
    }

    /** 元素模型也需要不可变拷贝（动态列表的每行模板会复制一份再注入数据）。 */
    default HtValue deepCopy() {
        return this;
    }
    HtValue TRUE = new Bool(true);
    HtValue FALSE = new Bool(false);
    static Bool bool(boolean v) {
        return v ? new Bool(true) : new Bool(false);
    }
    static Num num(long v) {
        return new Num(v);
    }
    static Real real(double v) {
        return new Real(v);
    }
    static Str str(String v) {
        return new Str(v == null ? "" : v);
    }
    static Obj obj() {
        return new Obj(new LinkedHashMap<>());
    }
    static Arr arr() {
        return new Arr(new ArrayList<>());
    }
    static Arr arr(List<HtValue> items) {
        return new Arr(new ArrayList<>(items));
    }
    default String typeName() {
        return switch (this) {
            case Null ignored -> "null";
            case Bool ignored -> "bool";
            case Num ignored -> "int";
            case Real ignored -> "float";
            case Str ignored -> "string";
            case Color ignored -> "color";
            case Arr ignored -> "list";
            case Obj ignored -> "object";
        };
    }
    default String asString() {
        return switch (this) {
            case Null ignored -> "";
            case Bool b -> b.value() ? "true" : "false";
            case Num n -> Long.toString(n.value());
            case Real r -> formatDouble(r.value());
            case Str s -> s.value();
            case Color c -> String.format("0x%08X", c.argb());
            case Arr a -> throw new HtException("列表无法直接当作字符串使用");
            case Obj o -> throw new HtException("对象无法直接当作字符串使用");
        };
    }
    default String exprString() {
        return asString();
    }
    default boolean truthy() {
        return switch (this) {
            case Null ignored -> false;
            case Bool b -> b.value();
            case Num n -> n.value() != 0;
            case Real r -> r.value() != 0;
            case Str s -> !s.value().isEmpty();
            case Color c -> c.argb() != 0;
            case Arr a -> !a.items().isEmpty();
            case Obj o -> !o.fields().isEmpty();
        };
    }
    default long asLong() {
        return switch (this) {
            case Num n -> n.value();
            case Real r -> Math.round(r.value());
            case Bool b -> b.value() ? 1 : 0;
            case Color c -> c.argb() & 0xFFFFFFFFL;
            case Str s -> {
                try {
                    yield Long.parseLong(s.value().trim());
                } catch (NumberFormatException e) {
                    throw new HtException("字符串 \"" + s.value() + "\" 不能转成整数");
                }
            }
            default -> throw new HtException(typeName() + " 不能转成整数");
        };
    }
    default double asDouble() {
        return switch (this) {
            case Num n -> (double) n.value();
            case Real r -> r.value();
            case Bool b -> b.value() ? 1.0 : 0.0;
            case Str s -> {
                try {
                    yield Double.parseDouble(s.value().trim());
                } catch (NumberFormatException e) {
                    throw new HtException("字符串 \"" + s.value() + "\" 不能转成浮点数");
                }
            }
            default -> throw new HtException(typeName() + " 不能转成浮点数");
        };
    }
    default boolean isNumeric() {
        return this instanceof Num || this instanceof Real;
    }
    static String formatDouble(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return Long.toString((long) v);
        }
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }
}
