package com.base.iso.fields;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * ISOFieldPackager ASCII variable len BINARY
 * 
 * @see BaseComponent
 */
public class IFA_LLABINARY extends aISOFieldBase {
    public IFA_LLABINARY() {
	super();
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_LLABINARY (int len, String description) {
        super(len, description);
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
        int len;
        byte[] b = (byte[]) c.getValue();
    
        if ( (len=b.length) > getLength() || len>99)
            throw new BaseException (
                "invalid len "+len 
                +" packing field "+ c.getKey()
            );
        //CJH incorrect IFA_LLBINARY pack 08/07/04
        byte[] data = ISOUtil.hexString( (byte[]) c.getValue() ).getBytes();
        byte[] nb=new byte[ 2 +  data.length];
        
        byte[] length = new DecimalFormat("00").format(len).getBytes();
        System.arraycopy(length, 0, nb, 0, 2);
        System.arraycopy(data, 0, nb, 2, data.length);
        return nb;
        //CJH END.
    }
    /**
     * @param c - the Component to unpack
     * @param b - binary image
     * @param offset - starting offset within the binary image
     * @return consumed bytes
     * @exception BaseException
     */
    public int unpack (BaseComponent c, byte[] b, int offset)
        throws BaseException
    {
      //CJH incorrect IFA_LLBINARY unpack 08/07/04
         
      int len = Integer.parseInt(new String(b, offset, 2));       
      c.setValue (ISOUtil.hex2byte(b, offset + 2, len));
      return len * 2 + 2;

      //CJH END.
        
        
    }
    public BaseComponent createComponent(int fieldNumber) {
        return new BinaryFieldComponent (fieldNumber);
    }
    public int getMaxPackedLength() {
        return (getLength() << 1) + 2;
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        int len = Integer.parseInt(new String(readBytes (in, 2)));
        c.setValue (readBytes (in, len));
    }
}

