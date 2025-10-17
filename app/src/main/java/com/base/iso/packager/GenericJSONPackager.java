package com.base.iso.packager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.base.iso.fields.base.aISOFieldBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericJSONPackager extends aBasePackager {

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

    public void setFieldsFromJson(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonString);

            JsonNode fieldsNode = root.get("isofields");
            if (fieldsNode == null || !fieldsNode.isArray()) {
                throw new IllegalArgumentException("Missing 'isofields' array in JSON");
            }

            List<JsonNode> sortedNodes = new ArrayList<>();
            fieldsNode.forEach(sortedNodes::add);

            // Sort by "id"
            sortedNodes.sort(Comparator.comparingInt(n -> n.get("id").asInt()));

            aISOFieldBase[] tempFields = new aISOFieldBase[129];

            for (JsonNode node : sortedNodes) {
                int id = node.get("id").asInt();
                int length = node.get("length").asInt();
                String name = node.get("name").asText();
                boolean pad = node.has("pad") && node.get("pad").asBoolean();
                String className = node.get("class").asText();

                aISOFieldBase field = createFieldFromClass(className, length, name, pad);
                if (field != null && id >= 0 && id < tempFields.length) {
                    tempFields[id] = field;
                }
            }

            // Trim to actual size
            int maxIndex = -1;
            for (int i = tempFields.length - 1; i >= 0; i--) {
                if (tempFields[i] != null) {
                    maxIndex = i;
                    break;
                }
            }

            aISOFieldBase[] finalFields = new aISOFieldBase[maxIndex + 1];
            System.arraycopy(tempFields, 0, finalFields, 0, maxIndex + 1);

            setDynamicFields(finalFields);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private aISOFieldBase createFieldFromClass(String shortClassName, int length, String description, boolean pad) {
    String fullClassName = "com.base.iso.fields." + shortClassName;

    try {
        Class<?> clazz = Class.forName(fullClassName);
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

        // Try setPad if exists
        try {
            Method setPad = clazz.getMethod("setPad", boolean.class);
            setPad.invoke(field, pad);
        } catch (NoSuchMethodException ignored) {}

        return field;

    } catch (Exception e) {
        System.err.println("Packager - Error creating field: " + fullClassName + " → " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}


    public void loadFromJsonFile(String filePath) {
        try {
            String jsonString = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
            setFieldsFromJson(jsonString); // Reuse existing JSON parsing logic
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON from file: " + filePath, e);
        }
    }
}
