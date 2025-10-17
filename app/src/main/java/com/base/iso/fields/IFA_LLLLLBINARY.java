package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len BINARY
 * 
 * @see BaseComponent
 */
public class IFA_LLLLLBINARY extends BinaryFieldBase {
    public IFA_LLLLLBINARY () {
        super(LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLLLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLLL);
        checkLength(len, 99999);
    }

    public void setLength(int len)
    {
        checkLength(len, 99999);
        super.setLength(len);
    }
}
