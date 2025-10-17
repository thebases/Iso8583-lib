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
public class IFA_LLLABINARY extends aISOFieldBase {
    public IFA_LLLABINARY() {
	super();
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_LLLABINARY (int len, String description) {
        super(len, description);
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
       //CJH incorrect IFA_LLLBINARY pack 08/07/04
   
        int len;
        byte[] b = (byte[]) c.getValue();
    
        if ( (len=b.length) > getLength() || len>999)
            throw new BaseException (
                "invalid len "+len 
                +" packing IFA_LLLABINARY field "+ c.getKey()
            );
        
        byte[] data = ISOUtil.hexString( (byte[]) c.getValue() ).getBytes();
        byte[] nb=new byte[ 3 +  data.length];
        byte[] length = new DecimalFormat("000").format(len).getBytes();
        System.arraycopy(length, 0, nb, 0, 3);
        System.arraycopy(data, 0, nb, 3, data.length);

        return nb;
//      CJH END
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
      //CJH incorrect IFA_LLLBINARY unpack 08/07/04
        
      int len = Integer.parseInt(new String(b, offset, 3));       
      c.setValue (ISOUtil.hex2byte(b, offset + 3, len));
      return len * 2 + 3;
        
      //CJH END
    }
    public BaseComponent createComponent(int fieldNumber) {
        return new BinaryFieldComponent (fieldNumber);
    }
    public int getMaxPackedLength() {
        return (getLength() << 1) + 3;
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        int len = Integer.parseInt(new String(readBytes (in, 3)));
        c.setValue (readBytes (in, len));
    }
}

