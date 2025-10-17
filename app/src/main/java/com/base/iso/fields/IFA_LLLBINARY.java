package com.base.iso.fields;

import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len BINARY
 * 
 * @see BaseComponent
 */
public class IFA_LLLBINARY extends BinaryFieldBase {
    public IFA_LLLBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_LLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLL);
        checkLength(len, 999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999);
        super.setLength(len);
    }
}
