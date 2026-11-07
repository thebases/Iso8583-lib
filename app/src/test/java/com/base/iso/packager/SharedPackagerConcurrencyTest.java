package com.base.iso.packager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.base.iso.TestMessages;
import com.base.iso.core.ISOMsg;
import com.base.iso.util.ISOUtil;

/**
 * T1/T13 — one configured packager shared by many threads (JVM service usage) must produce the
 * single-thread golden output for every pack and unpack. Each thread uses its own ISOMsg.
 */
class SharedPackagerConcurrencyTest {

    private static final int THREADS = 8;
    private static final int ITERATIONS = 500;

    @Test
    void sharedIso87bPackagerIsDeterministic() throws Exception {
        assertDeterministic(new ISO87BPackager());
    }

    @Test
    void sharedJsonPackagerIsDeterministic() throws Exception {
        GenericJSONPackager g = new GenericJSONPackager();
        g.setFieldsFromJson(TestMessages.iso87bLayoutJson(false));
        assertDeterministic(g);
    }

    private static void assertDeterministic(aBasePackager p) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                results.add(pool.submit((Callable<Integer>) () -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        byte[] packed = TestMessages.m0200(p).pack();
                        assertEquals(ISO87BPackagerRoundTripTest.HEX_0200, ISOUtil.hexString(packed));
                        ISOMsg back = new ISOMsg();
                        back.setPackager(p);
                        back.unpack(packed);
                        assertEquals(TestMessages.TRACK2, back.getString(35));
                        assertEquals(TestMessages.PIN_BLOCK, ISOUtil.hexString(back.getBytes(52)));
                    }
                    return ITERATIONS;
                }));
            }
            for (Future<Integer> f : results)
                assertEquals(ITERATIONS, f.get(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
