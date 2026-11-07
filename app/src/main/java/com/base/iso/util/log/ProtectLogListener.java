package com.base.iso.util.log;

import java.util.List;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOField;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.log.iLogListener;
import com.base.iso.util.ISOUtil;

/**
 * Masks card data in log events before any later listener writes them (PCI DSS req. 3, plan T12).
 * Add it to the {@link Logger} FIRST, before {@link SimpleLogListener} or any shipping listener:
 * <pre>
 *   logger.addListener(new ProtectLogListener());
 *   logger.addListener(new SimpleLogListener(System.out));
 * </pre>
 * Every {@link ISOMsg} in the event payload is replaced by a masked clone (the caller's message
 * is never modified):
 * <ul>
 *   <li>protected fields (default 2, 35, 45) → {@link ISOUtil#protect} (first 6 / last 4 kept);</li>
 *   <li>wiped fields (default 52, 55) → "[WIPED]";</li>
 *   <li>raw {@code byte[]} payload items → "[n bytes]".</li>
 * </ul>
 * Masking applies to top-level field numbers; sub-fields of composite fields are not inspected.
 * Stateless after construction → safe to share across threads.
 */
public class ProtectLogListener implements iLogListener {

    /** PAN, track 2, track 1. */
    public static final int[] DEFAULT_PROTECT = { 2, 35, 45 };
    /** PIN block, ICC/EMV data. */
    public static final int[] DEFAULT_WIPE = { 52, 55 };

    private static final String WIPED = "[WIPED]";

    private final int[] protectFields;
    private final int[] wipeFields;

    public ProtectLogListener() {
        this(DEFAULT_PROTECT, DEFAULT_WIPE);
    }

    /**
     * @param protectFields fields shown as {@code ISOUtil.protect(value)}
     * @param wipeFields    fields replaced by "[WIPED]"
     */
    public ProtectLogListener(int[] protectFields, int[] wipeFields) {
        this.protectFields = protectFields.clone();
        this.wipeFields = wipeFields.clone();
    }

    @Override
    public LogEvent log(LogEvent ev) {
        List<Object> payload = ev.getPayLoad();
        synchronized (payload) {
            for (int i = 0; i < payload.size(); i++) {
                Object o = payload.get(i);
                if (o instanceof ISOMsg)
                    payload.set(i, mask((ISOMsg) o));
                else if (o instanceof byte[])
                    payload.set(i, "[" + ((byte[]) o).length + " bytes]");
            }
        }
        return ev;
    }

    /** @return a masked clone; {@code m} itself is untouched */
    public ISOMsg mask(ISOMsg m) {
        ISOMsg c = (ISOMsg) m.clone();
        try {
            for (int f : protectFields) {
                if (!c.hasField(f))
                    continue;
                Object v = c.getValue(f);
                // ISOField set directly: set(int,String) would hex-parse binary fields.
                c.set(new ISOField(f, v instanceof String ? ISOUtil.protect((String) v) : WIPED));
            }
            for (int f : wipeFields) {
                if (c.hasField(f))
                    c.set(new ISOField(f, WIPED));
            }
        } catch (BaseException e) {
            // Masking failed — never fall back to the clear message.
            ISOMsg safe = new ISOMsg();
            try {
                safe.setMTI(c.getMTI());
            } catch (BaseException ignored) {
                // no MTI available
            }
            return safe;
        }
        return c;
    }
}
