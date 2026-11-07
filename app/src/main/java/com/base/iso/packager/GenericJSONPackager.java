package com.base.iso.packager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.base.iso.fields.IFB_BITMAP;
import com.base.iso.fields.base.BitMapFieldBase;
import com.base.iso.fields.base.MsgFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.util.json.Json;
import com.base.iso.util.xml.Xml;
import com.base.iso.util.xml.XmlElement;

/**
 * Packager whose field layout is defined at runtime, from a JSON or XML string or a field array.
 *
 * Layout JSON: {@code {"isofields":[{"id":2,"length":19,"name":"PAN","class":"IFB_LLNUM","pad":false}, ...]}}
 * <br>
 * Layout XML:
 * {@code <isopackager><isofield id="2" length="19" name="PAN" class="IFB_LLNUM" pad="false"/>...</isopackager>}
 * <br>
 * Sub-fields (F48/F63-style private data): {@code <isofieldpackager>} in XML, an entry with a
 * {@code "subfields"} array in JSON — built as {@code MsgFieldBase} + {@link GenericSubFieldPackager}.
 * <ul>
 *   <li>{@code class} = simple name of a codec in {@code com.base.iso.fields} (instantiated by
 *       reflection — R8 keep rules ship in META-INF/proguard/iso8583.pro). The qualified form
 *       {@code com.base.iso.fields.X} is also accepted.</li>
 *   <li>{@code id}/{@code length} may be numbers or numeric strings; {@code pad} a boolean or
 *       "true"/"false"; {@code name} optional.</li>
 * </ul>
 * The library never reads files: the consumer reads its layout (Android asset, config service,
 * ...) into a String and calls {@link #setFieldsFromJson(String)} or {@link #setFieldsFromXml(String)}.
 *
 * Thread-safety: pack/unpack are safe to share across threads once configured. Do not reload a
 * layout while other threads pack; build a new instance and swap the reference instead.
 */
public class GenericJSONPackager extends aBasePackager {

    /** Package holding every field codec that a layout may name. */
    private static final String FIELDS_PACKAGE = "com.base.iso.fields.";
    /** Accepted values for a sub-layout's {@code packager} attribute / key. */
    private static final List<String> SUB_PACKAGERS = Arrays.asList(
            "GenericSubFieldPackager",
            "com.base.iso.packager.GenericSubFieldPackager");
    /** Field ids 0 (MTI) .. 128 (primary + secondary bitmap range). */
    private static final int MAX_FIELD_ID = 128;

    private aISOFieldBase[] fld = {};

    public GenericJSONPackager() {
        super();
        // Initialize with default fields from ISO87BPackager
        ISO87BPackager defaultPackager = new ISO87BPackager();
        this.fld = defaultPackager.fld.clone(); // Safe shallow clone
        setFieldPackager(this.fld);
    }

    /**
     * Replace the entire field packager dynamically.
     */
    public void setDynamicFields(aISOFieldBase[] newFields) {
        this.fld = newFields.clone(); // Shallow copy
        setFieldPackager(this.fld);
    }

    /**
     * Get the current list of dynamic fields.
     */
    public List<aISOFieldBase> getDynamicFields() {
        return new ArrayList<>(Arrays.asList(this.fld));
    }

    /**
     * Prints all current field definitions to the screen.
     */
    public void printFields() {
        for (int i = 0; i < fld.length; i++) {
            aISOFieldBase field = fld[i];
            if (field != null) {
                System.out.printf("Field %03d: %-40s (Length: %d, Class: %s) Use Pad: %s %n",
                        i, field.getDescription(), field.getLength(), field.getClass().getSimpleName(), field.getPad());
            }
        }
    }

