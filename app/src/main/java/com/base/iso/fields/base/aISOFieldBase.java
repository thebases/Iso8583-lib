package com.base.iso.fields.base;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOField;

/**
 * base class for the various IF*.java Field Packagers
 * Implements "FlyWeight" pattern
 */
public abstract class aISOFieldBase {
    private int len;
    private String description;
    protected boolean pad;

    /**
     * Default Constructor
     */
    public aISOFieldBase()
    {
        this.len = -1;
        this.description = null;
    }

    /**
     * @param len - field Len
     * @param description - details
     */
    public aISOFieldBase(int len, String description) {
        this.len = len;
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getLength() {
        return len;
    }
    public void setLength(int len) {
        this.len = len;
    }

    public void setPad(boolean pad) {
        this.pad = pad;
    }

    public boolean getPad() {
        return pad;
    }

    public abstract int getMaxPackedLength();

    public BaseComponent createComponent(int fieldNumber) {
        return new ISOField (fieldNumber);
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public abstract byte[] pack (BaseComponent c) throws BaseException;

    /**
     * @param c - the Component to unpack
     * @param b - binary image
     * @param offset - starting offset within the binary image
     * @return consumed bytes
     * @exception BaseException
     */
    public abstract int unpack (BaseComponent c, byte[] b, int offset)
        throws BaseException;

    /**
     * @param c  - the Component to unpack
     * @param in - input stream
     * @exception BaseException
     */
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        unpack (c, readBytes (in, getMaxPackedLength ()), 0);
    }
    /**
     * @param c   - the Component to unpack
     * @param out - output stream
     * @exception BaseException
     * @exception IOException
     */
    public void pack (BaseComponent c, ObjectOutput out) 
        throws IOException, BaseException
    {
        out.write (pack (c));
    }

    protected byte[] readBytes (InputStream in, int l) throws IOException {
        byte[] b = new byte [l];
        int n = 0;
        while (n < l) {
            int count = in.read(b, n, l - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
        return b;
    }
}

