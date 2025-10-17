package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len CHAR
 * 
 * @see BaseComponent
 */
public class IFA_LLLLCHAR extends StringFieldBase {
    public IFA_LLLLCHAR () {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLLLCHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLLL);
        checkLength(len, 9999);
    }

    public void setLength(int len)
    {
        checkLength(len, 9999);
        super.setLength(len);
    }
}