    /**
     * Replaces the layout with the one described by {@code jsonString}.
     *
     * Fails loudly: any problem (malformed JSON, missing "isofields" array, missing id/length/class,
     * id out of 0..128, duplicate id, unknown or non-codec class, invalid sub-layout) throws
     * {@link IllegalArgumentException} naming the entry path, and the previous layout stays active.
     * The whole layout is built and validated first, then swapped in one assignment.
     *
     * Sub-fields: an entry with a {@code "subfields"} array is a sub-layout. Its own id/length/class
     * describe the outer field (e.g. IFB_LLLBINARY); the array lists the sub-fields. Optional keys
     * (same meaning as the XML attributes): {@code emitBitmap}, {@code bitmapField} (default 1),
     * {@code firstField}, {@code maxValidField}. See {@link GenericSubFieldPackager}.
     *
     * @param jsonString layout document (never a path — the library does not read files)
     * @throws IllegalArgumentException if the layout is invalid
     */
    public void setFieldsFromJson(String jsonString) {
        Object root = Json.parse(jsonString);
        if (!(root instanceof Map))
            throw new IllegalArgumentException("JSON layout must be an object");
        Object fieldsNode = ((Map<?, ?>) root).get("isofields");
        if (!(fieldsNode instanceof List))
            throw new IllegalArgumentException("Missing 'isofields' array in JSON");
        setDynamicFields(buildFields(jsonEntries((List<?>) fieldsNode, "isofields"), "isofields"));
    }

    /**
     * Replaces the layout with the one described by {@code xmlString}. Format: root {@code <isopackager>} with one {@code <isofield id length name class pad/>} per
     * field (attribute values follow the same rules as the JSON keys).
     *
     * Sub-fields: {@code <isofieldpackager id length name class packager="...GenericSubFieldPackager"
     * [emitBitmap] [bitmapField] [firstField] [maxValidField]>} containing {@code <isofield>} (and
     * nested {@code <isofieldpackager>}) children. Only GenericSubFieldPackager
     * semantics are supported; tagged/TLV sub-packagers ({@code tagMapper}, {@code tagSize},
     * {@code lenSize}, other {@code packager} classes) are rejected.
     *
     * Same fail-loud contract as {@link #setFieldsFromJson(String)}; also rejects unknown child
     * elements. A DOCTYPE that only names an external DTD is accepted and ignored (nothing is
     * fetched).
     *
     * @param xmlString layout document (never a path — the library does not read files)
     * @throws IllegalArgumentException if the layout is invalid
     */
    public void setFieldsFromXml(String xmlString) {
        XmlElement root = Xml.parse(xmlString);
        if (!"isopackager".equals(root.getName()))
            throw new IllegalArgumentException("XML layout root must be <isopackager>, found <" + root.getName() + ">");
        setDynamicFields(buildFields(xmlEntries(root, ""), "isofield"));
    }

    /** One layout entry: field attributes, plus the sub-layout entries when it is a sub-layout. */
    private static final class Entry {
        final Map<?, ?> attrs;
        final List<Entry> sub; // null = plain field
        final String where;

        Entry(Map<?, ?> attrs, List<Entry> sub, String where) {
            this.attrs = attrs;
            this.sub = sub;
            this.where = where;
        }
    }

    private static List<Entry> jsonEntries(List<?> nodes, String path) {
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        for (Object node : nodes) {
            String where = path + "[" + i++ + "]";
            if (!(node instanceof Map))
                throw new IllegalArgumentException(where + " is not an object");
            Map<?, ?> m = (Map<?, ?>) node;
            List<Entry> sub = null;
            if (m.containsKey("subfields")) {
                Object s = m.get("subfields");
                if (!(s instanceof List))
                    throw new IllegalArgumentException(where + ": 'subfields' must be an array");
                sub = jsonEntries((List<?>) s, where + ".subfields");
            }
            entries.add(new Entry(m, sub, where));
        }
        return entries;
    }

    private static List<Entry> xmlEntries(XmlElement parent, String path) {
        List<Entry> entries = new ArrayList<>();
        int i = 0;
        for (XmlElement f : parent.getChildren()) {
            String where = path + "isofield[" + i++ + "]";
            if ("isofield".equals(f.getName()))
                entries.add(new Entry(f.getAttributes(), null, where)); // String values: lenient like JSON strings
            else if ("isofieldpackager".equals(f.getName()))
                entries.add(new Entry(f.getAttributes(), xmlEntries(f, where + "."), where));
            else
                throw new IllegalArgumentException(where + ": unexpected element <" + f.getName() + ">");
        }
        return entries;
    }

