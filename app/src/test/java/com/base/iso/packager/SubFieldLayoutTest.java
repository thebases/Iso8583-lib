package com.base.iso.packager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.core.ISOMsgConverter;
import com.base.iso.fields.IFB_BINARY;
import com.base.iso.fields.IFB_BITMAP;
import com.base.iso.fields.IFB_LLCHAR;
import com.base.iso.fields.IFB_LLLBINARY;
import com.base.iso.fields.IFB_LLLCHAR;
import com.base.iso.fields.IFB_NUMERIC;
import com.base.iso.fields.IF_CHAR;
import com.base.iso.fields.base.MsgFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * Sub-field layouts (XML {@code <isofieldpackager>} / JSON {@code "subfields"}):
 * F48 = positional sub-fields inside IFB_LLLCHAR, F63 = bitmapped sub-fields inside IFB_LLLBINARY.
 *
 * Expected bytes are derived by hand from the codec rules pinned in FieldCodecTest
 * (BCD MTI/length prefixes, IF_CHAR, IFB_LLCHAR, IFB_NUMERIC left pad, IFB_BITMAP, IFB_BINARY).
 */
class SubFieldLayoutTest {

    // ----------------------------------------------------------------- layouts

    static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE isopackager SYSTEM \"layout.dtd\">\n"
            + "<isopackager>\n"
            + "  <isofield id=\"0\" length=\"4\" name=\"MTI\" class=\"IFB_NUMERIC\" pad=\"true\"/>\n"
            + "  <isofield id=\"1\" length=\"16\" name=\"BIT MAP\" class=\"IFB_BITMAP\"/>\n"
            + "  <isofield id=\"11\" length=\"6\" name=\"STAN\" class=\"IFB_NUMERIC\" pad=\"true\"/>\n"
            + "  <isofieldpackager id=\"48\" length=\"999\" name=\"PRIVATE POSITIONAL\" class=\"IFB_LLLCHAR\"\n"
            + "        packager=\"GenericSubFieldPackager\" emitBitmap=\"false\">\n"
            + "    <isofield id=\"1\" length=\"2\" name=\"TABLE\" class=\"IF_CHAR\"/>\n"
            + "    <isofield id=\"2\" length=\"20\" name=\"DATA\" class=\"IFB_LLCHAR\"/>\n"
            + "    <isofield id=\"3\" length=\"4\" name=\"NUM\" class=\"IFB_NUMERIC\" pad=\"true\"/>\n"
            + "  </isofieldpackager>\n"
            + "  <isofieldpackager id=\"63\" length=\"999\" name=\"PRIVATE BITMAPPED\" class=\"IFB_LLLBINARY\"\n"
            + "        packager=\"GenericSubFieldPackager\" emitBitmap=\"true\" bitmapField=\"0\">\n"
            + "    <isofield id=\"0\" length=\"8\" name=\"SUB BITMAP\" class=\"IFB_BITMAP\"/>\n"
            + "    <isofield id=\"1\" length=\"2\" name=\"TABLE\" class=\"IF_CHAR\"/>\n"
            + "    <isofield id=\"2\" length=\"20\" name=\"DATA\" class=\"IFB_LLCHAR\"/>\n"
            + "    <isofield id=\"3\" length=\"4\" name=\"BIN\" class=\"IFB_BINARY\"/>\n"
            + "  </isofieldpackager>\n"
            + "</isopackager>\n";

