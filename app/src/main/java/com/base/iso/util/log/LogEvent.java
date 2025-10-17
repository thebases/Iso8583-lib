package com.base.iso.util.log;

/*
 * This LogEvent have been re-write to use for Android 10+
 * 
 */
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;

import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.interfaces.log.iLoggeable;

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

    public void addMessage(String tagname, String message) {
        // payLoad.add("<" + tagname + ">" + message + "</" + tagname + ">");
         payLoad.add("{\"" + tagname + "\":" + message + "}\n");
    }

    public iLogSource getSource() {
        return source;
    }

    public void setSource(iLogSource source) {
        this.source = source;
    }

    protected String dumpHeader(PrintStream p, String indent) {
        if (dumpedAt == 0L)
            dumpedAt = System.currentTimeMillis();
        Date date = new Date(dumpedAt);
        StringBuilder sb = new StringBuilder(indent);
        sb.append("{\"log-realm\":\"");
        // sb.append("<log realm=\"");
        sb.append(getRealm());
        sb.append("\", \n \"at\":\"");
        sb.append(date.toString());
        sb.append('.');
        sb.append(Long.toString(dumpedAt % 1000));
        sb.append('"');
        if (dumpedAt != createdAt) {
            sb.append(", \n \"lifespan\":\"");
            sb.append(Long.toString(dumpedAt - createdAt));
            sb.append("ms\"");
        }
        sb.append(",");

        // sb.append('>');
        p.println(sb.toString());
        return indent + "  ";
    }

    protected void dumpTrailer(PrintStream p, String indent) {
        p.println(indent + "}");
    }

    public void dump(PrintStream p, String outer) {
        String indent = dumpHeader(p, outer);
        String newIndent = (tag != null) ? indent + "  " : "";

        if (payLoad.isEmpty()) {
            if (tag != null)
                p.println(indent + "\"" + tag + "\": ");
        } else {
            if (tag != null)
                p.println(indent + "\"" + tag + "\": ");
            synchronized (payLoad) {
                for (Object o : payLoad) {
                    if (o instanceof iLoggeable) {
                        ((iLoggeable) o).dump(p, newIndent);
                    } else if (o instanceof SQLException) {
                        SQLException e = (SQLException) o;
                        p.println(newIndent + "{\"SQLException\": " + e.getMessage() + "}");
                        p.println(newIndent + "{\"SQLState\"" + e.getSQLState() + "}");
                        p.println(newIndent + "{\"VendorError\"" + e.getErrorCode() + "}");
                        e.printStackTrace(p);
                    } else if (o instanceof Throwable) {
                        Throwable t = (Throwable) o;
                        p.println(newIndent + "{\"exception\": \"name\":\"" + t.getClass().getName() + "\"}");
                        p.println(newIndent + escapeXml(t.getMessage()));
                        t.printStackTrace(p);
                        p.println(newIndent + "}");
                    } else if (o instanceof Object[]) {
                        p.print(newIndent + "[");
                        Object[] array = (Object[]) o;
                        for (int i = 0; i < array.length; i++) {
                            if (i > 0) p.print(", ");
                            p.print(array[i]);
                        }
                        p.println("]");
                    } else if (o instanceof Element) {
                        try {
                            TransformerFactory tf = TransformerFactory.newInstance();
                            Transformer transformer = tf.newTransformer();
                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                            DOMSource source = new DOMSource((Element) o);
                            StreamResult result = new StreamResult(p);
                            transformer.transform(source, result);
                        } catch (Exception e) {
                            p.println(newIndent + "<error>Failed to transform XML Element</error>");
                            e.printStackTrace(p);
                        }
                    } else if (o != null) {
                        p.println(newIndent + escapeXml(o.toString()));
                    } else {
                        p.println(newIndent + "null");
                    }
                }
            }
            if (tag != null)
                p.println(indent + "");
        }
        dumpTrailer(p, outer);
    }

    // public void dump(PrintStream p, String outer) {
    //     String indent = dumpHeader(p, outer);
    //     String newIndent = (tag != null) ? indent + "  " : "";

    //     if (payLoad.isEmpty()) {
    //         if (tag != null)
    //             p.println(indent + "<" + tag + "/>");
    //     } else {
    //         if (tag != null)
    //             p.println(indent + "<" + tag + ">");
    //         synchronized (payLoad) {
    //             for (Object o : payLoad) {
    //                 if (o instanceof iLoggeable) {
    //                     ((iLoggeable) o).dump(p, newIndent);
    //                 } else if (o instanceof SQLException) {
    //                     SQLException e = (SQLException) o;
    //                     p.println(newIndent + "<SQLException>" + e.getMessage() + "</SQLException>");
    //                     p.println(newIndent + "<SQLState>" + e.getSQLState() + "</SQLState>");
    //                     p.println(newIndent + "<VendorError>" + e.getErrorCode() + "</VendorError>");
    //                     e.printStackTrace(p);
    //                 } else if (o instanceof Throwable) {
    //                     Throwable t = (Throwable) o;
    //                     p.println(newIndent + "<exception name=\"" + t.getClass().getName() + "\">");
    //                     p.println(newIndent + escapeXml(t.getMessage()));
    //                     t.printStackTrace(p);
    //                     p.println(newIndent + "</exception>");
    //                 } else if (o instanceof Object[]) {
    //                     p.print(newIndent + "[");
    //                     Object[] array = (Object[]) o;
    //                     for (int i = 0; i < array.length; i++) {
    //                         if (i > 0) p.print(", ");
    //                         p.print(array[i]);
    //                     }
    //                     p.println("]");
    //                 } else if (o instanceof Element) {
    //                     try {
    //                         TransformerFactory tf = TransformerFactory.newInstance();
    //                         Transformer transformer = tf.newTransformer();
    //                         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    //                         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

    //                         DOMSource source = new DOMSource((Element) o);
    //                         StreamResult result = new StreamResult(p);
    //                         transformer.transform(source, result);
    //                     } catch (Exception e) {
    //                         p.println(newIndent + "<error>Failed to transform XML Element</error>");
    //                         e.printStackTrace(p);
    //                     }
    //                 } else if (o != null) {
    //                     p.println(newIndent + escapeXml(o.toString()));
    //                 } else {
    //                     p.println(newIndent + "null");
    //                 }
    //             }
    //         }
    //         if (tag != null)
    //             p.println(indent + "</" + tag + ">");
    //     }
    //     dumpTrailer(p, outer);
    // }

    public String getRealm() {
        return source != null ? source.getRealm() : "";
    }

    public List<Object> getPayLoad() {
        return payLoad;
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(baos);
        dump(p, "");
        return baos.toString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