    /**
     * Builds the codec array for one (sub-)layout. Nothing is published here: the caller swaps the
     * finished top-level array in, so a bad layout never leaves a half-applied state.
     */
    private aISOFieldBase[] buildFields(List<Entry> entries, String label) {
        aISOFieldBase[] tempFields = new aISOFieldBase[MAX_FIELD_ID + 1];
        int maxIndex = -1;
        for (Entry e : entries) {
            Map<?, ?> f = e.attrs;
            int id = intValue(f, "id", e.where);
            int length = intValue(f, "length", e.where);
            String className = stringValue(f, "class", e.where);
            Object nameNode = f.get("name");
            String name = nameNode == null ? "" : String.valueOf(nameNode);
            boolean pad = boolValue(f.get("pad"));

            if (id < 0 || id > MAX_FIELD_ID)
                throw new IllegalArgumentException(e.where + ": id " + id + " out of range 0.." + MAX_FIELD_ID);
            if (tempFields[id] != null)
                throw new IllegalArgumentException(e.where + ": duplicate id " + id);

            aISOFieldBase codec;
            try {
                codec = createFieldFromClass(className, length, name, pad);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(e.where + ": " + ex.getMessage(), ex);
            }
            tempFields[id] = e.sub == null ? codec : subLayout(e, id, codec);
            maxIndex = Math.max(maxIndex, id);
        }
        if (maxIndex < 0)
            throw new IllegalArgumentException("layout has no " + label + " entries");

        aISOFieldBase[] finalFields = new aISOFieldBase[maxIndex + 1];
        System.arraycopy(tempFields, 0, finalFields, 0, maxIndex + 1);
        return finalFields;
    }

    /** Builds {@code MsgFieldBase(outer codec, GenericSubFieldPackager(sub-fields))} for a sub-layout entry. */
    private aISOFieldBase subLayout(Entry e, int id, aISOFieldBase outer) {
        Map<?, ?> f = e.attrs;
        Object packager = f.get("packager");
        if (packager != null && !SUB_PACKAGERS.contains(String.valueOf(packager).trim()))
            throw new IllegalArgumentException(e.where + ": packager '" + packager
                    + "' not supported (only GenericSubFieldPackager semantics)");
        for (String tlv : new String[] { "tagMapper", "tagSize", "lenSize", "swapTagAndLen" })
            if (f.containsKey(tlv))
                throw new IllegalArgumentException(e.where + ": '" + tlv + "' (tagged/TLV sub-fields) is not supported");
        if (f.containsKey("headerLength") && intValue(f, "headerLength", e.where) != 0)
            throw new IllegalArgumentException(e.where + ": 'headerLength' is not supported in sub-layouts");

        aISOFieldBase[] inner = buildFields(e.sub, e.where + " sub-field");
        int bitmapField = f.containsKey("bitmapField") ? intValue(f, "bitmapField", e.where) : 1;
        int firstField = f.containsKey("firstField") ? intValue(f, "firstField", e.where) : -1;
        boolean hasBitmapCodec = bitmapField >= 0 && bitmapField < inner.length
                && inner[bitmapField] instanceof BitMapFieldBase;
        // emitBitmap absent → inferred from the layout: bitmapped when the codec at bitmapField
        // is a bitmap codec.
        boolean bitmapped = f.containsKey("emitBitmap") ? boolValue(f.get("emitBitmap")) : hasBitmapCodec;
        if (f.containsKey("maxValidField")) {
            int maxValid = intValue(f, "maxValidField", e.where);
            if (inner.length - 1 > maxValid)
                throw new IllegalArgumentException(e.where + ": sub-field id " + (inner.length - 1)
                        + " exceeds maxValidField " + maxValid);
        }
        if (bitmapped && hasBitmapCodec && inner[bitmapField] instanceof IFB_BITMAP
                && inner[bitmapField].getLength() > 8
                && (firstField >= 0 ? firstField : bitmapField + 1) <= 1)
            throw new IllegalArgumentException(e.where + ": IFB_BITMAP longer than 8 bytes with data in sub-field 1"
                    + " — bit 1 would be read as 'secondary bitmap present'; use length 8 (or 4)");
        try {
            return new MsgFieldBase(outer, new GenericSubFieldPackager(id, inner, bitmapped, bitmapField, firstField));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(e.where + ": " + ex.getMessage(), ex);
        }
    }

