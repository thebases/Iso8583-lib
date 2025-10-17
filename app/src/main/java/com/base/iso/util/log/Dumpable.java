package com.base.iso.util.log;

import java.io.PrintStream;

import com.base.iso.interfaces.log.iLoggeable;
import com.base.iso.util.ISOUtil;

public class Dumpable implements iLoggeable {
    String name;
    byte[] payload;

    public Dumpable (String name, byte[] payload) {
        this.name = name;
        this.payload = payload;
    }
    public void dump(PrintStream p, String indent) {
        // p.println (indent + "<" + name + ">");
        // p.print (ISOUtil.hexdump (payload));
        // p.println (indent + "</" + name + ">");
         p.println (indent + "{\"" + name + "\": ");
        p.print (ISOUtil.hexdump (payload));
        p.println (indent + "}");
    }
}
