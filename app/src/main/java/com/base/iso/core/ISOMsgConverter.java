package com.base.iso.core;

import java.util.LinkedHashMap;
import java.util.Map;

import com.base.iso.interfaces.iPackager;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.json.Json;
import com.base.iso.util.xml.Xml;
import com.base.iso.util.xml.XmlElement;

/**
 * Converts an {@link ISOMsg} to and from JSON / XML text with the library's built-in readers and
 * writers (no third-party library). Useful for APIs, test fixtures and diagnostics. This is a
 * textual view of the fields — it does not change how messages are packed on the wire.
 *
 * <p><b>JSON</b>
 * <pre>
 * {"header": "6000010002",
 *  "fields": {"0": "0200", "2": "4111111111111111",
 *             "52": {"binary": "0123456789ABCDEF"},
 *             "48": {"fields": {"1": "ABC"}}}}
 * </pre>
 * <b>XML</b>
 * <pre>
 * &lt;isomsg&gt;
 *   &lt;header&gt;6000010002&lt;/header&gt;
 *   &lt;field id="0" value="0200"/&gt;
 *   &lt;field id="52" value="0123456789ABCDEF" type="binary"/&gt;
 *   &lt;isomsg id="48"&gt;&lt;field id="1" value="ABC"/&gt;&lt;/isomsg&gt;
 * &lt;/isomsg&gt;
 * </pre>
 * Field 0 is the MTI; the bitmap is not written (it is recomputed on pack). Binary values and the
 * header are upper-case hex.
 *
 * <p><b>Card data:</b> the output is cleartext. For logs, mask first:
 * {@code ISOMsgConverter.toJson(new ProtectLogListener().mask(msg))}.
 *
 * <p>Stateless; thread-safe.
 */
public final class ISOMsgConverter {

    private ISOMsgConverter() {}

    // ------------------------------------------------------------------ JSON

    /** Compact JSON for {@code m}. */
    public static String toJson(ISOMsg m) throws BaseException {
        return Json.write(toJsonTree(m, true));
    }

    /** Indented JSON for {@code m}. */
    public static String toJsonPretty(ISOMsg m) throws BaseException {
        return Json.writePretty(toJsonTree(m, true));
    }

    /**
     * Rebuilds a message from {@link #toJson} output.
     * @param packager set on the result when not null (needed before {@code pack()})
     * @throws BaseException on malformed JSON or an unsupported shape (message names the field)
     */
    public static ISOMsg fromJson(String json, iPackager packager) throws BaseException {
        Object root;
        try {
            root = Json.parse(json);
        } catch (IllegalArgumentException e) {
            throw new BaseException(e.getMessage(), e);
        }
        if (!(root instanceof Map))
            throw new BaseException("ISOMsg JSON must be an object");
        ISOMsg m = new ISOMsg();
        readJsonFields(m, (Map<?, ?>) root, "");
        Object header = ((Map<?, ?>) root).get("header");
        if (header != null)
            m.setHeader(hex(header, "header"));
        if (packager != null)
            m.setPackager(packager);
        return m;
    }

    static Map<String, Object> toJsonTree(ISOMsg m, boolean top) throws BaseException {
        Map<String, Object> node = new LinkedHashMap<>();
        if (top && m.getHeader() != null)
            node.put("header", ISOUtil.hexString(m.getHeader()));
        Map<String, Object> fields = new LinkedHashMap<>();
        for (Object o : m.getChildren().values()) {
            BaseComponent c = (BaseComponent) o;
            int fld = (Integer) c.getKey();
            if (fld < 0)
                continue; // bitmap
            if (c instanceof ISOMsg) {
                fields.put(Integer.toString(fld), toJsonTree((ISOMsg) c, false));
            } else if (c.getValue() instanceof byte[]) {
                Map<String, Object> bin = new LinkedHashMap<>();
                bin.put("binary", ISOUtil.hexString((byte[]) c.getValue()));
                fields.put(Integer.toString(fld), bin);
            } else {
                fields.put(Integer.toString(fld), String.valueOf(c.getValue()));
            }
        }
        node.put("fields", fields);
        return node;
    }

