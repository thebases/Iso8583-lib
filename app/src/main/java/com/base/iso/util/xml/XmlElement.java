package com.base.iso.util.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One XML element: name, attributes (document order), child elements and text content.
 * Text is the concatenation of the element's direct text / CDATA nodes (mixed-content order is
 * not preserved — enough for configuration and message documents).
 *
 * Built by {@link Xml#parse(String)} or by hand for {@link Xml#write(XmlElement)}. Not thread-safe
 * while being built; safe to share once complete.
 */
public final class XmlElement {

    private final String name;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<XmlElement> children = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();

    public XmlElement(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("element name is empty");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** @return attribute value, or null when absent */
    public String getAttribute(String attr) {
        return attributes.get(attr);
    }

    /** Read-only view of the attributes in document order. */
    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /** Read-only view of the child elements in document order. */
    public List<XmlElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /** @return direct children named {@code childName} */
    public List<XmlElement> getChildren(String childName) {
        List<XmlElement> l = new ArrayList<>();
        for (XmlElement c : children)
            if (c.name.equals(childName))
                l.add(c);
        return l;
    }

    /** @return first direct child named {@code childName}, or null */
    public XmlElement getChild(String childName) {
        for (XmlElement c : children)
            if (c.name.equals(childName))
                return c;
        return null;
    }

    /** Text content (entities decoded). Empty string when none. */
    public String getText() {
        return text.toString();
    }

    /** Sets (or replaces) an attribute; returns this for chaining. */
    public XmlElement setAttribute(String attr, String value) {
        if (attr == null || attr.isEmpty())
            throw new IllegalArgumentException("attribute name is empty");
        if (value == null)
            attributes.remove(attr);
        else
            attributes.put(attr, value);
        return this;
    }

    /** Appends a child; returns the child for chaining. */
    public XmlElement addChild(XmlElement child) {
        children.add(child);
        return child;
    }

    /** Creates, appends and returns a new child element. */
    public XmlElement addChild(String childName) {
        return addChild(new XmlElement(childName));
    }

    /** Replaces the text content; returns this. */
    public XmlElement setText(String value) {
        text.setLength(0);
        if (value != null)
            text.append(value);
        return this;
    }

    void appendText(String value) {
        text.append(value);
    }

    /** Indented XML for this element (no XML declaration). */
    @Override
    public String toString() {
        return Xml.write(this);
    }
}
