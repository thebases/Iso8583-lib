package com.base.iso.fields;
import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.aISOFieldBase;




/**
 * ISOFieldPackager Binary Hex Fixed LLBINARY 
 * @see BaseComponent
 */
public class IFB_LLHFBINARY extends aISOFieldBase {
    public IFB_LLHFBINARY() {
        super();
    }
    /**
    * @param len - field len
    * @param description symbolic descrption
    */
    public IFB_LLHFBINARY (int len, String description) {
    super(len, description);
    }
   /**
    * @param c - a component
    * @return packed component
    * @exception BaseException
    */
    public byte[] pack (BaseComponent c) throws BaseException {
        int len = ((byte[]) c.getValue()).length;
        if (len > getLength() || len>255) {
            throw new BaseException (
                "invalid len "+len +" packing field "+ c.getKey()
            );
        }
        byte[] b = new byte[getLength() + 1];
        b[0] = (byte) len;
        System.arraycopy(c.getValue(), 0, b, 1, len);
        return b;
    }
   /**
    * @param c - the Component to unpack
    * @param b - binary image
    * @param offset - starting offset within the binary image
    * @return consumed bytes
    * @exception BaseException
    */
    public int unpack (BaseComponent c, byte[] b, int offset) throws BaseException
    {
        int len = b[offset] & 0xFF;
        byte[] value = new byte[len];
        System.arraycopy(b, ++offset, value, 0, len);
        c.setValue (value);
        return getLength()+1;
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        byte[] b = readBytes (in, 1);
        int len = b[0] & 0xFF;
        c.setValue (readBytes (in, len));
        in.skip (getLength () - len);
    }
    public BaseComponent createComponent(int fieldNumber) {
        return new BinaryFieldComponent (fieldNumber);
    }
    public int getMaxPackedLength() {
        return getLength() + 1;
    }
}

