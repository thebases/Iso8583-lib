package com.base.iso.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.packager.ISO87BPackagerRoundTripTest;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.ProtectLogListener;

/** ISOMsg ↔ JSON / XML with the built-in readers and writers. */
class ISOMsgConverterTest {

    private static final String TPDU = "6000010002";
    private final ISO87BPackager p = new ISO87BPackager();

    private ISOMsg sample() throws Exception {
        ISOMsg m = TestMessages.m0200(p);
        m.setHeader(ISOUtil.hex2byte(TPDU));
        return m;
    }

    @Test
    void jsonShape() throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0800");
        m.set(11, "000001");
        m.set(52, ISOUtil.hex2byte("0A0B"));
        ISOMsg sub = new ISOMsg(48);
        sub.set(1, "A\"B");
        m.set(sub);
        m.recalcBitMap(); // bitmap must not appear in the output
        assertEquals("{\"fields\":{\"0\":\"0800\",\"11\":\"000001\",\"48\":{\"fields\":{\"1\":\"A\\\"B\"}},"
                + "\"52\":{\"binary\":\"0A0B\"}}}", ISOMsgConverter.toJson(m));
    }

    @Test
    void jsonRoundTripPacksIdentically() throws Exception {
        ISOMsg m = sample();
        String json = ISOMsgConverter.toJsonPretty(m);
        ISOMsg back = ISOMsgConverter.fromJson(json, p);
        assertEquals(TPDU, ISOUtil.hexString(back.getHeader()));
        assertArrayEquals(ISOUtil.hex2byte(TestMessages.PIN_BLOCK), back.getBytes(52));
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(back.pack()));
        assertEquals(json, ISOMsgConverter.toJsonPretty(back));
    }

    @Test
    void xmlShape() throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0800");
        m.set(52, ISOUtil.hex2byte("0A0B"));
        ISOMsg sub = new ISOMsg(48);
        sub.set(1, "<&>");
        m.set(sub);
        assertEquals("<isomsg>\n"
                + "  <field id=\"0\" value=\"0800\"/>\n"
                + "  <isomsg id=\"48\">\n"
                + "    <field id=\"1\" value=\"&lt;&amp;&gt;\"/>\n"
                + "  </isomsg>\n"
                + "  <field id=\"52\" value=\"0A0B\" type=\"binary\"/>\n"
                + "</isomsg>\n", ISOMsgConverter.toXml(m));
    }

    @Test
    void xmlRoundTripPacksIdentically() throws Exception {
        ISOMsg m = sample();
        String xml = ISOMsgConverter.toXml(m);
        assertTrue(xml.contains("<header>" + TPDU + "</header>"), xml);
        ISOMsg back = ISOMsgConverter.fromXml(xml, p);
        assertEquals(TPDU, ISOUtil.hexString(back.getHeader()));
        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(back.pack()));
        assertEquals(xml, ISOMsgConverter.toXml(back));
    }

    @Test
    void nestedRoundTrip() throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0100");
        ISOMsg sub = new ISOMsg(63);
        sub.set(1, "X");
        sub.set(2, ISOUtil.hex2byte("FF"));
        m.set(sub);
        for (ISOMsg back : new ISOMsg[] {
                ISOMsgConverter.fromJson(ISOMsgConverter.toJson(m), null),
                ISOMsgConverter.fromXml(ISOMsgConverter.toXml(m), null) }) {
            assertNull(back.getPackager());
            assertEquals("X", back.getString("63.1"));
            assertArrayEquals(new byte[] { (byte) 0xFF }, back.getBytes("63.2"));
        }
    }

    @Test
    void maskedViewForLogs() throws Exception {
        String json = ISOMsgConverter.toJson(new ProtectLogListener().mask(sample()));
        assertFalse(json.contains(TestMessages.PAN), json);
        assertFalse(json.contains(TestMessages.PIN_BLOCK), json);
        assertTrue(json.contains("411111______1111"), json);
    }

    @Test
    void rejectsBadInput() {
        for (String bad : new String[] { "[]", "{}", "{\"fields\":{\"x\":\"1\"}}", "{\"fields\":{\"2\":5}}",
                "{\"fields\":{\"52\":{\"binary\":\"ZZ\"}}}", "{\"header\":\"G0\",\"fields\":{}}", "{bad" })
            assertThrows(BaseException.class, () -> ISOMsgConverter.fromJson(bad, null), bad);
        for (String bad : new String[] { "<msg/>", "<isomsg><field value=\"1\"/></isomsg>",
                "<isomsg><field id=\"2\"/></isomsg>", "<isomsg><field id=\"52\" value=\"Z\" type=\"binary\"/></isomsg>",
                "<isomsg><isomsg id=\"48\"><header>00</header></isomsg></isomsg>", "<isomsg><x id=\"1\"/></isomsg>",
                "<isomsg>" })
            assertThrows(BaseException.class, () -> ISOMsgConverter.fromXml(bad, null), bad);
    }
}
