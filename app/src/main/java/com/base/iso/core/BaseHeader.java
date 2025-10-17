package com.base.iso.core;
import java.io.PrintStream;

import com.base.iso.interfaces.log.iLoggeable;
import com.base.iso.util.ISOUtil;
public class BaseHeader implements ISOHeader, iLoggeable {
    /**
     * 
     */
    private static final long serialVersionUID = 8674535007934468935L;
    protected byte[] header;
    transient boolean asciiEncoding = false;

    /**
     * Default Constructor.
     * Used by Class.forName.newInstance(...);
     */
    public BaseHeader()
    {
        header = null;
    }

    public BaseHeader (byte[] header) {
        unpack(header);
    }

    public Object clone()
    {
        try {
            BaseHeader h = (BaseHeader) super.clone();
            if (this.header != null)
                h.header = this.header.clone();
            return h;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    public byte[] pack() {
        return header;
    }

    public int unpack (byte[] header) {
        this.header = header;
        return header != null ? header.length : 0;
    }

    public int getLength () {
        return header != null ? header.length : 0;
    }

    public void setDestination(String dst) {}
    public void setSource(String src) {}
    public String getDestination() { return null; }
    public String getSource() { return null; }
    public void swapDirection() {}

    public void dump (PrintStream p, String indent) {
        if (header != null) {
            p.println (
                indent
              + "<header>" + ISOUtil.hexString (header) + "</header>"
            );
        }
    }
    public void setAsciiEncoding(boolean asciiEncoding) {
        this.asciiEncoding = asciiEncoding;
    }
    public boolean isAsciiEncoding() {
        return asciiEncoding;
    }
}
