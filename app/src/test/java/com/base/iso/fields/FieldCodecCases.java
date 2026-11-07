package com.base.iso.fields;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.fields.base.aTaggedFieldBase;
import com.base.iso.interfaces.iTagMapper;

/**
 * Table of field-codec cases for {@link FieldCodecTest} (plan T19).
 *
 * Each case = codec factory + sample value + golden packed hex. Golden hex was recorded from the
 * pre-change code (commit 402fc3e); a mismatch means the wire format changed.
 * Families: BCD numeric (IFB_NUMERIC/LLNUM/LLLNUM), binary-prefixed char/binary (IFB_LL*, IFB_LLL*),
 * IFB_AMOUNT, IF_CHAR, ASCII (IFA_*), EBCDIC (IFAE_LLCHAR) and tagged TLV (IFA_TT*).
 */
final class FieldCodecCases {

    private FieldCodecCases() {}

    /** One codec under test. {@code value} is a String or a byte[] (binary codecs). */
    static final class Case {
        final String name;
        final Supplier<aISOFieldBase> codec;
        final Object value;
        final String packedHex;
        /** true when the codec is expected to reject a value one char/byte longer than its max. */
        final boolean rejectsOverLength;
        /** Value expected after unpack when the codec normalizes (zero/space padding); defaults to {@code value}. */
        Object back;

        Case(String name, Supplier<aISOFieldBase> codec, Object value, String packedHex, boolean rejectsOverLength) {
            this.name = name;
            this.codec = codec;
            this.value = value;
            this.packedHex = packedHex;
            this.rejectsOverLength = rejectsOverLength;
            this.back = value;
        }

        Case back(Object v) {
            this.back = v;
            return this;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Sub-field number used by the tagged codecs (parent field 48, sub-field 1 ↔ tag "01"/"001"). */
    static final int TAG_SUBFIELD = 1;

    /** Tagged codec wired to a one-entry tag mapper: sub-field 1 ↔ {@code tag}. */
    private static aISOFieldBase tagged(aTaggedFieldBase f, int len, final String tag) {
        f.setLength(len);
        f.setDescription("tagged");
        f.setParentFieldNumber(48);
        f.setTagMapper(new iTagMapper() {
            public String getTagForField(int fieldNumber, int subFieldNumber) {
                return subFieldNumber == TAG_SUBFIELD ? tag : null;
            }

            public Integer getFieldNumberForTag(int fieldNumber, String t) {
                return tag.equals(t) ? TAG_SUBFIELD : null;
            }
        });
        return f;
    }

    private static aISOFieldBase noArg(aISOFieldBase f, int len) {
        f.setLength(len);
        f.setDescription("test");
        return f;
    }

    private static byte[] bytes(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++)
            b[i] = (byte) (0xA0 + i);
        return b;
    }

