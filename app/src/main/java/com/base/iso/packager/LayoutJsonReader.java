package com.base.iso.packager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader for the GenericJSONPackager layout schema (plan T20).
 *
 * Replaces Jackson so the jar has zero third-party runtime dependencies. JDK-only and safe on
 * Android API 24. Recursive descent over the subset the layout needs:
 * objects, arrays, strings (all JSON escapes incl. \\uXXXX), integers, true/false/null.
 * Floats, comments and trailing commas are rejected.
 *
 * Output types: {@code Map<String,Object>} (insertion-ordered), {@code List<Object>},
 * {@code String}, {@code Long}, {@code Boolean}, or {@code null}.
 * Errors are {@link IllegalArgumentException} with the character offset.
 *
 * Stateless entry point; each call uses its own instance, so it is thread-safe.
 */
final class LayoutJsonReader {

    /** Guards against stack overflow on hostile input; the layout schema needs depth 3. */
    private static final int MAX_DEPTH = 64;

    private final String s;
    private int pos;
    private int depth;

    private LayoutJsonReader(String s) {
        this.s = s;
    }

    /** Parses a complete JSON document; anything but whitespace after the value is an error. */
    static Object parse(String json) {
        if (json == null)
            throw new IllegalArgumentException("JSON layout is null");
        LayoutJsonReader r = new LayoutJsonReader(json);
        r.skipWs();
        Object v = r.readValue();
        r.skipWs();
        if (r.pos != json.length())
            throw r.error("unexpected trailing content");
        return v;
    }

    private Object readValue() {
        if (pos >= s.length())
            throw error("unexpected end of input");
        char c = s.charAt(pos);
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': return literal("true", Boolean.TRUE);
            case 'f': return literal("false", Boolean.FALSE);
            case 'n': return literal("null", null);
            default:
                if (c == '-' || (c >= '0' && c <= '9'))
                    return readInteger();
                throw error("unexpected character '" + c + "'");
        }
    }

    private Map<String, Object> readObject() {
        enter();
        pos++; // '{'
        Map<String, Object> m = new LinkedHashMap<>();
        skipWs();
        if (peek() == '}') {
            pos++;
            depth--;
            return m;
        }
        for (;;) {
            skipWs();
            if (peek() != '"')
                throw error("expected object key");
            String key = readString();
            skipWs();
            expect(':');
            skipWs();
            m.put(key, readValue());
            skipWs();
            char c = next();
            if (c == '}')
                break;
            if (c != ',')
                throw error("expected ',' or '}'", pos - 1);
        }
        depth--;
        return m;
    }

    private List<Object> readArray() {
        enter();
        pos++; // '['
        List<Object> l = new ArrayList<>();
        skipWs();
        if (peek() == ']') {
            pos++;
            depth--;
            return l;
        }
        for (;;) {
            skipWs();
            l.add(readValue());
            skipWs();
            char c = next();
            if (c == ']')
                break;
            if (c != ',')
                throw error("expected ',' or ']'", pos - 1);
        }
        depth--;
        return l;
    }

    private String readString() {
        int start = pos;
        pos++; // opening quote
        StringBuilder sb = new StringBuilder();
        for (;;) {
            if (pos >= s.length())
                throw error("unterminated string", start);
            char c = s.charAt(pos++);
            if (c == '"')
                return sb.toString();
            if (c < 0x20)
                throw error("control character in string", pos - 1);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= s.length())
                throw error("unterminated string", start);
            char e = s.charAt(pos++);
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (pos + 4 > s.length())
                        throw error("truncated \\u escape", pos - 2);
                    int cp = 0;
                    for (int i = 0; i < 4; i++) {
                        int d = Character.digit(s.charAt(pos + i), 16);
                        if (d < 0)
                            throw error("invalid \\u escape", pos - 2);
                        cp = (cp << 4) | d;
                    }
                    pos += 4;
                    sb.append((char) cp);
                    break;
                default:
                    throw error("invalid escape '\\" + e + "'", pos - 2);
            }
        }
    }

    /** Integers only (the schema has no fractional values); range-checked to long. */
    private Long readInteger() {
        int start = pos;
        if (peek() == '-')
            pos++;
        int digits = pos;
        while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9')
            pos++;
        if (pos == digits)
            throw error("invalid number", start);
        if (pos < s.length() && (s.charAt(pos) == '.' || s.charAt(pos) == 'e' || s.charAt(pos) == 'E'))
            throw error("fractional/exponent numbers are not supported", start);
        try {
            return Long.valueOf(s.substring(start, pos));
        } catch (NumberFormatException ex) {
            throw error("number out of range", start);
        }
    }

    private Object literal(String word, Object value) {
        if (!s.startsWith(word, pos))
            throw error("unexpected token");
        pos += word.length();
        return value;
    }

    private void enter() {
        if (++depth > MAX_DEPTH)
            throw error("nesting deeper than " + MAX_DEPTH);
    }

    private void skipWs() {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
                break;
            pos++;
        }
    }

    private char peek() {
        if (pos >= s.length())
            throw error("unexpected end of input");
        return s.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char c) {
        if (next() != c)
            throw error("expected '" + c + "'", pos - 1);
    }

    private IllegalArgumentException error(String msg) {
        return error(msg, pos);
    }

    private IllegalArgumentException error(String msg, int at) {
        return new IllegalArgumentException("Invalid JSON layout: " + msg + " at offset " + at);
    }
}
