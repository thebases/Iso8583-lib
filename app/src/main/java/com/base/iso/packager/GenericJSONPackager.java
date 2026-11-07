package com.base.iso.packager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.base.iso.fields.base.aISOFieldBase;

/**
 * Packager whose field layout is defined at runtime, from a JSON string or a field array.
 *
 * Layout JSON: {@code {"isofields":[{"id":2,"length":19,"name":"PAN","class":"IFB_LLNUM","pad":false}, ...]}}
 * <ul>
 *   <li>{@code class} = simple name of a codec in {@code com.base.iso.fields} (instantiated by
 *       reflection — R8 keep rules ship in META-INF/proguard/iso8583.pro).</li>
 *   <li>{@code id}/{@code length} may be numbers or numeric strings; {@code pad} a boolean or
 *       "true"/"false"; {@code name} optional.</li>
 * </ul>
 * The library never reads files: the consumer reads its layout (Android asset, config service,
 * ...) into a String and calls {@link #setFieldsFromJson(String)}.
 *
 * Thread-safety: pack/unpack are safe to share across threads once configured. Do not reload a
 * layout while other threads pack; build a new instance and swap the reference instead.
 */
public class GenericJSONPackager extends aBasePackager {

    /** Package holding every field codec that a layout may name. */
    private static final String FIELDS_PACKAGE = "com.base.iso.fields.";
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
     * id out of 0..128, duplicate id, unknown or non-codec class) throws
     * {@link IllegalArgumentException} naming the entry, and the previous layout stays active.
     * The whole layout is built and validated first, then swapped in one assignment.
     *
     * @param jsonString layout document (never a path — the library does not read files)
     * @throws IllegalArgumentException if the layout is invalid
     */
    public void setFieldsFromJson(String jsonString) {
        // 1. Parse (JDK-only reader; offsets in error messages).
        Object root = LayoutJsonReader.parse(jsonString);
        if (!(root instanceof Map))
            throw new IllegalArgumentException("JSON layout must be an object");
        Object fieldsNode = ((Map<?, ?>) root).get("isofields");
        if (!(fieldsNode instanceof List))
            throw new IllegalArgumentException("Missing 'isofields' array in JSON");

        // 2. Build every codec into a temp array; nothing is published until all succeed.
        aISOFieldBase[] tempFields = new aISOFieldBase[MAX_FIELD_ID + 1];
        int maxIndex = -1;
        int entry = 0;
        for (Object node : (List<?>) fieldsNode) {
            if (!(node instanceof Map))
                throw new IllegalArgumentException("isofields[" + entry + "] is not an object");
            Map<?, ?> f = (Map<?, ?>) node;
            int id = intValue(f, "id", entry);
            int length = intValue(f, "length", entry);
            String className = stringValue(f, "class", entry);
            Object nameNode = f.get("name");
            String name = nameNode == null ? "" : String.valueOf(nameNode);
            boolean pad = boolValue(f.get("pad"));

            if (id < 0 || id > MAX_FIELD_ID)
                throw new IllegalArgumentException("isofields[" + entry + "]: id " + id + " out of range 0.." + MAX_FIELD_ID);
            if (tempFields[id] != null)
                throw new IllegalArgumentException("isofields[" + entry + "]: duplicate id " + id);

            tempFields[id] = createFieldFromClass(className, length, name, pad);
            maxIndex = Math.max(maxIndex, id);
            entry++;
        }
        if (maxIndex < 0)
            throw new IllegalArgumentException("'isofields' array is empty");

        // 3. Trim to the highest defined id and swap in.
        aISOFieldBase[] finalFields = new aISOFieldBase[maxIndex + 1];
        System.arraycopy(tempFields, 0, finalFields, 0, maxIndex + 1);
        setDynamicFields(finalFields);
    }

    /** Required integer; accepts a JSON number or a numeric string (old layout files use strings). */
    private static int intValue(Map<?, ?> f, String key, int entry) {
        Object v = f.get(key);
        if (v instanceof Long) {
            long l = (Long) v;
            if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
                throw new IllegalArgumentException("isofields[" + entry + "]: '" + key + "' out of range: " + l);
            return (int) l;
        }
        if (v instanceof String) {
            try {
                return Integer.parseInt(((String) v).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("isofields[" + entry + "]: '" + key + "' is not a number: \"" + v + "\"");
            }
        }
        throw new IllegalArgumentException("isofields[" + entry + "]: missing or invalid '" + key + "'");
    }

    private static String stringValue(Map<?, ?> f, String key, int entry) {
        Object v = f.get(key);
        if (!(v instanceof String) || ((String) v).trim().isEmpty())
            throw new IllegalArgumentException("isofields[" + entry + "]: missing '" + key + "'");
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
    private aISOFieldBase createFieldFromClass(String shortClassName, int length, String description, boolean pad) {
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
