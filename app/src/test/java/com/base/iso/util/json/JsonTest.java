package com.base.iso.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Built-in JSON reader/writer (replaced Jackson). */
class JsonTest {

    // --- reader ---

    @Test
    void parsesScalarsObjectsAndArrays() {
        Map<?, ?> m = (Map<?, ?>) Json.parse(
                " {\"a\": 12, \"b\": -3, \"c\": true, \"d\": false, \"e\": null, \"f\": [1, \"x\", {\"g\": []}]} ");
        assertEquals(12L, m.get("a"));
        assertEquals(-3L, m.get("b"));
        assertEquals(Boolean.TRUE, m.get("c"));
        assertEquals(Boolean.FALSE, m.get("d"));
        assertTrue(m.containsKey("e"));
        assertNull(m.get("e"));
        List<?> f = (List<?>) m.get("f");
        assertEquals(Arrays.asList(1L, "x"), f.subList(0, 2));
        assertEquals(Collections.emptyList(), ((Map<?, ?>) f.get(2)).get("g"));
    }

    @Test
    void keepsKeyOrder() {
        Map<?, ?> m = (Map<?, ?>) Json.parse("{\"z\":1,\"a\":2,\"m\":3}");
        assertEquals(Arrays.asList("z", "a", "m"), Arrays.asList(m.keySet().toArray()));
    }

    @Test
    void parsesDecimalsAndBigNumbers() {
        assertEquals(new BigDecimal("12.50"), Json.parse("12.50"));
        assertEquals(new BigDecimal("-1.5E+3"), Json.parse("-1.5e3"));
        assertEquals(new BigDecimal("123456789012345678901234567890"), Json.parse("123456789012345678901234567890"));
        assertEquals(0L, Json.parse("0"));
    }

    @Test
    void decodesEscapes() {
        assertEquals("q\" b\\ s/ \b\f\n\r\t é€",
                Json.parse("\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u00e9\\u20AC\""));
    }

    @Test
    void emptyContainers() {
        assertEquals(Collections.emptyList(), Json.parse("[]"));
        assertEquals(Collections.emptyMap(), Json.parse("{ }"));
    }

    @Test
    void nestingLimit() {
        StringBuilder ok = new StringBuilder();
        for (int i = 0; i < 60; i++) ok.append('[');
        for (int i = 0; i < 60; i++) ok.append(']');
        Json.parse(ok.toString());

        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 10000; i++) deep.append('[');
        assertThrows(IllegalArgumentException.class, () -> Json.parse(deep.toString()));
    }

    @Test
    void rejectsMalformedInputWithOffset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Json.parse("{\"a\": 1} x"));
        assertTrue(e.getMessage().contains("offset 9"), e.getMessage());

        for (String bad : new String[] { "\"unterminated", "{\"a\" 1}", "[1,]", "[1 2]", "01", "1.", "1e", "-",
                "\"bad \\x escape\"", "\"\\u12\"", "tru", "", "{\"a\":1,}", "\"tab\there\"" })
            assertThrows(IllegalArgumentException.class, () -> Json.parse(bad), bad);
        assertThrows(IllegalArgumentException.class, () -> Json.parse(null));
    }

    // --- writer ---

    @Test
    void writesCompactAndRoundTrips() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("s", "quote\" back\\ nl\n tab\t ctl\u0001 é");
        m.put("n", 42L);
        m.put("i", 7);
        m.put("d", new BigDecimal("12.50"));
        m.put("b", true);
        m.put("z", null);
        m.put("l", Arrays.asList(1L, "x", Collections.emptyMap()));
        m.put("a", new Object[] { "y" });
        String json = Json.write(m);
        assertEquals("{\"s\":\"quote\\\" back\\\\ nl\\n tab\\t ctl\\u0001 é\",\"n\":42,\"i\":7,\"d\":12.50,"
                + "\"b\":true,\"z\":null,\"l\":[1,\"x\",{}],\"a\":[\"y\"]}", json);

        Map<?, ?> back = (Map<?, ?>) Json.parse(json);
        assertEquals(m.get("s"), back.get("s"));
        assertEquals(7L, back.get("i"));
        assertEquals(new BigDecimal("12.50"), back.get("d"));
    }

    @Test
    void writesPretty() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1L);
        m.put("b", Arrays.asList(2L, 3L));
        assertEquals("{\n  \"a\": 1,\n  \"b\": [\n    2,\n    3\n  ]\n}", Json.writePretty(m));
        assertEquals(m, Json.parse(Json.writePretty(m)));
    }

    @Test
    void rejectsUnwritableValues() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
    }
}
