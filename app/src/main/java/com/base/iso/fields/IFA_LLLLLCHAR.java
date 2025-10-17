package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len CHAR 
 * @see BaseComponent
 */
public class IFA_LLLLLCHAR extends StringFieldBase {
    public IFA_LLLLLCHAR () {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLLLLCHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.LLLLL);
        checkLength(len, 99999);
    }

    public void setLength(int len)
    {
        checkLength(len, 99999);
        super.setLength(len);
    }
}
