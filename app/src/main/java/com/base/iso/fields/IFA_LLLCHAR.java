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
public class IFA_LLLCHAR extends StringFieldBase {
    public IFA_LLLCHAR () {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLLCHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLL);
        checkLength(len, 999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999);
        super.setLength(len);
    }
}
