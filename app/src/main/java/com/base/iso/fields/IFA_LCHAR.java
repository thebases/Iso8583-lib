package com.base.iso.fields;

import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len CHAR.
 * No padding, ASCII Interpreter, Single digit ASCII length Prefixer.
 * 
 */
public class IFA_LCHAR extends StringFieldBase {
    public IFA_LCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.L);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_LCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, AsciiPrefixer.L);
        checkLength(len, 9);
    }

    public void setLength(int len)
    {
        checkLength(len, 9);
        super.setLength(len);
    }
}
