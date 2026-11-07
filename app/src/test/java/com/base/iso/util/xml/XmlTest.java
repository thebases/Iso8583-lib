package com.base.iso.util.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Built-in XML reader/writer (no javax.xml, no third-party library). */
class XmlTest {

    @Test
    void parsesElementsAttributesAndText() {
        XmlElement r = Xml.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!-- layout -->\n"
                + "<root a=\"1\" b='two'>\n"
                + "  <child id=\"x\"/>\n"
                + "  <child id=\"y\">text &amp; more</child>\n"
                + "  <other><![CDATA[<raw> & stuff]]></other>\n"
                + "  <?pi ignored?>\n"
                + "</root>\n<!-- trailer -->\n");
        assertEquals("root", r.getName());
        assertEquals("1", r.getAttribute("a"));
        assertEquals("two", r.getAttribute("b"));
        assertNull(r.getAttribute("c"));
        assertEquals(3, r.getChildren().size());
        assertEquals(2, r.getChildren("child").size());
        assertEquals("text & more", r.getChildren("child").get(1).getText());
        assertEquals("<raw> & stuff", r.getChild("other").getText());
        assertEquals("", r.getText()); // formatting whitespace is not content
    }

    @Test
    void decodesEntitiesAndCharacterReferences() {
        XmlElement r = Xml.parse("<e v=\"&lt;&gt;&amp;&quot;&apos;&#65;&#x42;&#x1F600;\">&#233;</e>");
        assertEquals("<>&\"'AB😀", r.getAttribute("v"));
        assertEquals("é", r.getText());
    }

    @Test
    void skipsExternalDoctypeWithoutFetching() {
        XmlElement r = Xml.parse("<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE isopackager SYSTEM \"http://example.invalid/layout.dtd\">\n"
                + "<isopackager/>");
        assertEquals("isopackager", r.getName());
    }

    @Test
    void rejectsDoctypeInternalSubset() {
        // billion-laughs / XXE vectors need an internal subset → refused outright
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Xml.parse(
                "<!DOCTYPE x [<!ENTITY a \"aaaa\">]><x>&a;</x>"));
        assertTrue(e.getMessage().contains("internal subset"), e.getMessage());
    }

    @Test
    void rejectsMalformedInput() {
        for (String bad : new String[] {
                "", "text only", "<a>", "<a></b>", "<a b=1/>", "<a b=\"1\" b=\"2\"/>", "<a b=\"<\"/>",
                "<a>&unknown;</a>", "<a>&#xZZ;</a>", "<a/><b/>", "<a><!-- -- --></a>", "<a><![CDATA[x</a>",
                "<a b=\"1\"c=\"2\"/>", "<a>]]></a>" })
            assertThrows(IllegalArgumentException.class, () -> Xml.parse(bad), bad);
        assertThrows(IllegalArgumentException.class, () -> Xml.parse(null));
    }

    @Test
    void reportsOffset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Xml.parse("<a></b>"));
        assertTrue(e.getMessage().contains("offset 3"), e.getMessage());
    }

    @Test
    void nestingLimit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("<a>");
        assertThrows(IllegalArgumentException.class, () -> Xml.parse(sb.toString()));
    }

    @Test
    void writesAndRoundTrips() {
        XmlElement root = new XmlElement("isomsg");
        root.addChild("header").setText("6000010002");
        root.addChild("field").setAttribute("id", "2").setAttribute("value", "a<b>&\"c\"\tx");
        root.addChild("empty");
        String xml = Xml.write(root);
        assertEquals("<isomsg>\n"
                + "  <header>6000010002</header>\n"
                + "  <field id=\"2\" value=\"a&lt;b&gt;&amp;&quot;c&quot;&#9;x\"/>\n"
                + "  <empty/>\n"
                + "</isomsg>\n", xml);

        XmlElement back = Xml.parse(xml);
        assertEquals("6000010002", back.getChild("header").getText());
        assertEquals("a<b>&\"c\"\tx", back.getChild("field").getAttribute("value"));
        assertEquals(xml, Xml.write(back));
    }

    @Test
    void writerRejectsIllegalCharacters() {
        XmlElement e = new XmlElement("a").setText("bad\u0001");
        assertThrows(IllegalArgumentException.class, () -> Xml.write(e));
    }
}
