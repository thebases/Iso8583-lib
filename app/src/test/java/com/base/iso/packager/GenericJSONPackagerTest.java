package com.base.iso.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * T1/T20 — GenericJSONPackager built from an in-memory JSON string (no file reads) must produce
 * the same layout and the same bytes as {@link ISO87BPackager}. The layout equivalence is the
 * golden record taken while Jackson was still the parser; the internal reader must keep it.
 */
class GenericJSONPackagerTest {

    @Test
    void numberTypedLayoutMatchesIso87b() throws Exception {
        assertMatchesIso87b(TestMessages.iso87bLayoutJson(false));
    }

    @Test
    void stringTypedLayoutMatchesIso87b() throws Exception {
        // id/length/pad as JSON strings — the style of the old bundled iso87binary.json
        assertMatchesIso87b(TestMessages.iso87bLayoutJson(true));
    }

    @Test
    void defaultConstructorStartsWithIso87bLayout() throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(TestMessages.m0200(g).pack()));
    }

    // --- T10: invalid layouts fail loudly and leave the previous layout untouched ---

    @Test
    void badJsonThrowsAndKeepsPreviousLayout() throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromJson(TestMessages.iso87bLayoutJson(false));
        List<aISOFieldBase> before = g.getDynamicFields();

        assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson("{\"isofields\": [ {\"id\": 0, "));
        assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson("{\"other\": []}"));
        assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson("{\"isofields\": []}"));
        assertThrows(IllegalArgumentException.class,
                () -> g.setFieldsFromJson("{\"isofields\": [{\"id\": 0, \"class\": \"IFB_NUMERIC\"}]}"));       // no length
        assertThrows(IllegalArgumentException.class,
                () -> g.setFieldsFromJson("{\"isofields\": [{\"length\": 4, \"class\": \"IFB_NUMERIC\"}]}"));   // no id
        assertThrows(IllegalArgumentException.class,
                () -> g.setFieldsFromJson("{\"isofields\": [{\"id\": 0, \"length\": 4}]}"));                    // no class
        assertThrows(IllegalArgumentException.class,
                () -> g.setFieldsFromJson("{\"isofields\": [{\"id\": 129, \"length\": 4, \"class\": \"IF_CHAR\"}]}"));
        assertThrows(IllegalArgumentException.class,
                () -> g.setFieldsFromJson("{\"isofields\": [{\"id\": 2, \"length\": 4, \"class\": \"IF_CHAR\"},"
                        + "{\"id\": 2, \"length\": 4, \"class\": \"IF_CHAR\"}]}"));                             // duplicate

        assertEquals(before, g.getDynamicFields());
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(TestMessages.m0200(g).pack()));
    }

    @Test
    void unknownClassIsNamedInError() {
        GenericJSONPackager g = new GenericJSONPackager();
        List<aISOFieldBase> before = g.getDynamicFields();
        String json = TestMessages.iso87bLayoutJson(false).replaceFirst("\"IF_CHAR\"", "\"IF_STRIPPED_BY_R8\"");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson(json));
        assertTrue(e.getMessage().contains("IF_STRIPPED_BY_R8"), e.getMessage());
        assertEquals(before, g.getDynamicFields());
    }

    @Test
    void nonCodecClassIsRejected() {
        GenericJSONPackager g = new GenericJSONPackager();
        // a real class in com.base.iso.fields that is not an aISOFieldBase subclass
        String json = "{\"isofields\": [{\"id\": 0, \"length\": 4, \"class\": \"base.aISOFieldBase\"},"
                + "{\"id\": 1, \"length\": 4, \"class\": \"../../Object\"}]}";
        assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson(json));
    }

    // --- XML layouts (built-in parser) ---

    @Test
    void xmlLayoutMatchesIso87b() throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromXml(TestMessages.iso87bLayoutXml());
        assertLayoutMatchesIso87b(g);
    }

    @Test
    void invalidXmlLayoutThrowsAndKeepsPreviousLayout() throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromXml(TestMessages.iso87bLayoutXml());
        List<aISOFieldBase> before = g.getDynamicFields();

        for (String bad : new String[] {
                "<isopackager><isofield id=\"0\" length=\"4\" class=\"IFB_NUMERIC\">",           // malformed
                "<packager><isofield id=\"0\" length=\"4\" class=\"IFB_NUMERIC\"/></packager>",  // wrong root
                "<isopackager/>",                                                              // empty
                "<isopackager><isofield id=\"0\" class=\"IFB_NUMERIC\"/></isopackager>",         // no length
                "<isopackager><isofield id=\"x\" length=\"4\" class=\"IFB_NUMERIC\"/></isopackager>",
                "<isopackager><isofield id=\"0\" length=\"4\" class=\"IF_NOPE\"/></isopackager>",
                "<isopackager><isofieldpackager id=\"48\"/></isopackager>",
                "<isopackager><foo/></isopackager>",
                "<!DOCTYPE isopackager [<!ENTITY x \"y\">]><isopackager/>" })
            assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromXml(bad), bad);

        assertEquals(before, g.getDynamicFields());
    }

    @Test
    void qualifiedClassNamesAcceptedInJsonToo() throws Exception {
        String json = TestMessages.iso87bLayoutJson(false)
                .replace("\"class\": \"", "\"class\": \"com.base.iso.fields.");
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromJson(json);
        assertLayoutMatchesIso87b(g);
    }

    @Test
    void foreignPackageClassNamesAreRejected() {
        // Only short names and com.base.iso.fields.X are accepted; other packages are not aliased.
        String json = TestMessages.iso87bLayoutJson(false)
                .replaceFirst("\"class\": \"IF_CHAR\"", "\"class\": \"org.example.iso.IF_CHAR\"");
        GenericJSONPackager g = new GenericJSONPackager();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> g.setFieldsFromJson(json));
        assertTrue(e.getMessage().contains("org.example.iso.IF_CHAR"), e.getMessage());
    }

    private static void assertMatchesIso87b(String json) throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromJson(json);
        assertLayoutMatchesIso87b(g);
    }

    private static void assertLayoutMatchesIso87b(GenericJSONPackager g) throws Exception {
        // Golden layout: per slot class, length, description, pad.
        ISO87BPackager ref = new ISO87BPackager();
        List<aISOFieldBase> got = g.getDynamicFields();
        assertEquals(129, got.size());
        for (int i = 0; i <= 128; i++) {
            aISOFieldBase e = ref.getFieldPackager(i);
            aISOFieldBase a = got.get(i);
            assertNotNull(a, "slot " + i);
            assertEquals(e.getClass(), a.getClass(), "class slot " + i);
            assertEquals(e.getLength(), a.getLength(), "length slot " + i);
            assertEquals(e.getDescription(), a.getDescription(), "description slot " + i);
            assertEquals(TestMessages.iso87bPad(e), a.getPad(), "pad slot " + i);
        }

        // Byte-identical wire output.
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(TestMessages.m0200(g).pack()));
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0800, ISOUtil.hexString(TestMessages.m0800(g).pack()));
        ISO87BPackagerRoundTripTest.roundTrip(TestMessages.m0400(g), ISO87BPackagerRoundTripTest.HEX_0400, g);
    }
}