    /** Required integer; accepts a JSON number or a numeric string (old layout files use strings). */
    private static int intValue(Map<?, ?> f, String key, String where) {
        Object v = f.get(key);
        if (v instanceof Long) {
            long l = (Long) v;
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
                throw new IllegalArgumentException(where + ": '" + key + "' out of range: " + l);
            return (int) l;
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(where + ": '" + key + "' is not a number: \"" + v + "\"");
            }
        }
        throw new IllegalArgumentException(where + ": missing or invalid '" + key + "'");
    }

    private static String stringValue(Map<?, ?> f, String key, String where) {
        Object v = f.get(key);
        if (!(v instanceof String) || ((String) v).trim().isEmpty())
            throw new IllegalArgumentException(where + ": missing '" + key + "'");
        return ((String) v).trim();
    }

    /** Optional boolean, same leniency as the former Jackson {@code asBoolean()}: true / "true" / non-zero. */
    private static boolean boolValue(Object v) {
        if (v instanceof Boolean)
            return (Boolean) v;
        if (v instanceof String)
            return "true".equals(((String) v).trim());
        if (v instanceof Long)
            return (Long) v != 0L;
        return false;
    }

    /**
     * Instantiates codec {@code com.base.iso.fields.<shortClassName>}: tries (int,String), then (int),
     * then the no-arg constructor, and applies setPad(boolean).
     * The class is checked to be an {@link aISOFieldBase} before any instance is created.
     *
     * @throws IllegalArgumentException naming the class when it is missing (e.g. stripped by R8),
     *         not a field codec, or cannot be constructed
     */
    private aISOFieldBase createFieldFromClass(String className, int length, String description, boolean pad) {
        String shortClassName = className;
        if (className.startsWith(FIELDS_PACKAGE))
            shortClassName = className.substring(FIELDS_PACKAGE.length());
        String fullClassName = FIELDS_PACKAGE + shortClassName;
        Class<?> clazz;
        try {
            // initialize=false: never run static initialisers of a class we may reject.
            clazz = Class.forName(fullClassName, false, GenericJSONPackager.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown field class '" + shortClassName
                    + "' (" + fullClassName + " not found; if minified with R8, check the iso8583 keep rules)", e);
        }
        if (!aISOFieldBase.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("Class '" + fullClassName + "' is not a field codec (aISOFieldBase)");

        try {
            aISOFieldBase field;
            try {
                // Preferred: (int, String)
                Constructor<?> ctor = clazz.getConstructor(int.class, String.class);
                field = (aISOFieldBase) ctor.newInstance(length, description);
            } catch (NoSuchMethodException e1) {
                try {
                    // Fallback: (int)
                    Constructor<?> ctor = clazz.getConstructor(int.class);
                    field = (aISOFieldBase) ctor.newInstance(length);
                    field.setDescription(description);
                } catch (NoSuchMethodException e2) {
                    // Fallback: no-arg
                    Constructor<?> ctor = clazz.getConstructor();
                    field = (aISOFieldBase) ctor.newInstance();
                    field.setLength(length);
                    field.setDescription(description);
                }
            }

            // setPad is defined on aISOFieldBase; codecs override it to switch interpreter.
            Method setPad = clazz.getMethod("setPad", boolean.class);
            setPad.invoke(field, pad);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalArgumentException("Cannot create field class '" + fullClassName + "': " + e, e);
        }
    }
}