    static final String JSON = "{\"isofields\": ["
            + "{\"id\":0,\"length\":4,\"name\":\"MTI\",\"class\":\"IFB_NUMERIC\",\"pad\":true},"
            + "{\"id\":1,\"length\":16,\"name\":\"BIT MAP\",\"class\":\"IFB_BITMAP\"},"
            + "{\"id\":11,\"length\":6,\"name\":\"STAN\",\"class\":\"IFB_NUMERIC\",\"pad\":true},"
            + "{\"id\":48,\"length\":999,\"name\":\"PRIVATE POSITIONAL\",\"class\":\"IFB_LLLCHAR\",\"emitBitmap\":false,"
            + " \"subfields\":["
            + "  {\"id\":1,\"length\":2,\"name\":\"TABLE\",\"class\":\"IF_CHAR\"},"
            + "  {\"id\":2,\"length\":20,\"name\":\"DATA\",\"class\":\"IFB_LLCHAR\"},"
            + "  {\"id\":3,\"length\":4,\"name\":\"NUM\",\"class\":\"IFB_NUMERIC\",\"pad\":true}]},"
            + "{\"id\":63,\"length\":999,\"name\":\"PRIVATE BITMAPPED\",\"class\":\"IFB_LLLBINARY\",\"emitBitmap\":true,\"bitmapField\":0,"
            + " \"subfields\":["
            + "  {\"id\":0,\"length\":8,\"name\":\"SUB BITMAP\",\"class\":\"IFB_BITMAP\"},"
            + "  {\"id\":1,\"length\":2,\"name\":\"TABLE\",\"class\":\"IF_CHAR\"},"
            + "  {\"id\":2,\"length\":20,\"name\":\"DATA\",\"class\":\"IFB_LLCHAR\"},"
            + "  {\"id\":3,\"length\":4,\"name\":\"BIN\",\"class\":\"IFB_BINARY\"}]}"
            + "]}";

    /** The same layout built in code (no JSON/XML), as a consumer could do it. */
    static GenericJSONPackager inCode() {
        IFB_NUMERIC num3 = new IFB_NUMERIC();
        num3.setLength(4);
        num3.setDescription("NUM");
        num3.setPad(true);
        aISOFieldBase[] f48 = { null, new IF_CHAR(2, "TABLE"), new IFB_LLCHAR(20, "DATA"), num3 };
        aISOFieldBase[] f63 = { new IFB_BITMAP(8, "SUB BITMAP"), new IF_CHAR(2, "TABLE"),
                new IFB_LLCHAR(20, "DATA"), new IFB_BINARY(4, "BIN") };
        aISOFieldBase[] main = new aISOFieldBase[64];
        main[0] = new IFB_NUMERIC(4, "MTI", true);
        main[1] = new IFB_BITMAP(16, "BIT MAP");
        main[11] = new IFB_NUMERIC(6, "STAN", true);
        main[48] = new MsgFieldBase(new IFB_LLLCHAR(999, "PRIVATE POSITIONAL"),
                new GenericSubFieldPackager(48, f48, false, 1, -1));
        main[63] = new MsgFieldBase(new IFB_LLLBINARY(999, "PRIVATE BITMAPPED"),
                new GenericSubFieldPackager(63, f63, true, 0, -1));
        GenericJSONPackager g = new GenericJSONPackager();
        g.setDynamicFields(main);
        return g;
    }

    static GenericJSONPackager fromXml() {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromXml(XML);
        return g;
    }

    static GenericJSONPackager fromJson() {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromJson(JSON);
        return g;
    }

    // ----------------------------------------------------------------- fixtures

    static final String HDR = "0100" + "0020000000010002" + "000001";
    static final String F48_ALL = "0010" + "4142" + "0548454C4C4F" + "0012";
    static final String F63_1_3 = "0014" + "A000000000000000" + "4142" + "0A0B0C0D";
    static final String FULL = HDR + F48_ALL + F63_1_3;

    /** 0100 with F11, F48.1-3 and F63.1 + F63.3 — set via field paths, packager first. */
    static ISOMsg full(GenericJSONPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set(11, "000001");
        m.set("48.1", "AB");
        m.set("48.2", "HELLO");
        m.set("48.3", "12");
        m.set("63.1", "AB");
        m.set("63.3", "0A0B0C0D"); // binary sub-field: hex parsed because 63's sub-packager is attached
        return m;
    }

    // ----------------------------------------------------------------- tests

    @Test
    void packsGoldenFromXmlJsonAndCode() throws Exception {
        for (GenericJSONPackager p : new GenericJSONPackager[] { fromXml(), fromJson(), inCode() })
            assertEquals(FULL, ISOUtil.hexString(full(p).pack()));
    }

    @Test
    void unpacksIntoNestedMessages() throws Exception {
        for (GenericJSONPackager p : new GenericJSONPackager[] { fromXml(), fromJson(), inCode() }) {
            ISOMsg m = new ISOMsg();
            m.setPackager(p);
            assertEquals(FULL.length() / 2, m.unpack(ISOUtil.hex2byte(FULL)));
            assertEquals("0100", m.getMTI());
            assertEquals("AB", m.getString("48.1"));
            assertEquals("HELLO", m.getString("48.2"));
            assertEquals("0012", m.getString("48.3"));
            assertEquals("AB", m.getString("63.1"));
            assertFalse(m.hasField("63.2"));
            assertArrayEquals(ISOUtil.hex2byte("0A0B0C0D"), m.getBytes("63.3"));
            assertTrue(m.getValue(48) instanceof ISOMsg);
        }
    }

