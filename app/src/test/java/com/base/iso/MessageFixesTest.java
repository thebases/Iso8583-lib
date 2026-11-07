package com.base.iso;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.GenericJSONPackager;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.ISOUtil;

/** T5, T6, T18 — message-level fixes, each pinned by a test that failed on 402fc3e. */
class MessageFixesTest {

    private final ISO87BPackager p = new ISO87BPackager();

    // --- T5: unpack error keeps the field number and the real reason (no NPE) ---

    @Test
    void unpackErrorWithNestedCauseNamesField() throws Exception {
        // 0200 with only F2 present; F2 LLNUM length byte 0x99 = 99 > max 19 (codec wraps a cause).
        byte[] frame = ISOUtil.hex2byte("0200" + "4000000000000000" + "99" + "4111111111111111");
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        BaseException e = assertThrows(BaseException.class, () -> m.unpack(frame));
        assertTrue(e.getMessage().contains("field=2"), e.getMessage());
    }

    @Test
    void unpackErrorWithoutNestedCauseNamesFieldAndReason() throws Exception {
        // Layout defines MTI, bitmap, F2 and F4 (slot 3 empty); the frame has bit 3 set → "field packager '3' is null",
        // a BaseException with no nested cause (previously an NPE → "null consumed=N").
        GenericJSONPackager sparse = new GenericJSONPackager();
        sparse.setFieldsFromJson("{\"isofields\":["
                + "{\"id\":0,\"length\":4,\"class\":\"IFB_NUMERIC\",\"pad\":true},"
                + "{\"id\":1,\"length\":16,\"class\":\"IFB_BITMAP\"},"
                + "{\"id\":2,\"length\":19,\"class\":\"IFB_LLNUM\"},"
                + "{\"id\":4,\"length\":12,\"class\":\"IFB_NUMERIC\",\"pad\":true}]}");
        byte[] frame = ISOUtil.hex2byte("0200" + "2000000000000000" + "000000");
        ISOMsg m = new ISOMsg();
        m.setPackager(sparse);
        BaseException e = assertThrows(BaseException.class, () -> m.unpack(frame));
        assertTrue(e.getMessage().contains("field=3"), e.getMessage());
        assertTrue(e.getMessage().contains("field packager '3' is null"), e.getMessage());
    }

    // --- T6: strict hex for binary fields ---

    @Test
    void hex2byteRejectsNonHex() {
        assertThrows(IllegalArgumentException.class, () -> ISOUtil.hex2byte("ZZ"));
        assertThrows(IllegalArgumentException.class, () -> ISOUtil.hex2byte("01 2"));
    }

    @Test
    void hex2byteOddLengthIsLeftPadded() {
        assertArrayEquals(new byte[] { 0x0A, (byte) 0xBC }, ISOUtil.hex2byte("ABC"));
        assertArrayEquals(new byte[] { 0x0A, (byte) 0xBC }, ISOUtil.hex2byte("abc"));
    }

    @Test
    void setBinaryFieldWithNonHexThrows() throws Exception {
        ISOMsg m = new ISOMsg();
        m.setPackager(p);
        BaseException e = assertThrows(BaseException.class, () -> m.set(52, "ZZZZZZZZZZZZZZZZ"));
        assertTrue(e.getMessage().contains("52"), e.getMessage());
        assertTrue(!m.hasField(52));
        m.set(52, "ABC");
        assertArrayEquals(new byte[] { 0x0A, (byte) 0xBC }, m.getBytes(52));
    }

    // --- T18: readPackager must not instantiate non-iPackager classes ---

    /** Loaded only via Class.forName; instantiation is recorded. */
    public static class Gadget {
        static final AtomicInteger CREATED = new AtomicInteger();

        public Gadget() {
            CREATED.incrementAndGet();
        }
    }

    /** Exposes the protected deserialization hook. */
    static class ProbeMsg extends ISOMsg {
        void readPackagerFrom(String className) throws Exception {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (ObjectOutputStream oo = new ObjectOutputStream(bo)) {
                byte[] b = className.getBytes("US-ASCII");
                oo.writeShort(b.length);
                oo.write(b);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()))) {
                readPackager(in);
            }
        }
    }

    @Test
    void readPackagerIgnoresNonPackagerClass() throws Exception {
        ProbeMsg m = new ProbeMsg();
        m.setPackager(p);
        Gadget.CREATED.set(0);
        m.readPackagerFrom(Gadget.class.getName());
        assertEquals(0, Gadget.CREATED.get());
        assertNull(m.getPackager());
    }

    @Test
    void readPackagerStillRestoresRealPackager() throws Exception {
        ProbeMsg m = new ProbeMsg();
        m.readPackagerFrom(ISO87BPackager.class.getName());
        assertTrue(m.getPackager() instanceof ISO87BPackager);
    }
}
