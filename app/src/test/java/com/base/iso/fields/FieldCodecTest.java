package com.base.iso.fields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BitMapComponent;
import com.base.iso.core.ISOField;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * T19 — per-codec wire-format tests: exact packed hex, pack→unpack round-trip, over-length behaviour.
 * Cases live in {@link FieldCodecCases}; lines marked FINDING document current (questionable)
 * behaviour that is pinned, not fixed.
 */
class FieldCodecTest {

    static List<FieldCodecCases.Case> cases() {
        return FieldCodecCases.all();
    }

    private static BaseComponent component(Object v) {
        return v instanceof byte[] ? new BinaryFieldComponent(1, (byte[]) v) : new ISOField(1, (String) v);
    }

    private static String text(Object v) {
        return v instanceof byte[] ? ISOUtil.hexString((byte[]) v) : (String) v;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void packsToGoldenHex(FieldCodecCases.Case k) throws Exception {
        assertEquals(k.packedHex, ISOUtil.hexString(k.codec.get().pack(component(k.value))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void unpackRoundTrips(FieldCodecCases.Case k) throws Exception {
        aISOFieldBase f = k.codec.get();
        byte[] packed = ISOUtil.hex2byte(k.packedHex);
        BaseComponent c = f.createComponent(1);
        // Unpack from the middle of a buffer to prove the codec honours the offset.
        byte[] framed = new byte[packed.length + 3];
        System.arraycopy(packed, 0, framed, 2, packed.length);
        assertEquals(packed.length, f.unpack(c, framed, 2));
        assertEquals(text(k.back), text(c.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void overLength(FieldCodecCases.Case k) throws Exception {
        aISOFieldBase f = k.codec.get();
        Object big = k.value instanceof byte[]
                ? new byte[f.getLength() + 1]
                : ISOUtil.zeropad("1", f.getLength() + 1);
        if (k.rejectsOverLength)
            assertThrows(BaseException.class, () -> f.pack(component(big)));
        else
            f.pack(component(big)); // pinned current behaviour (see FINDING in FieldCodecCases)
    }

    @Test
    void ifbBitmapPrimaryOnly() throws Exception {
        BitSet bs = new BitSet();
        bs.set(3);
        bs.set(11);
        bs.set(41);
        IFB_BITMAP f = new IFB_BITMAP(16, "BIT MAP");
        byte[] p = f.pack(new BitMapComponent(-1, bs));
        assertEquals("2020000000800000", ISOUtil.hexString(p));
        BitMapComponent back = new BitMapComponent(-1);
        assertEquals(8, f.unpack(back, p, 0));
        assertEquals(bs, back.getValue());
    }

    @Test
    void ifbBitmapWithSecondary() throws Exception {
        BitSet bs = new BitSet();
        bs.set(11);
        bs.set(70);
        IFB_BITMAP f = new IFB_BITMAP(16, "BIT MAP");
        byte[] p = f.pack(new BitMapComponent(-1, bs));
        // bit 1 (secondary bitmap present) is set by the codec
        assertEquals("80200000000000000400000000000000", ISOUtil.hexString(p));
        BitMapComponent back = new BitMapComponent(-1);
        assertEquals(16, f.unpack(back, p, 0));
        BitSet got = (BitSet) back.getValue();
        assertEquals(true, got.get(11));
        assertEquals(true, got.get(70));
    }
}
