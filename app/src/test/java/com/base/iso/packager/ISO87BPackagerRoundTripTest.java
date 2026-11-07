package com.base.iso.packager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.util.ISOUtil;

/**
 * T1 — golden-master round trip through {@link ISO87BPackager} (and one ISO93B frame).
 * Hex fixtures were recorded from commit 402fc3e; they are the wire contract.
 */
public class ISO87BPackagerRoundTripTest {

    public static final String HEX_0200 =
            "02007020060020C0120016411111111111111100000000000001000000012300510001334111111111111111D2512101123456789"
          + "05445524D303030314D45524348414E54303030303030310123456789ABCDEF002239463236303841314232433344344535463630373138";
    public static final String HEX_0210 =
            "0210302000000E80000000000000000001000000012331323334353637383930313241314232433330305445524D30303031";
    public static final String HEX_0400 =
            "0400F020040000800000000000400000000016411111111111111100000000000001000000012400515445524D30303031"
          + "020000012311071200000000000000000000000000";
    public static final String HEX_0800 =
            "08008020000000800000040000000000000000000130303030303030310301";
    public static final String HEX_0810 =
            "081080200000028000000400000000000000000001303030303030303030310301";
    public static final String HEX_93_0800 =
            "08008020000000800000040000000000000000000130303030303030313010";

    private final ISO87BPackager p = new ISO87BPackager();

    @Test
    void pack0200() throws Exception {
        roundTrip(TestMessages.m0200(p), HEX_0200, p);
    }

    @Test
    void pack0210() throws Exception {
        roundTrip(TestMessages.m0210(p), HEX_0210, p);
    }

    @Test
    void pack0400() throws Exception {
        roundTrip(TestMessages.m0400(p), HEX_0400, p);
    }

    @Test
    void pack0800WithSecondaryBitmap() throws Exception {
        roundTrip(TestMessages.m0800(p), HEX_0800, p);
    }

    @Test
    void pack0810() throws Exception {
        roundTrip(TestMessages.m0810(p), HEX_0810, p);
    }

    @Test
    void iso93b0800() throws Exception {
        ISO93BPackager p93 = new ISO93BPackager();
        roundTrip(TestMessages.m0800(p93), HEX_93_0800, p93);
    }

    @Test
    void binaryFieldIsHexDecodedWhenPackagerSetFirst() throws Exception {
        ISOMsg m = TestMessages.m0200(p);
        assertArrayEquals(ISOUtil.hex2byte(TestMessages.PIN_BLOCK), m.getBytes(52));
    }

    /** pack → compare golden hex → unpack into a fresh msg → every field equal. */
    static void roundTrip(ISOMsg m, String expectedHex, iPackager pk) throws Exception {
        byte[] packed = m.pack();
        assertEquals(expectedHex, ISOUtil.hexString(packed));

        ISOMsg back = new ISOMsg();
        back.setPackager(pk);
        assertEquals(packed.length, back.unpack(packed));
        assertSameFields(m, back);
    }

    @SuppressWarnings("unchecked")
    static void assertSameFields(ISOMsg expected, ISOMsg actual) throws Exception {
        Map<Integer, BaseComponent> fields = expected.getChildren();
        for (Map.Entry<Integer, BaseComponent> e : fields.entrySet()) {
            int fld = e.getKey();
            if (fld < 0)
                continue; // bitmap compared through the packed image
            assertTrue(actual.hasField(fld), "missing field " + fld);
            Object v = e.getValue().getValue();
            if (v instanceof byte[])
                assertArrayEquals((byte[]) v, actual.getBytes(fld), "field " + fld);
            else
                assertEquals(v, actual.getString(fld), "field " + fld);
        }
        assertEquals(expected.getMTI(), actual.getMTI());
    }
}
