package com.base.iso;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.interfaces.iPackager;
import com.base.iso.packager.ISO87BPackager;

/**
 * Shared test fixtures: canonical ISO 8583 messages built the way consumers build them
 * (packager set BEFORE values, so binary fields such as F52 are parsed as hex).
 *
 * The *_HEX constants are golden masters recorded from the pre-change code (commit 402fc3e).
 * Any change that alters them is a wire-format change and must be justified by a host spec.
 */
public final class TestMessages {

    private TestMessages() {}

    /** Test PAN (Visa test range) — never a real card. */
    public static final String PAN = "4111111111111111";
    /** Track 2 equivalent with '=' separator (BCD packs it as nibble D). */
    public static final String TRACK2 = "4111111111111111=2512101123456789";
    /** PIN block as hex (8 bytes binary on the wire). */
    public static final String PIN_BLOCK = "0123456789ABCDEF";
    /** F55 is IFB_LLLCHAR in ISO87B, so ICC data travels as text here. */
    public static final String ICC = "9F2608A1B2C3D4E5F60718";

    public static ISOMsg m0200(iPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0200");
        m.set(2, PAN);
        m.set(3, "000000");
        m.set(4, "000000010000");
        m.set(11, "000123");
        m.set(22, "051");
        m.set(23, "001");
        m.set(35, TRACK2);
        m.set(41, "TERM0001");
        m.set(42, "MERCHANT0000001");
        m.set(52, PIN_BLOCK);
        m.set(55, ICC);
        return m;
    }

    public static ISOMsg m0210(iPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0210");
        m.set(3, "000000");
        m.set(4, "000000010000");
        m.set(11, "000123");
        m.set(37, "123456789012");
        m.set(38, "A1B2C3");
        m.set(39, "00");
        m.set(41, "TERM0001");
        return m;
    }

    public static ISOMsg m0400(iPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0400");
        m.set(2, PAN);
        m.set(3, "000000");
        m.set(4, "000000010000");
        m.set(11, "000124");
        m.set(22, "051");
        m.set(41, "TERM0001");
        m.set(90, "020000012311071200000000000000000000000000");
        return m;
    }

    public static ISOMsg m0800(iPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0800");
        m.set(11, "000001");
        m.set(41, "00000001");
        m.set(70, "301");
        return m;
    }

    public static ISOMsg m0810(iPackager p) throws BaseException {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        m.setMTI("0810");
        m.set(11, "000001");
        m.set(39, "00");
        m.set(41, "00000001");
        m.set(70, "301");
        return m;
    }

    /**
     * The "pad" value a JSON layout needs to reproduce an ISO87B slot.
     * Quirk: {@code IFB_NUMERIC(len, desc, true)} selects the left-padded BCD interpreter in its
     * constructor but leaves {@code getPad()} false, so getPad() alone under-reports it.
     */
    public static boolean iso87bPad(aISOFieldBase f) {
        return f instanceof com.base.iso.fields.IFB_NUMERIC || f.getPad();
    }

    /**
     * Builds a GenericJSONPackager layout JSON equivalent to {@link ISO87BPackager}.
     * Generated text (not a file) so the tests survive the removal of file loading (T15).
     *
     * @param stringTyped true → id/length/pad as JSON strings (the style of the old bundled
     *                    iso87binary.json); false → numbers / booleans
     */
    public static String iso87bLayoutJson(boolean stringTyped) {
        ISO87BPackager ref = new ISO87BPackager();
        StringBuilder sb = new StringBuilder("{\n  \"isofields\": [\n");
        for (int i = 0; i <= 128; i++) {
            aISOFieldBase f = ref.getFieldPackager(i);
            String q = stringTyped ? "\"" : "";
            sb.append("    {\"id\": ").append(q).append(i).append(q)
              .append(", \"length\": ").append(q).append(f.getLength()).append(q)
              .append(", \"name\": \"").append(f.getDescription().replace("\"", "\\\"")).append('"')
              .append(", \"class\": \"").append(f.getClass().getSimpleName()).append('"')
              .append(", \"pad\": ").append(q).append(iso87bPad(f)).append(q)
              .append('}').append(i < 128 ? ",\n" : "\n");
        }
        return sb.append("  ]\n}\n").toString();
    }

    /**
     * The same ISO87B layout in the jPOS GenericPackager XML format, with a jPOS DOCTYPE line and
     * jPOS-qualified class names (org.jpos.iso.X) to prove ported layouts load unchanged.
     */
    public static String iso87bLayoutXml() {
        ISO87BPackager ref = new ISO87BPackager();
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE isopackager SYSTEM \"genericpackager.dtd\">\n<isopackager>\n");
        for (int i = 0; i <= 128; i++) {
            aISOFieldBase f = ref.getFieldPackager(i);
            sb.append("  <isofield id=\"").append(i)
              .append("\" length=\"").append(f.getLength())
              .append("\" name=\"").append(com.base.iso.util.xml.Xml.escapeAttribute(f.getDescription()))
              .append("\" class=\"org.jpos.iso.").append(f.getClass().getSimpleName())
              .append("\" pad=\"").append(iso87bPad(f)).append("\"/>\n");
        }
        return sb.append("</isopackager>\n").toString();
    }
}
