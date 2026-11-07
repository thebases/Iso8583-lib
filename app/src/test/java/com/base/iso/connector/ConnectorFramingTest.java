package com.base.iso.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.packager.ISO87BPackagerRoundTripTest;
import com.base.iso.util.ISOUtil;

/**
 * T1 — exact wire framing per connector ([length prefix][TPDU][packed msg]) on send(ISOMsg),
 * and receive() of a scripted frame. Uses a real loopback socket.
 */
class ConnectorFramingTest {

    static final String TPDU = "6000010002";
    /** 0800 packed = 31 bytes; with 5-byte TPDU the length prefix carries 36. */
    static final String P0800 = ISO87BPackagerRoundTripTest.HEX_0800;
    static final String P0810 = ISO87BPackagerRoundTripTest.HEX_0810;

    private final ISO87BPackager p = new ISO87BPackager();

    private static int bytes(String hex) {
        return hex.length() / 2;
    }

    /** Connects, sends the 0800 (no msg header → connector TPDU, no swap), returns the frame. */
    private String sendFrame(BaseConnector c, Loopback lb, int prefixLen) throws Exception {
        c.connect();
        ISOMsg m = TestMessages.m0800(p);
        c.send(m);
        int hdr = c.getHeader() == null ? 0 : c.getHeader().length;
        return ISOUtil.hexString(lb.read(prefixLen + hdr + bytes(P0800)));
    }

    @Test
    void nccSendsBcdLengthTpduMessage() throws Exception {
        try (Loopback lb = new Loopback()) {
            NCCConnector c = new NCCConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU));
            assertEquals("0036" + TPDU + P0800, sendFrame(c, lb, 2));
            c.disconnect();
        }
    }

    @Test
    void bcdSendsBcdLengthTpduMessage() throws Exception {
        try (Loopback lb = new Loopback()) {
            BCDConnector c = new BCDConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU));
            assertEquals("0036" + TPDU + P0800, sendFrame(c, lb, 2));
            c.disconnect();
        }
    }

    @Test
    void nacSendsBinaryLengthTpduMessage() throws Exception {
        try (Loopback lb = new Loopback()) {
            NACConnector c = new NACConnector(lb.host(), lb.port(), p, ISOUtil.hex2byte(TPDU));
            assertEquals("0024" + TPDU + P0800, sendFrame(c, lb, 2));
            c.disconnect();
        }
    }

    @Test
    void hexSendsAsciiHexLengthMessage() throws Exception {
        try (Loopback lb = new Loopback()) {
            HEXConnector c = new HEXConnector(lb.host(), lb.port(), p, null);
            // 31 = 0x1f → "001f" in ASCII
            assertEquals("30303166" + P0800, sendFrame(c, lb, 4));
            c.disconnect();
        }
    }

    @Test
    void nccReceivesScriptedFrame() throws Exception {
        assertReceives(new NCCConnector(null, 0, p, ISOUtil.hex2byte(TPDU)), "0038" + TPDU + P0810);
    }

    @Test
    void bcdReceivesScriptedFrame() throws Exception {
        assertReceives(new BCDConnector(null, 0, p, ISOUtil.hex2byte(TPDU)), "0038" + TPDU + P0810);
    }

    @Test
    void nacReceivesScriptedFrame() throws Exception {
        assertReceives(new NACConnector(null, 0, p, ISOUtil.hex2byte(TPDU)), "0026" + TPDU + P0810);
    }

    @Test
    void hexReceivesScriptedFrame() throws Exception {
        // 33 = 0x21
        assertReceives(new HEXConnector(null, 0, p, null), "30303231" + P0810);
    }

    private void assertReceives(BaseConnector c, String frameHex) throws Exception {
        try (Loopback lb = new Loopback()) {
            c.setHost(lb.host(), lb.port());
            c.connect();
            lb.write(ISOUtil.hex2byte(frameHex));
            ISOMsg r = c.receive();
            assertEquals("0810", r.getMTI());
            assertEquals("000001", r.getString(11));
            assertEquals("00", r.getString(39));
            assertEquals("00000001", r.getString(41));
            assertEquals("301", r.getString(70));
            if (c.getHeader() != null)
                assertEquals(TPDU, ISOUtil.hexString(r.getHeader()));
            c.disconnect();
        }
    }
}
