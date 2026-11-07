package com.base.iso.util.xml;

import java.util.Map;

/**
 * Built-in XML support (no third-party library, no javax.xml; JDK-only, Android API 24 safe,
 * thread-safe).
 *
 * <pre>
 *   XmlElement root = Xml.parse(text);
 *   String out      = Xml.write(root);
 * </pre>
 *
 * Parser scope: elements, attributes, text, CDATA, comments, processing instructions, the XML
 * declaration, the predefined entities ({@code &lt; &gt; &amp; &quot; &apos;}) and character
 * references ({@code &#65; &#x41;}). Namespaces are not interpreted (a prefixed name such as
 * {@code a:b} is kept as-is).
 *
 * Security: no external resources are ever loaded. A DOCTYPE that only names an external DTD
 * (e.g. {@code <!DOCTYPE isopackager SYSTEM "layout.dtd">}) is skipped; a DOCTYPE
 * with an internal subset ({@code [...]}) is rejected, so entity-expansion attacks cannot run.
 */
public final class Xml {

    static final int MAX_DEPTH = 64;

    private Xml() {}

    /**
     * Parses a complete document and returns its root element.
     * @throws IllegalArgumentException on malformed or unsupported input (message has the offset)
     */
    public static XmlElement parse(String xml) {
        if (xml == null)
            throw new IllegalArgumentException("Invalid XML: input is null");
        return new Parser(xml).document();
    }

    /** Indented XML for {@code root} (2 spaces, no XML declaration). */
    public static String write(XmlElement root) {
        StringBuilder sb = new StringBuilder();
        write(sb, root, "", 0);
        return sb.toString();
    }

    /** Escapes text content: {@code & < >}. */
    public static String escapeText(String s) {
        return escape(s, false);
    }

    /** Escapes an attribute value: {@code & < > "} plus tab/CR/LF (kept by parsers as-is). */
    public static String escapeAttribute(String s) {
        return escape(s, true);
    }

    private static String escape(String s, boolean attr) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"':
                    sb.append(attr ? "&quot;" : "\"");
                    break;
                case '\t': case '\n': case '\r':
                    if (attr)
                        sb.append("&#").append((int) c).append(';');
                    else
                        sb.append(c);
                    break;
                default:
                    if (c < 0x20)
                        throw new IllegalArgumentException("character 0x" + Integer.toHexString(c) + " is not allowed in XML 1.0");
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void write(StringBuilder sb, XmlElement e, String indent, int depth) {
        if (depth > MAX_DEPTH)
            throw new IllegalArgumentException("XML element nested deeper than " + MAX_DEPTH);
        sb.append(indent).append('<').append(e.getName());
        for (Map.Entry<String, String> a : e.getAttributes().entrySet())
            sb.append(' ').append(a.getKey()).append("=\"").append(escapeAttribute(a.getValue())).append('"');
        String text = e.getText();
        if (e.getChildren().isEmpty() && text.isEmpty()) {
            sb.append("/>\n");
            return;
        }
        sb.append('>');
        if (e.getChildren().isEmpty()) {
            sb.append(escapeText(text)).append("</").append(e.getName()).append(">\n");
            return;
        }
        sb.append('\n');
        if (!text.isEmpty())
            sb.append(indent).append("  ").append(escapeText(text)).append('\n');
        for (XmlElement c : e.getChildren())
            write(sb, c, indent + "  ", depth + 1);
        sb.append(indent).append("</").append(e.getName()).append(">\n");
    }

    /** Single-use recursive-descent parser. */
    private static final class Parser {
        private final String s;
        private int pos;
        private int depth;

        Parser(String s) {
            this.s = s;
        }

        XmlElement document() {
            if (s.startsWith("﻿"))
                pos++;
            misc(true);
            if (!startsWith("<") || pos + 1 >= s.length() || !isNameStart(s.charAt(pos + 1)))
                throw error("expected root element");
            XmlElement root = element();
            misc(false);
            if (pos != s.length())
                throw error("unexpected content after root element");
            return root;
        }

        /** Whitespace, comments, PIs (and one DOCTYPE before the root). */
        private void misc(boolean prolog) {
            for (;;) {
                skipWs();
                if (startsWith("<?")) {
                    skipPast("?>", "unterminated processing instruction");
                } else if (startsWith("<!--")) {
                    comment();
                } else if (prolog && startsWith("<!DOCTYPE")) {
                    doctype();
                } else {
                    return;
                }
            }
        }

        private void doctype() {
            int start = pos;
            while (pos < s.length() && s.charAt(pos) != '>') {
                char c = s.charAt(pos);
                if (c == '[')
                    throw error("DOCTYPE internal subset is not supported (entity declarations are refused)", pos);
                if (c == '"' || c == '\'') {
                    int end = s.indexOf(c, pos + 1);
                    if (end < 0)
                        throw error("unterminated DOCTYPE", start);
                    pos = end;
                }
                pos++;
            }
            if (pos >= s.length())
                throw error("unterminated DOCTYPE", start);
            pos++; // '>'
        }

        private void comment() {
            int start = pos;
            pos += 4;
            int end = s.indexOf("--", pos);
            if (end < 0)
                throw error("unterminated comment", start);
            if (!s.startsWith("-->", end))
                throw error("'--' not allowed inside a comment", end);
            pos = end + 3;
        }