    @Test
    void positionalGapIsPackedEmptyToKeepAlignment() throws Exception {
        GenericJSONPackager p = fromXml();
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set("48.1", "AB");
        m.set("48.3", "12");
        // 48.2 (IFB_LLCHAR) missing → packed as empty "00" so 48.3 stays in position
        String hex = ISOUtil.hexString(m.pack());
        assertEquals("0100" + "0000000000010000" + "0005" + "4142" + "00" + "0012", hex);
        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(ISOUtil.hex2byte(hex));
        assertEquals("", back.getString("48.2"));
        assertEquals("0012", back.getString("48.3"));
    }

    @Test
    void positionalTrailingSubFieldsAreOptional() throws Exception {
        GenericJSONPackager p = fromXml();
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set("48.1", "AB");
        String hex = ISOUtil.hexString(m.pack());
        assertEquals("0100" + "0000000000010000" + "0002" + "4142", hex);

        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(ISOUtil.hex2byte(hex)); // stops cleanly after 48.1
        assertEquals("AB", back.getString("48.1"));
        assertFalse(back.hasField("48.2"));
        assertFalse(back.hasField("48.3"));
    }

    @Test
    void positionalUnknownTrailingBytesAreIgnored() throws Exception {
        // inner = all three sub-fields + 2 extra bytes the layout does not know
        String frame = "0100" + "0000000000010000" + "0012" + "4142" + "0548454C4C4F" + "0012" + "FFFF";
        ISOMsg m = new ISOMsg();
        m.setPackager(fromXml());
        m.unpack(ISOUtil.hex2byte(frame));
        assertEquals("0012", m.getString("48.3"));
    }

    @Test
    void bitmappedOnlyTheSetBits() throws Exception {
        GenericJSONPackager p = fromJson();
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set("63.2", "XY");
        String hex = ISOUtil.hexString(m.pack());
        assertEquals("0100" + "0000000000000002" + "0011" + "4000000000000000" + "025859", hex);
        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(ISOUtil.hex2byte(hex));
        assertEquals("XY", back.getString("63.2"));
        assertFalse(back.hasField("63.1"));
    }

    @Test
    void bitmappedUnknownBitFails() {
        // sub-bitmap bit 5 set, but the layout has no sub-field 5 → its length is unknowable
        String frame = "0100" + "0000000000000002" + "0010" + "0800000000000000" + "0102";
        ISOMsg m = new ISOMsg();
        m.setPackager(fromXml());
        BaseException e = assertThrows(BaseException.class, () -> m.unpack(ISOUtil.hex2byte(frame)));
        assertTrue(String.valueOf(e.getMessage()).contains("field=63"), e.getMessage());
    }

    @Test
    void truncatedPositionalSubFieldNamesThePath() {
        // 48.2 says 5 chars but only 2 follow
        String frame = "0100" + "0000000000010000" + "0005" + "4142" + "054849";
        ISOMsg m = new ISOMsg();
        m.setPackager(fromXml());
        BaseException e = assertThrows(BaseException.class, () -> m.unpack(ISOUtil.hex2byte(frame)));
        assertTrue(String.valueOf(e.getMessage()).contains("field=48"), e.getMessage());
    }

    @Test
    void pathSetAttachesSubPackagerSoBinaryIsHex() throws Exception {
        ISOMsg m = full(fromXml());
        ISOMsg f63 = (ISOMsg) m.getValue(63);
        assertTrue(f63.getPackager() instanceof GenericSubFieldPackager);
        assertArrayEquals(ISOUtil.hex2byte("0A0B0C0D"), m.getBytes("63.3"));
        assertThrows(BaseException.class, () -> m.set("63.3", "ZZ")); // strict hex reaches sub-fields too
    }

