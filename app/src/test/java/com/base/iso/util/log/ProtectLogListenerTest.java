package com.base.iso.util.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.ISOUtil;

/** T12 — no cleartext PAN / track 2 / PIN block / ICC data reaches a log listener. */
class ProtectLogListenerTest {

    private static final String PAN_MIDDLE = "111111"; // digits 7..12 of the test PAN

    private static String logged(boolean protect, Runnable work, ISO87BPackager p) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Logger logger = new Logger();
        if (protect)
            logger.addListener(new ProtectLogListener());
        logger.addListener(new SimpleLogListener(new PrintStream(out, true)));
        p.setLogger(logger, "test");
        work.run();
        return out.toString();
    }

    private static void assertNoCardData(String log) {
        assertFalse(log.contains(TestMessages.PAN), "PAN leaked");
        assertFalse(log.contains("41111111" + PAN_MIDDLE), "PAN prefix leaked");
        assertFalse(log.contains(TestMessages.PIN_BLOCK), "PIN block leaked");
        assertFalse(log.contains(TestMessages.ICC), "ICC data leaked");
        assertFalse(log.contains("2512101123456789"), "track 2 discretionary data leaked");
    }

    @Test
    void messageInEventIsMaskedAndOriginalUntouched() throws Exception {
        ISO87BPackager p = new ISO87BPackager();
        ISOMsg m = TestMessages.m0200(p);
        String log = logged(true, () -> {
            LogEvent evt = new LogEvent(p, "send");
            evt.addMessage(m);
            evt.addMessage(new byte[] { 1, 2, 3 });
            Logger.log(evt);
        }, p);
        assertNoCardData(log);
        assertTrue(log.contains("411111______1111"), log);
        assertTrue(log.contains("[WIPED]"), log);
        assertTrue(log.contains("[3 bytes]"), log);
        // caller's message unchanged
        assertEquals(TestMessages.PAN, m.getString(2));
        assertEquals(TestMessages.PIN_BLOCK, ISOUtil.hexString(m.getBytes(52)));
    }

    @Test
    void packagerDoesNotLogValuesByDefault() throws Exception {
        ISO87BPackager p = new ISO87BPackager();
        byte[] packed = TestMessages.m0200(p).pack();
        // Even without ProtectLogListener the packager itself must not emit card data.
        String log = logged(false, () -> {
            try {
                TestMessages.m0200(p).pack();
                ISOMsg back = new ISOMsg();
                back.setPackager(p);
                back.unpack(packed);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, p);
        assertNoCardData(log);
        assertFalse(log.contains(ISOUtil.hexString(packed)), "raw image leaked");
        assertTrue(log.contains("IFB_BINARY"), "field/codec metadata is still logged");
    }

    @Test
    void packagerLogsValuesOnlyInExplicitDebugMode() throws Exception {
        ISO87BPackager p = new ISO87BPackager();
        p.setLogFieldValues(true);
        byte[] packed = TestMessages.m0200(p).pack();
        String log = logged(false, () -> {
            try {
                ISOMsg back = new ISOMsg();
                back.setPackager(p);
                back.unpack(packed);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, p);
        assertTrue(log.contains(TestMessages.PAN));
    }
}
