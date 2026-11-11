package com.base.iso.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.packager.ISO87BPackagerRoundTripTest;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/** send(..., hexdump) / receive(hexdump): the exact wire frame lands in the log event, opt-in only. */
class HexdumpLoggingTest {

    static final String TPDU = "6000010002";
    static final String P0800 = ISO87BPackagerRoundTripTest.HEX_0800;
    static final String P0810 = ISO87BPackagerRoundTripTest.HEX_0810;

    private final ISO87BPackager p = new ISO87BPackager();
    private final List<String> events = new ArrayList<>();

    private <C extends BaseConnector> C logged(C c) {
        Logger logger = new Logger();
        logger.addListener(ev -> {
            events.add(ev.toString());
            return ev;
        });
        c.setLogger(logger, "test");
        return c;
    }

    private String only(String tag) {
        String found = null;
        for (String e : events)
            if (e.contains("\"" + tag + "\""))
                found = e;
        return found;
    }

    @Test
    void sendIsoMsgWithHexdumpLogsFullWireFrame() throws Exception {
        try (Loopback lb = new Loopback()) {
            NCCConnector c = logged(new NCCConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU)));
            c.connect();
            ISOMsg m = TestMessages.m0800(p);
            c.send(m, true);
            String expected = "0036" + TPDU + P0800;
            // wire unchanged by the capture
            assertEquals(expected, ISOUtil.hexString(lb.read(2 + 5 + P0800.length() / 2)));
            String evt = only("send");
            assertTrue(evt.contains("\"hexdump\""), evt);
            assertTrue(evt.contains("\"hex\": \"" + expected + "\""), evt);
            assertTrue(evt.contains("\"length\": " + expected.length() / 2), evt);
            assertTrue(evt.contains("0000  00 36 60 00 01 00 02"), evt);
            c.disconnect();
        }
    }

    @Test
    void sendWithoutFlagDoesNotLogRawBytes() throws Exception {
        try (Loopback lb = new Loopback()) {
            NACConnector c = logged(new NACConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU)));
            c.connect();
            c.send(TestMessages.m0800(p));
            assertEquals("0024" + TPDU + P0800, ISOUtil.hexString(lb.read(2 + 5 + P0800.length() / 2)));
            assertFalse(only("send").contains("hexdump"));
            c.disconnect();
        }
    }

    @Test
    void receiveWithHexdumpLogsFullWireFrame() throws Exception {
        try (Loopback lb = new Loopback()) {
            NACConnector c = logged(new NACConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU)));
            c.connect();
            String frame = "0026" + TPDU + P0810;
            lb.write(ISOUtil.hex2byte(frame));
            ISOMsg r = c.receive(true);
            assertEquals("0810", r.getMTI());
            assertTrue(only("receive").contains("\"hex\": \"" + frame + "\""), only("receive"));
            // next receive without the flag: stream restored, no dump
            events.clear();
            lb.write(ISOUtil.hex2byte(frame));
            assertEquals("0810", c.receive().getMTI());
            assertFalse(only("receive").contains("hexdump"));
            c.disconnect();
        }
    }

    @Test
    void receiveHexdumpIsLoggedEvenWhenUnpackFails() throws Exception {
        try (Loopback lb = new Loopback()) {
            NACConnector c = logged(new NACConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU)));
            c.connect();
            String frame = "0007" + TPDU + "0810"; // truncated: MTI only, no bitmap
            lb.write(ISOUtil.hex2byte(frame));
            try {
                c.receive(true);
            } catch (BaseException expected) {
                // unpack failure is the point
            }
            assertTrue(only("receive").contains("\"hex\": \"" + frame + "\""), only("receive"));
            c.disconnect();
        }
    }

    @Test
    void rawSendersLogFrameWhenAsked() throws Exception {
        try (Loopback lb = new Loopback()) {
            BCDConnector c = logged(new BCDConnector(lb.host(), lb.port(), p, null));
            c.connect();
            byte[] body = ISOUtil.hex2byte(P0800);
            c.sendBcd(body, ISOUtil.hex2byte(TPDU), true);
            String expected = "0036" + TPDU + P0800;
            assertEquals(expected, ISOUtil.hexString(lb.read(expected.length() / 2)));
            assertTrue(only("send").contains("\"hex\": \"" + expected + "\""), only("send"));
            c.disconnect();
        }
    }

    @Test
    void addHexdumpShape() {
        LogEvent evt = new LogEvent("x");
        BaseConnector.addHexdump(evt, ISOUtil.hex2byte("0200"));
        String s = evt.toString();
        assertTrue(s.contains("\"hex\": \"0200\""), s);
        assertTrue(s.contains("\"dump\""), s);
    }
}
