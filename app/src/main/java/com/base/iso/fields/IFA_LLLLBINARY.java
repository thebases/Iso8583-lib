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
public class IFA_LLLLBINARY extends BinaryFieldBase {
    public IFA_LLLLBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_LLLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLL);
        checkLength(len, 9999);
    }

    public void setLength(int len)
    {
        checkLength(len, 9999);
        super.setLength(len);
    }
}