    static List<Case> all() {
        List<Case> c = new ArrayList<>();
        // --- BCD numeric (used by ISO87B/ISO93B) ---
        c.add(new Case("IFB_NUMERIC even", () -> new IFB_NUMERIC(6, "t", true), "123", "000123", true).back("000123"));
        c.add(new Case("IFB_NUMERIC odd leftpad", () -> new IFB_NUMERIC(3, "t", true), "51", "0051", true).back("051"));
        c.add(new Case("IFB_NUMERIC odd rightpad", () -> new IFB_NUMERIC(3, "t", false), "051", "0510", true));
        c.add(new Case("IFB_LLNUM PAN", () -> new IFB_LLNUM(19, "t", false), "4111111111111111", "164111111111111111", true));
        c.add(new Case("IFB_LLNUM odd", () -> new IFB_LLNUM(19, "t", false), "123", "031230", true));
        c.add(new Case("IFB_LLNUM odd leftpad", () -> new IFB_LLNUM(19, "t", true), "123", "030123", true));
        c.add(new Case("IFB_LLNUM track2", () -> new IFB_LLNUM(37, "t", false), "4111111111111111=2512101", "244111111111111111D2512101", true));
        c.add(new Case("IFB_LLLNUM", () -> new IFB_LLLNUM(999, "t", false), "12345", "0005123450", true));
        // --- binary-length-prefixed char / binary ---
        c.add(new Case("IFB_LLCHAR", () -> new IFB_LLCHAR(25, "t"), "HELLO", "0548454C4C4F", true));
        c.add(new Case("IFB_LLLCHAR", () -> new IFB_LLLCHAR(999, "t"), "ABC", "0003414243", true));
        c.add(new Case("IFB_BINARY", () -> new IFB_BINARY(8, "t"), bytes(8), "A0A1A2A3A4A5A6A7", true));
        c.add(new Case("IFB_LLBINARY", () -> new IFB_LLBINARY(16, "t"), bytes(3), "03A0A1A2", false)); // FINDING: over-length accepted (no max check)
        c.add(new Case("IFB_LLLBINARY", () -> new IFB_LLLBINARY(999, "t"), bytes(3), "0003A0A1A2", false)); // FINDING: over-length accepted (no max check)
        c.add(new Case("IFB_LLHCHAR", () -> new IFB_LLHCHAR(99, "t"), "AB", "024142", true));
        c.add(new Case("IFB_LLHBINARY", () -> new IFB_LLHBINARY(99, "t"), bytes(2), "02A0A1", false)); // FINDING: over-length accepted (no max check)
        c.add(new Case("IFB_AMOUNT", () -> new IFB_AMOUNT(9, "t", true), "C00001000", "4300001000", true));
        // --- fixed char ---
        c.add(new Case("IF_CHAR padded", () -> new IF_CHAR(8, "t"), "TERM01", "5445524D30312020", true).back("TERM01  "));
        // --- ASCII ---
        c.add(new Case("IFA_NUMERIC", () -> new IFA_NUMERIC(6, "t"), "123", "303030313233", true).back("000123"));
        c.add(new Case("IFA_LLNUM", () -> new IFA_LLNUM(19, "t"), "4111", "303434313131", true));
        c.add(new Case("IFA_LLLNUM", () -> new IFA_LLLNUM(999, "t"), "12", "3030323132", true));
        c.add(new Case("IFA_LLCHAR", () -> new IFA_LLCHAR(99, "t"), "HI", "30324849", true));
        c.add(new Case("IFA_LLLCHAR", () -> noArg(new IFA_LLLCHAR(), 999), "HI", "3030324849", true));
        c.add(new Case("IFA_BINARY", () -> new IFA_BINARY(2, "t"), bytes(2), "41304131", true));
        c.add(new Case("IFA_LLBINARY", () -> new IFA_LLBINARY(10, "t"), bytes(2), "3032A0A1", false)); // FINDING: over-length accepted (no max check)
        c.add(new Case("IFA_LLLBINARY", () -> new IFA_LLLBINARY(100, "t"), bytes(2), "303032A0A1", false)); // FINDING: over-length accepted (no max check)
        c.add(new Case("IFA_AMOUNT", () -> new IFA_AMOUNT(9, "t"), "C00001000", "433030303031303030", true));
        // --- EBCDIC ---
        c.add(new Case("IFAE_LLCHAR", () -> new IFAE_LLCHAR(20, "t"), "ABC", "3033C1C2C3", true)); // FINDING: prefix is ASCII "03", not EBCDIC F0F3 (jPOS IFAE_LLCHAR uses EBCDIC prefix)
        // --- tagged TLV (sub-field 1 of field 48) ---
        c.add(new Case("IFA_TTLLCHAR", () -> tagged(new IFA_TTLLCHAR(), 20, "01"), "ABC", "30313033414243", true));
        c.add(new Case("IFA_TTLLLCHAR", () -> tagged(new IFA_TTLLLCHAR(), 99, "01"), "ABC", "3031303033414243", true));
        c.add(new Case("IFA_TTTLLLCHAR", () -> tagged(new IFA_TTTLLLCHAR(), 99, "001"), "ABC", "303031303033414243", true));
        return c;
    }
}