    @Test
    void nestedSubLayouts() throws Exception {
        String xml = "<isopackager>"
                + "<isofield id=\"0\" length=\"4\" name=\"MTI\" class=\"IFB_NUMERIC\" pad=\"true\"/>"
                + "<isofield id=\"1\" length=\"16\" name=\"BITMAP\" class=\"IFB_BITMAP\"/>"
                + "<isofieldpackager id=\"48\" length=\"999\" name=\"OUTER\" class=\"IFB_LLLCHAR\" emitBitmap=\"false\">"
                + "  <isofield id=\"1\" length=\"2\" name=\"A\" class=\"IF_CHAR\"/>"
                + "  <isofieldpackager id=\"2\" length=\"99\" name=\"INNER\" class=\"IFB_LLCHAR\" emitBitmap=\"false\">"
                + "    <isofield id=\"1\" length=\"3\" name=\"X\" class=\"IF_CHAR\"/>"
                + "    <isofield id=\"2\" length=\"9\" name=\"Y\" class=\"IFB_LLCHAR\"/>"
                + "  </isofieldpackager>"
                + "</isofieldpackager>"
                + "</isopackager>";
        GenericJSONPackager p = new GenericJSONPackager();
        p.setFieldsFromXml(xml);
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set("48.1", "AB");
        m.set("48.2.1", "XYZ");
        m.set("48.2.2", "Q");
        // inner(48.2) = 58595A 01 51 (5 bytes) → IFB_LLCHAR "05"+…; outer(48) = 4142 + 05 58595A0151 (8 bytes)
        String hex = ISOUtil.hexString(m.pack());
        assertEquals("0100" + "0000000000010000" + "0008" + "4142" + "05" + "58595A" + "0151", hex);
        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(ISOUtil.hex2byte(hex));
        assertEquals("XYZ", back.getString("48.2.1"));
        assertEquals("Q", back.getString("48.2.2"));
    }

    @Test
    void defaultBitmapFieldIsOneAndEmitBitmapInferred() throws Exception {
        // defaults: bitmapField=1; emitBitmap omitted → bitmapped because id 1 is a bitmap codec
        String json = "{\"isofields\":["
                + "{\"id\":0,\"length\":4,\"class\":\"IFB_NUMERIC\",\"pad\":true},"
                + "{\"id\":1,\"length\":16,\"class\":\"IFB_BITMAP\"},"
                + "{\"id\":63,\"length\":999,\"class\":\"IFB_LLLBINARY\",\"subfields\":["
                + "  {\"id\":1,\"length\":8,\"class\":\"IFB_BITMAP\"},"
                + "  {\"id\":2,\"length\":2,\"class\":\"IF_CHAR\"},"
                + "  {\"id\":3,\"length\":2,\"class\":\"IF_CHAR\"}]}]}";
        GenericJSONPackager p = new GenericJSONPackager();
        p.setFieldsFromJson(json);
        GenericSubFieldPackager sub = (GenericSubFieldPackager) ((MsgFieldBase) p.getFieldPackager(63)).getISOMsgPackager();
        assertTrue(sub.isBitmapped());
        assertEquals(1, sub.getBitmapField());
        assertEquals(2, sub.getFirstSubField());

        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0100");
        m.set("63.3", "CD");
        // sub-bitmap bit 3 → 0x20; then only 63.3
        assertEquals("0100" + "0000000000000002" + "0010" + "2000000000000000" + "4344",
                ISOUtil.hexString(m.pack()));
    }

    @Test
    void converterRoundTripPacksIdentically() throws Exception {
        GenericJSONPackager p = fromXml();
        ISOMsg m = full(p);
        String json = ISOMsgConverter.toJson(m);
        assertTrue(json.contains("\"48\":{\"fields\":{\"1\":\"AB\""), json);
        assertEquals(FULL, ISOUtil.hexString(ISOMsgConverter.fromJson(json, p).pack()));
        assertEquals(FULL, ISOUtil.hexString(ISOMsgConverter.fromXml(ISOMsgConverter.toXml(m), p).pack()));

        // unpacked message → JSON omits the sub-bitmap
        ISOMsg u = new ISOMsg();
        u.setPackager(p);
        u.unpack(ISOUtil.hex2byte(FULL));
        assertFalse(ISOMsgConverter.toJson(u).contains("-1"));
    }

