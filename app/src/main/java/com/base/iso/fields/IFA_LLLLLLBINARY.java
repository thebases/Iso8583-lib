package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len BINARY 
 * @see BaseComponent
 */
public class IFA_LLLLLLBINARY extends BinaryFieldBase {
    public IFA_LLLLLLBINARY () {
        super(LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLLLLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.LLLLLL);
        checkLength(len, 999999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999999);
        super.setLength(len);
    }
}