    private static void readJsonFields(ISOMsg m, Map<?, ?> node, String path) throws BaseException {
        Object fields = node.get("fields");
        if (!(fields instanceof Map))
            throw new BaseException("missing 'fields' object" + at(path));
        for (Map.Entry<?, ?> e : ((Map<?, ?>) fields).entrySet()) {
            String fpath = path.isEmpty() ? String.valueOf(e.getKey()) : path + "." + e.getKey();
            int fld = fieldNumber(String.valueOf(e.getKey()), fpath);
            Object v = e.getValue();
            if (v instanceof String) {
                m.set(new ISOField(fld, (String) v));
            } else if (v instanceof Map && ((Map<?, ?>) v).containsKey("binary")) {
                m.set(new BinaryFieldComponent(fld, hex(((Map<?, ?>) v).get("binary"), "field " + fpath)));
            } else if (v instanceof Map && ((Map<?, ?>) v).containsKey("fields")) {
                ISOMsg sub = new ISOMsg(fld);
                readJsonFields(sub, (Map<?, ?>) v, fpath);
                m.set(sub);
            } else {
                throw new BaseException("field " + fpath + ": expected a string, {\"binary\":hex} or {\"fields\":{...}}");
            }
        }
    }

    // ------------------------------------------------------------------- XML

    /** Indented XML for {@code m}. */
    public static String toXml(ISOMsg m) throws BaseException {
        return Xml.write(toXmlTree(m, true));
    }

    /**
     * Rebuilds a message from {@link #toXml} output.
     * @param packager set on the result when not null (needed before {@code pack()})
     * @throws BaseException on malformed XML or an unsupported shape (message names the field)
     */
    public static ISOMsg fromXml(String xml, iPackager packager) throws BaseException {
        XmlElement root;
        try {
            root = Xml.parse(xml);
        } catch (IllegalArgumentException e) {
            throw new BaseException(e.getMessage(), e);
        }
        if (!"isomsg".equals(root.getName()))
            throw new BaseException("ISOMsg XML root must be <isomsg>, found <" + root.getName() + ">");
        ISOMsg m = new ISOMsg();
        readXmlFields(m, root, "");
        if (packager != null)
            m.setPackager(packager);
        return m;
    }

    private static XmlElement toXmlTree(ISOMsg m, boolean top) throws BaseException {
        XmlElement node = new XmlElement("isomsg");
        if (!top)
            node.setAttribute("id", Integer.toString(m.getFieldNumber()));
        if (top && m.getHeader() != null)
            node.addChild("header").setText(ISOUtil.hexString(m.getHeader()));
        for (Object o : m.getChildren().values()) {
            BaseComponent c = (BaseComponent) o;
            int fld = (Integer) c.getKey();
            if (fld < 0)
                continue; // bitmap
            if (c instanceof ISOMsg) {
                node.addChild(toXmlTree((ISOMsg) c, false));
            } else if (c.getValue() instanceof byte[]) {
                node.addChild("field").setAttribute("id", Integer.toString(fld))
                        .setAttribute("value", ISOUtil.hexString((byte[]) c.getValue()))
                        .setAttribute("type", "binary");
            } else {
                node.addChild("field").setAttribute("id", Integer.toString(fld))
                        .setAttribute("value", String.valueOf(c.getValue()));
            }
        }
        return node;
    }

    private static void readXmlFields(ISOMsg m, XmlElement node, String path) throws BaseException {
        for (XmlElement e : node.getChildren()) {
            if ("header".equals(e.getName())) {
                if (!path.isEmpty())
                    throw new BaseException("<header> only allowed on the top-level <isomsg>" + at(path));
                m.setHeader(hex(e.getText().trim(), "header"));
                continue;
            }
            String id = e.getAttribute("id");
            String fpath = path.isEmpty() ? String.valueOf(id) : path + "." + id;
            if (id == null)
                throw new BaseException("<" + e.getName() + "> without id" + at(path));
            int fld = fieldNumber(id, fpath);
            if ("field".equals(e.getName())) {
                String value = e.getAttribute("value");
                if (value == null)
                    throw new BaseException("field " + fpath + ": missing value attribute");
                if ("binary".equals(e.getAttribute("type")))
                    m.set(new BinaryFieldComponent(fld, hex(value, "field " + fpath)));
                else
                    m.set(new ISOField(fld, value));
            } else if ("isomsg".equals(e.getName())) {
                ISOMsg sub = new ISOMsg(fld);
                readXmlFields(sub, e, fpath);
                m.set(sub);
            } else {
                throw new BaseException("unexpected element <" + e.getName() + ">" + at(path));
            }
        }
    }

    // --------------------------------------------------------------- helpers

    private static int fieldNumber(String key, String fpath) throws BaseException {
        try {
            int n = Integer.parseInt(key.trim());
            if (n < 0)
                throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new BaseException("invalid field number '" + fpath + "'");
        }
    }

    private static byte[] hex(Object v, String what) throws BaseException {
        if (!(v instanceof String))
            throw new BaseException(what + ": expected a hex string");
        try {
            return ISOUtil.hex2byte((String) v);
        } catch (IllegalArgumentException e) {
            throw new BaseException(what + ": " + e.getMessage(), e);
        }
    }

    private static String at(String path) {
        return path.isEmpty() ? "" : " in field " + path;
    }
}
