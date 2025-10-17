package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.util.ISOUtil;

/**
 * ISOFieldPackager Binary Amount 
 * @see BaseComponent
 */
public class IFB_AMOUNT extends aISOFieldBase {
    private BCDInterpreter interpreter;
    
    public IFB_AMOUNT() {
        super();
        interpreter = BCDInterpreter.LEFT_PADDED;
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_AMOUNT(int len, String description, boolean pad) {
        super(len, description);
        this.pad = pad;
        interpreter = pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED;
    }
    
    public void setPad(boolean pad)
    {
        this.pad = pad;
        interpreter = pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED;
    }

    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
        String s = (String) c.getValue();
        String amount = ISOUtil.zeropad(s.substring(1), getLength()-1);
        byte[] b   = new byte[1 + (getLength() >> 1)];
        b[0] = (byte) s.charAt(0);
        interpreter.interpret(amount, b, 1);
        return b;
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
        String d = new String(b, offset, 1)
                    + interpreter.uninterpret(b, offset + 1, getLength() - 1);
        c.setValue(d);
        return 1 + (getLength() >> 1);
    }
    public int getMaxPackedLength() {
        return 1 + (getLength() >> 1);
    }
}
