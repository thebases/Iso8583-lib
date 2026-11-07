package com.base.iso.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.ISOUtil;

/** T7, T8, T9 + host fallback — transport fixes, each pinned by a test that failed on 402fc3e. */
class ConnectorFixesTest {

    private static final String TPDU = "6000010002";
    private static final String SWAPPED = "6000020001";
    private final ISO87BPackager p = new ISO87BPackager();

    // --- T7: resending the same ISOMsg must not flip its TPDU ---

    @Test
    void nccResendKeepsSwappedTpdu() throws Exception {
        assertResendStable(new NCCConnector(null, 0, p, null));
    }

    @Test
    void bcdResendKeepsSwappedTpdu() throws Exception {
        assertResendStable(new BCDConnector(null, 0, p, null));
    }

    @Test
    void nacResendKeepsSwappedTpdu() throws Exception {
        assertResendStable(new NACConnector(null, 0, p, null));
    }

    private void assertResendStable(BaseConnector c) throws Exception {
        try (Loopback lb = new Loopback()) {
            c.setHost(lb.host(), lb.port());
            c.connect();
            ISOMsg m = TestMessages.m0800(p);
            m.setHeader(ISOUtil.hex2byte(TPDU)); // request header as received from the terminal side
            int frameLen = 2 + 5 + ConnectorFramingTest.P0800.length() / 2;

            c.send(m);
            String first = ISOUtil.hexString(lb.read(frameLen));
            c.send(m); // retry / reversal re-send of the same object
            String second = ISOUtil.hexString(lb.read(frameLen));

            assertEquals(SWAPPED, first.substring(4, 14));
            assertEquals(first, second);
            assertEquals(TPDU, ISOUtil.hexString(m.getHeader()), "caller's header must not be mutated");
            c.disconnect();
        }
    }

    // --- T8: a refused connection must surface as an exception ---

    @Test
    void connectToClosedPortThrows() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = s.getLocalPort();
        } // now closed → connection refused
        NCCConnector c = new NCCConnector(InetAddress.getLoopbackAddress().getHostAddress(), port, p, null);
        assertThrows(IOException.class, c::connect);
        assertFalse(c.isConnected());
    }

    // --- T9: corrupt length prefix closes the socket ---

    @Test
    void nccCorruptBcdLengthClosesSocket() throws Exception {
        assertCorruptLengthCloses(new NCCConnector(null, 0, p, null), "0ABC");
    }

    @Test
    void bcdCorruptBcdLengthClosesSocket() throws Exception {
        assertCorruptLengthCloses(new BCDConnector(null, 0, p, null), "0ABC");
    }

    @Test
    void hexCorruptAsciiLengthClosesSocket() throws Exception {
        assertCorruptLengthCloses(new HEXConnector(null, 0, p, null), "5A5A5A5A"); // "ZZZZ"
    }

    private void assertCorruptLengthCloses(BaseConnector c, String prefixHex) throws Exception {
        try (Loopback lb = new Loopback()) {
            c.setHost(lb.host(), lb.port());
            c.connect();
            lb.write(ISOUtil.hex2byte(prefixHex));
            BaseException e = assertThrows(BaseException.class, c::receive);
            assertTrue(e.getMessage().contains("invalid length prefix"), e.getMessage());
            assertFalse(c.isConnected());
        }
    }

    // --- setHosts: fallback hosts replace the removed "alternate-host" config keys ---

    @Test
    void connectFallsBackToNextHost() throws Exception {
        int deadPort;
        try (ServerSocket tmp = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            deadPort = tmp.getLocalPort(); // closed below → connection refused
        }
        try (ServerSocket live = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            NCCConnector c = new NCCConnector(null, 0, p, null);
            c.setHosts(new String[] { "127.0.0.1", "127.0.0.1" },
                       new int[] { deadPort, live.getLocalPort() });
            c.setConnectTimeout(2000);
            c.connect();
            try {
                assertTrue(c.isConnected());
                assertEquals(live.getLocalPort(), c.getSocket().getPort());
                assertEquals(deadPort, c.getPort()); // primary stays hosts[0]
            } finally {
                c.disconnect();
            }
        }
    }

    @Test
    void setHostsRejectsMismatchedArrays() {
        NCCConnector c = new NCCConnector();
        assertThrows(IllegalArgumentException.class,
                () -> c.setHosts(new String[] { "a", "b" }, new int[] { 1 }));
        assertThrows(IllegalArgumentException.class,
                () -> c.setHosts(new String[0], new int[0]));
    }
}
