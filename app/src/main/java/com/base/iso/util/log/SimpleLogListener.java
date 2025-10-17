
package com.base.iso.util.log;

import java.io.PrintStream;

import com.base.iso.interfaces.log.iLogListener;


public class SimpleLogListener implements iLogListener {
    PrintStream p;

    public SimpleLogListener () {
        super();
        p = System.out;
    }
    public SimpleLogListener (PrintStream p) {
        this ();
        setPrintStream (p);
    }
    public synchronized void setPrintStream (PrintStream p) {
        this.p = p;
    }
    public synchronized void close() {
        if (p != null) {
            p.close();
            p = null;
        }
    }
    public synchronized LogEvent log (LogEvent ev) {
        if (p != null) {
            ev.dump (p, "");
            p.flush();
        }
        return ev;
    }
}

