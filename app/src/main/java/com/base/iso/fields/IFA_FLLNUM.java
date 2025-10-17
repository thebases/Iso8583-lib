package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.FilledStringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.RightPadder;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len padded (fixed) NUMERIC
 * (suitable to use in ANSI X9.2 interchanges.
 * 
 * @see BaseComponent
 * @see IFA_LLNUM
 */
public class IFA_FLLNUM extends FilledStringFieldBase {
    public IFA_FLLNUM() {
        super(RightPadder.SPACE_PADDER, AsciiInterpreter.INSTANCE, AsciiPrefixer.LL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_FLLNUM(int len, String description) {
        super(len, description, RightPadder.SPACE_PADDER, AsciiInterpreter.INSTANCE, AsciiPrefixer.LL);
        checkLength(len, 99);
    }

    public void setLength(int len)
    {
        checkLength(len, 99);
        super.setLength(len);
    }
}
