

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
    record Arr(List<HtValue> items) implements HtValue {}
    record Obj(Map<String, HtValue> fields) implements HtValue {}
    HtValue NULL = new Null();
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
