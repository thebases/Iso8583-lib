package com.base.iso.util.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

/**
 * Built-in JSON support (no third-party library; JDK-only, Android API 24 safe, thread-safe).
 *
 * <pre>
 *   Map&lt;?,?&gt; root = (Map&lt;?,?&gt;) Json.parse(text);
 *   String out     = Json.write(root);        // compact
 *   String pretty  = Json.writePretty(root);  // 2-space indent
 * </pre>
 *
 * Parsed types: {@code Map<String,Object>} (keeps key order), {@code List<Object>}, {@code String},
 * {@code Long}, {@code BigDecimal} (fractions / exponents / beyond long), {@code Boolean}, null.
 * Writable types: the above plus any {@link Map} (keys via toString), {@link Collection}, Object[],
 * Integer/Short/Byte/BigInteger, finite Double/Float.
 */
public final class Json {

    private Json() {}

    /**
     * Parses a JSON document.
     * @throws IllegalArgumentException on malformed input (message includes the offset)
     */
    public static Object parse(String json) {
        return JsonReader.parse(json);
    }

    /** Compact JSON text for {@code value}. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, null, "", 0);
        return sb.toString();
    }

    /** Indented JSON text (2 spaces) for {@code value}. */
    public static String writePretty(Object value) {
        StringBuilder sb = new StringBuilder();
        write(sb, value, "  ", "", 0);
        return sb.toString();
    }

    /** Appends {@code s} as a quoted, escaped JSON string. */
    public static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    // control chars and the JS-hostile line separators are \\u-escaped
                    if (c < 0x20 || c == ' ' || c == ' ')
                        sb.append(String.format("\\u%04x", (int) c));
                    else
                        sb.append(c);
            }
        }
        sb.append('"');
    }

    private static void write(StringBuilder sb, Object v, String step, String indent, int depth) {
        if (depth > JsonReader.MAX_DEPTH)
            throw new IllegalArgumentException("JSON value nested deeper than " + JsonReader.MAX_DEPTH);
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String || v instanceof Character) {
            quote(sb, v.toString());
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d))
                throw new IllegalArgumentException("JSON cannot represent " + d);
            sb.append(new BigDecimal(v.toString()).toString());
        } else if (v instanceof BigDecimal) {
            sb.append(((BigDecimal) v).toString());
        } else if (v instanceof Long || v instanceof Integer || v instanceof Short
                || v instanceof Byte || v instanceof BigInteger) {
            sb.append(v.toString());
        } else if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            String inner = step == null ? "" : indent + step;
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first)
                    sb.append(',');
                first = false;
                newline(sb, step, inner);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(step == null ? ":" : ": ");
                write(sb, e.getValue(), step, inner, depth + 1);
            }
            newline(sb, step, indent);
            sb.append('}');
        } else if (v instanceof Collection || v instanceof Object[]) {
            Iterable<?> items = v instanceof Collection ? (Collection<?>) v : java.util.Arrays.asList((Object[]) v);
            if (!items.iterator().hasNext()) {
                sb.append("[]");
                return;
            }
            String inner = step == null ? "" : indent + step;
            sb.append('[');
            boolean first = true;
            for (Object o : items) {
                if (!first)
                    sb.append(',');
                first = false;
                newline(sb, step, inner);
                write(sb, o, step, inner, depth + 1);
            }
            newline(sb, step, indent);
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Cannot write " + v.getClass().getName() + " as JSON");
        }
    }

    private static void newline(StringBuilder sb, String step, String indent) {
        if (step != null)
            sb.append('\n').append(indent);
    }
}