    @Test
    void sharedPackagerWithSubFieldsIsThreadSafe() throws Exception {
        GenericJSONPackager p = fromXml();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        try {
            List<java.util.concurrent.Future<?>> fs = new java.util.ArrayList<>();
            for (int t = 0; t < 6; t++)
                fs.add(pool.submit(() -> {
                    for (int i = 0; i < 300; i++) {
                        assertEquals(FULL, ISOUtil.hexString(full(p).pack()));
                        ISOMsg u = new ISOMsg();
                        u.setPackager(p);
                        u.unpack(ISOUtil.hex2byte(FULL));
                        assertEquals("HELLO", u.getString("48.2"));
                    }
                    return null;
                }));
            for (java.util.concurrent.Future<?> f : fs)
                f.get(60, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    // ----------------------------------------------------------- layout errors

    @Test
    void invalidSubLayoutsFailLoudlyWithPath() {
        String head = "<isopackager><isofield id=\"0\" length=\"4\" class=\"IFB_NUMERIC\" pad=\"true\"/>"
                + "<isofield id=\"1\" length=\"16\" class=\"IFB_BITMAP\"/>";
        String[][] cases = {
            { "not supported", "<isofieldpackager id=\"48\" length=\"999\" class=\"IFB_LLLCHAR\""
                    + " packager=\"TaggedFieldsPackager\"><isofield id=\"1\" length=\"2\" class=\"IF_CHAR\"/></isofieldpackager>" },
            { "tagMapper", "<isofieldpackager id=\"48\" length=\"999\" class=\"IFB_LLLCHAR\" tagMapper=\"x\">"
                    + "<isofield id=\"1\" length=\"2\" class=\"IF_CHAR\"/></isofieldpackager>" },
            { "bitmap", "<isofieldpackager id=\"63\" length=\"999\" class=\"IFB_LLLBINARY\" emitBitmap=\"true\" bitmapField=\"0\">"
                    + "<isofield id=\"1\" length=\"2\" class=\"IF_CHAR\"/></isofieldpackager>" },
            { "secondary bitmap", "<isofieldpackager id=\"63\" length=\"999\" class=\"IFB_LLLBINARY\" emitBitmap=\"true\" bitmapField=\"0\">"
                    + "<isofield id=\"0\" length=\"16\" class=\"IFB_BITMAP\"/><isofield id=\"1\" length=\"2\" class=\"IF_CHAR\"/></isofieldpackager>" },
            { "maxValidField", "<isofieldpackager id=\"48\" length=\"999\" class=\"IFB_LLLCHAR\" emitBitmap=\"false\" maxValidField=\"2\">"
                    + "<isofield id=\"3\" length=\"2\" class=\"IF_CHAR\"/></isofieldpackager>" },
            { "has no", "<isofieldpackager id=\"48\" length=\"999\" class=\"IFB_LLLCHAR\"/>" },
            { "IF_NOPE", "<isofieldpackager id=\"48\" length=\"999\" class=\"IFB_LLLCHAR\" emitBitmap=\"false\">"
                    + "<isofield id=\"1\" length=\"2\" class=\"IF_CHAR\"/><isofield id=\"2\" length=\"2\" class=\"IF_NOPE\"/></isofieldpackager>" },
        };
        for (String[] c : cases) {
            GenericJSONPackager g = new GenericJSONPackager();
            List<aISOFieldBase> before = g.getDynamicFields();
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> g.setFieldsFromXml(head + c[1] + "</isopackager>"), c[0]);
            assertTrue(e.getMessage().contains(c[0]), c[0] + " → " + e.getMessage());
            assertTrue(e.getMessage().contains("isofield[2]"), "path in: " + e.getMessage());
            assertEquals(before, g.getDynamicFields());
        }
        // the path names the nested entry
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new GenericJSONPackager()
                .setFieldsFromXml(head + cases[6][1] + "</isopackager>"));
        assertTrue(e.getMessage().startsWith("isofield[2].isofield[1]: Unknown field class 'IF_NOPE'"), e.getMessage());
    }

    @Test
    void jsonSubfieldsMustBeArray() {
        String json = "{\"isofields\":[{\"id\":0,\"length\":4,\"class\":\"IFB_NUMERIC\"},"
                + "{\"id\":48,\"length\":9,\"class\":\"IFB_LLLCHAR\",\"subfields\":{}}]}";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new GenericJSONPackager().setFieldsFromJson(json));
        assertTrue(e.getMessage().contains("isofields[1]"), e.getMessage());
    }
}