        private XmlElement element() {
            if (++depth > MAX_DEPTH)
                throw error("nesting deeper than " + MAX_DEPTH);
            pos++; // '<'
            String name = name();
            XmlElement e = new XmlElement(name);
            for (;;) {
                boolean ws = skipWs();
                if (startsWith("/>")) {
                    pos += 2;
                    depth--;
                    return e;
                }
                if (startsWith(">")) {
                    pos++;
                    break;
                }
                if (!ws)
                    throw error("expected whitespace, '>' or '/>'");
                int at = pos;
                String attr = name();
                skipWs();
                expect('=');
                skipWs();
                if (e.getAttribute(attr) != null)
                    throw error("duplicate attribute '" + attr + "'", at);
                e.setAttribute(attr, attributeValue());
            }
            content(e);
            depth--;
            return e;
        }

        private void content(XmlElement e) {
            for (;;) {
                if (pos >= s.length())
                    throw error("unclosed element <" + e.getName() + ">");
                if (startsWith("</")) {
                    int at = pos;
                    pos += 2;
                    String close = name();
                    skipWs();
                    expect('>');
                    if (!close.equals(e.getName()))
                        throw error("mismatched </" + close + ">, expected </" + e.getName() + ">", at);
                    return;
                }
                if (startsWith("<!--")) {
                    comment();
                } else if (startsWith("<![CDATA[")) {
                    int start = pos;
                    int end = s.indexOf("]]>", pos + 9);
                    if (end < 0)
                        throw error("unterminated CDATA", start);
                    e.appendText(s.substring(pos + 9, end));
                    pos = end + 3;
                } else if (startsWith("<?")) {
                    skipPast("?>", "unterminated processing instruction");
                } else if (startsWith("<!")) {
                    throw error("markup declaration not allowed in content");
                } else if (startsWith("<")) {
                    e.addChild(element());
                } else {
                    e.appendText(text());
                }
            }
        }

        /** Character data up to the next '<', with entities decoded. */
        private String text() {
            StringBuilder sb = new StringBuilder();
            while (pos < s.length() && s.charAt(pos) != '<') {
                char c = s.charAt(pos);
                if (c == '&') {
                    reference(sb);
                } else {
                    if (c == '>' && s.startsWith("]]>", pos - 2))
                        throw error("']]>' not allowed in text");
                    sb.append(c);
                    pos++;
                }
            }
            // whitespace-only runs between elements are formatting, not content
            String t = sb.toString();
            return t.trim().isEmpty() ? "" : t;
        }

        private String attributeValue() {
            if (pos >= s.length())
                throw error("expected quoted attribute value");
            char q = s.charAt(pos);
            if (q != '"' && q != '\'')
                throw error("expected quoted attribute value");
            int start = pos++;
            StringBuilder sb = new StringBuilder();
            for (;;) {
                if (pos >= s.length())
                    throw error("unterminated attribute value", start);
                char c = s.charAt(pos);
                if (c == q) {
                    pos++;
                    return sb.toString();
                }
                if (c == '<')
                    throw error("'<' not allowed in attribute value");
                if (c == '&') {
                    reference(sb);
                } else {
                    sb.append(c);
                    pos++;
                }
            }
        }

        private void reference(StringBuilder sb) {
            int start = pos;
            int end = s.indexOf(';', pos);
            if (end < 0 || end - pos > 12)
                throw error("unterminated entity reference", start);
            String ref = s.substring(pos + 1, end);
            pos = end + 1;
            switch (ref) {
                case "lt": sb.append('<'); return;
                case "gt": sb.append('>'); return;
                case "amp": sb.append('&'); return;
                case "quot": sb.append('"'); return;
                case "apos": sb.append('\''); return;
                default:
                    if (ref.startsWith("#")) {
                        try {
                            int cp = ref.startsWith("#x")
                                    ? Integer.parseInt(ref.substring(2), 16)
                                    : Integer.parseInt(ref.substring(1));
                            sb.appendCodePoint(cp);
                            return;
                        } catch (IllegalArgumentException bad) {
                            throw error("invalid character reference &" + ref + ";", start);
                        }
                    }
                    throw error("unknown entity &" + ref + ";", start);
            }
        }

        private String name() {
            int start = pos;
            if (pos >= s.length() || !isNameStart(s.charAt(pos)))
                throw error("expected a name");
            pos++;
            while (pos < s.length() && isNameChar(s.charAt(pos)))
                pos++;
            return s.substring(start, pos);
        }

        private static boolean isNameStart(char c) {
            return Character.isLetter(c) || c == '_' || c == ':';
        }

        private static boolean isNameChar(char c) {
            return isNameStart(c) || Character.isDigit(c) || c == '-' || c == '.';
        }

        private boolean skipWs() {
            int start = pos;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
                    break;
                pos++;
            }
            return pos > start;
        }

        private void skipPast(String end, String msg) {
            int at = s.indexOf(end, pos);
            if (at < 0)
                throw error(msg);
            pos = at + end.length();
        }

        private boolean startsWith(String t) {
            return s.startsWith(t, pos);
        }

        private void expect(char c) {
            if (pos >= s.length() || s.charAt(pos) != c)
                throw error("expected '" + c + "'");
            pos++;
        }

        private IllegalArgumentException error(String msg) {
            return error(msg, pos);
        }

        private IllegalArgumentException error(String msg, int at) {
            return new IllegalArgumentException("Invalid XML: " + msg + " at offset " + at);
        }
    }
}
