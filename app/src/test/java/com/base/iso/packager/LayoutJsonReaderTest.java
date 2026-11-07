package com.base.iso.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** T20 — unit tests for the internal JSON reader that replaced Jackson. */
class LayoutJsonReaderTest {

    @Test
    void parsesScalarsObjectsAndArrays() {
        Map<?, ?> m = (Map<?, ?>) LayoutJsonReader.parse(
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
    void decodesEscapes() {
        assertEquals("q\" b\\ s/ \b\f\n\r\t é€",
                LayoutJsonReader.parse("\"q\\\" b\\\\ s\\/ \\b\\f\\n\\r\\t \\u00e9\\u20AC\""));
    }

    @Test
    void emptyContainers() {
        assertEquals(Collections.emptyList(), LayoutJsonReader.parse("[]"));
        assertEquals(Collections.emptyMap(), LayoutJsonReader.parse("{ }"));
    }

    @Test
    void deepNestingWithinLimit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) sb.append('[');
        for (int i = 0; i < 60; i++) sb.append(']');
        LayoutJsonReader.parse(sb.toString());
    }

    @Test
    void rejectsTooDeepNesting() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) sb.append('[');
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse(sb.toString()));
    }

    @Test
    void rejectsMalformedInputWithOffset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> LayoutJsonReader.parse("{\"a\": 1} x"));
        assertTrue(e.getMessage().contains("offset 9"), e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("\"unterminated"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("{\"a\" 1}"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("[1,]"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("[1 2]"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("1.5"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("\"bad \\x escape\""));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("\"\\u12\""));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse("tru"));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse(""));
        assertThrows(IllegalArgumentException.class, () -> LayoutJsonReader.parse(null));
    }
}
