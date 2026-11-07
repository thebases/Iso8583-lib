package com.base.iso.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.json.Json;

/** Every log event, and ISOMsg.dump, must be one valid JSON document. */
class LogEventJsonTest {

    private static Map<?, ?> parse(String text) {
        Object v = Json.parse(text); // throws IllegalArgumentException if not valid JSON
        assertTrue(v instanceof Map, text);
        return (Map<?, ?>) v;
    }

    /** Logger whose listeners mask, then keep each event's text. */
    private static List<String> capture(ISO87BPackager p) {
        List<String> events = new ArrayList<>();
        Logger logger = new Logger();
        logger.addListener(new ProtectLogListener());
        logger.addListener(ev -> {
            events.add(ev.toString());
            return ev;
        });
        p.setLogger(logger, "test");
        return events;
    }

    @Test
    void eventWithMixedPayloadIsValidJson() throws Exception {
        ISO87BPackager p = new ISO87BPackager();
        LogEvent evt = new LogEvent(p, "send");
        evt.addMessage(TestMessages.m0200(p));
        evt.addMessage(new byte[] { 1, 2, 3 });
        evt.addMessage(new BaseException("boom \"quoted\" <xml>"));
        evt.addMessage("peer-disconnect", "EOF");
        evt.addMessage(new Object[] { "a", 1, null });
        evt.addMessage((Object) null);

        Map<?, ?> root = parse(evt.toString());
        assertTrue(root.containsKey("log-realm"));
        assertTrue(root.containsKey("at"));
        List<?> items = (List<?>) root.get("send");
        assertEquals(6, items.size());

        Map<?, ?> isomsg = (Map<?, ?>) ((Map<?, ?>) items.get(0)).get("isomsg");
        Map<?, ?> fields = (Map<?, ?>) isomsg.get("fields");
        assertEquals("0200", fields.get("0"));
        assertTrue(isomsg.get("bitmap") instanceof List);
        assertEquals("[3 bytes]", items.get(1));
        assertEquals("boom \"quoted\" <xml>", ((Map<?, ?>) items.get(2)).get("message"));
        assertEquals("EOF", ((Map<?, ?>) items.get(3)).get("peer-disconnect"));
    }

    @Test
    void isoMsgDumpIsValidJson() throws Exception {
        ISOMsg m = TestMessages.m0200(new ISO87BPackager());
        m.setDirection(ISOMsg.OUTGOING);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.dump(new PrintStream(out, true), "    ");
        Map<?, ?> isomsg = (Map<?, ?>) parse(out.toString()).get("isomsg");
        assertEquals("outgoing", isomsg.get("direction"));
        assertTrue(((Map<?, ?>) isomsg.get("fields")).containsKey("52"));
    }

    @Test
    void packagerUnpackEventsAreValidJsonAndCarryNoValues() throws Exception {
        ISO87BPackager p = new ISO87BPackager();
        byte[] packed = TestMessages.m0200(p).pack();
        List<String> events = capture(p);

        ISOMsg back = new ISOMsg();
        back.setPackager(p);
        back.unpack(packed);                              // byte[] path
        ISOMsg back2 = new ISOMsg();
        back2.setPackager(p);
        back2.unpack(new ByteArrayInputStream(packed));   // stream path

        assertFalse(events.isEmpty());
        for (String e : events) {
            Map<?, ?> root = parse(e);
            assertFalse(e.contains(TestMessages.PAN), e);
            assertFalse(e.contains("\"value\""), "values must be off by default: " + e);
            if (root.containsKey("unpack"))
                assertTrue(e.contains("\"codec\""), e);
        }
    }
}
