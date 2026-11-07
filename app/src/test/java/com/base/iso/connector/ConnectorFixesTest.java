package com.base.iso.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.ISOUtil;

/** T7, T8, T9, T18 — transport fixes, each pinned by a test that failed on 402fc3e. */
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

    // --- T18: channel registry is safe under concurrent setName ---

    @Test
    void concurrentSetNameLosesNothing() throws Exception {
        final int threads = 8, perThread = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<List<NCCConnector>>> fs = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int id = t;
                fs.add(pool.submit(() -> {
                    List<NCCConnector> mine = new ArrayList<>();
                    for (int i = 0; i < perThread; i++) {
                        NCCConnector c = new NCCConnector();
                        c.setName("t18-" + id + "-" + i);
                        mine.add(c);
                    }
                    return mine;
                }));
            }
            for (int t = 0; t < threads; t++) {
                List<NCCConnector> mine = fs.get(t).get(60, TimeUnit.SECONDS);
                for (int i = 0; i < perThread; i++)
                    assertSame(mine.get(i), BaseConnector.getChannel("t18-" + t + "-" + i));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void renameAndUnregisterRemoveOldEntry() {
        NCCConnector c = new NCCConnector();
        c.setName("t18-old");
        c.setName("t18-new");
        assertEquals(null, BaseConnector.getChannel("t18-old"));
        assertSame(c, BaseConnector.getChannel("t18-new"));
        c.unregister();
        assertEquals(null, BaseConnector.getChannel("t18-new"));
    }
}
