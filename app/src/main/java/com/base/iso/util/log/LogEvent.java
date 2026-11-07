package com.base.iso.util.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.interfaces.log.iLoggeable;
import com.base.iso.util.json.Json;
import com.base.iso.util.xml.XmlElement;

/**
 * One log entry: a realm, a tag and a payload list.
 *
 * <p>{@link #dump} writes the event as one valid JSON object:
 * <pre>
 * {
 *   "log-realm": "host-a/10.0.0.1:8000",
 *   "at": "2026-11-07T15:20:52.534+0700",
 *   "lifespan-ms": 6,
 *   "send": { "isomsg": { ... } }
 * }
 * </pre>
 * The payload is written under the tag: a single item as-is, several items as an array.
 * Raw {@code byte[]} items are written as {@code "[n bytes]"}, never as data.
 */
public class LogEvent {
    private iLogSource source;
    private String tag;
    private final List<Object> payLoad;
    private long createdAt;
    private long dumpedAt;

    public LogEvent(String tag) {
        this.tag = tag;
        this.createdAt = System.currentTimeMillis();
        this.payLoad = Collections.synchronizedList(new ArrayList<>());
    }

    public LogEvent() {
        this("info");
    }

    public LogEvent(String tag, Object msg) {
        this(tag);
        addMessage(msg);
    }

    public LogEvent(iLogSource source, String tag) {
        this(tag);
        this.source = source;
    }

    public LogEvent(iLogSource source, String tag, Object msg) {
        this(tag);
        this.source = source;
        addMessage(msg);
    }

    public String getTag() {
        return tag;
    }

    public void addMessage(Object msg) {
        payLoad.add(msg);
    }

    /** Adds {@code {"tagname": message}} to the payload. */
    public void addMessage(String tagname, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(tagname, message);
        payLoad.add(entry);
    }

    public iLogSource getSource() {
        return source;
    }

    public void setSource(iLogSource source) {
        this.source = source;
    }

    /** Writes this event as an indented JSON object; every line is prefixed with {@code indent}. */
    public void dump(PrintStream p, String indent) {
        if (dumpedAt == 0L)
            dumpedAt = System.currentTimeMillis();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("log-realm", getRealm());
        // SimpleDateFormat is not thread-safe: one instance per call.
        event.put("at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT).format(new Date(dumpedAt)));
        if (dumpedAt != createdAt)
            event.put("lifespan-ms", dumpedAt - createdAt);

        List<Object> items = new ArrayList<>();
        synchronized (payLoad) {
            for (Object o : payLoad)
                items.add(toJsonValue(o));
        }
        Object body = items.isEmpty() ? null : items.size() == 1 ? items.get(0) : items;
        event.put(tag != null ? tag : "info", body);

        for (String line : Json.writePretty(event).split("\n"))
            p.println(indent + line);
    }

    /** Converts a payload item into a value {@link Json#write} accepts. */
    private static Object toJsonValue(Object o) {
        if (o == null || o instanceof String || o instanceof Boolean
                || o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte
                || o instanceof Double || o instanceof Float || o instanceof BigDecimal || o instanceof BigInteger)
            return o;
        if (o instanceof byte[])
            return "[" + ((byte[]) o).length + " bytes]"; // never log raw bytes: may be card data
        if (o instanceof Throwable)
            return throwable((Throwable) o);
        if (o instanceof iLoggeable)
            return loggeable((iLoggeable) o);
        if (o instanceof Map) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet())
                m.put(String.valueOf(e.getKey()), toJsonValue(e.getValue()));
            return m;
        }
        if (o instanceof Collection || o instanceof Object[]) {
            Iterable<?> it = o instanceof Collection ? (Collection<?>) o : Arrays.asList((Object[]) o);
            List<Object> l = new ArrayList<>();
            for (Object x : it)
                l.add(toJsonValue(x));
            return l;
        }
        if (o instanceof XmlElement)
            return o.toString();
        return o.toString();
    }

    private static Map<String, Object> throwable(Throwable t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("exception", t.getClass().getName());
        m.put("message", t.getMessage());
        List<String> stack = new ArrayList<>();
        for (String line : capture(t::printStackTrace).split("\\r?\n")) {
            String s = line.trim();
            if (!s.isEmpty())
                stack.add(s);
        }
        m.put("stack", stack);
        return m;
    }

    /** Embeds the component's own dump: as JSON when it is valid JSON, else as text. */
    private static Object loggeable(iLoggeable l) {
        String text = capture(ps -> l.dump(ps, "")).trim();
        try {
            return Json.parse(text);
        } catch (IllegalArgumentException notJson) {
            return text;
        }
    }

    private interface Writer {
        void write(PrintStream ps);
    }

    private static String capture(Writer w) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        w.write(ps);
        ps.flush();
        return baos.toString();
    }

    public String getRealm() {
        return source != null ? source.getRealm() : "";
    }

    public List<Object> getPayLoad() {
        return payLoad;
    }

    public String toString() {
        return capture(ps -> dump(ps, ""));
    }
}
