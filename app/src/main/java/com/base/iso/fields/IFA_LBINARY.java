package com.base.iso.fields;

import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * ISOFieldPackager ASCII variable len BINARY
 *
 */
public class IFA_LBINARY extends BinaryFieldBase {
    public IFA_LBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.L);
    }

    /**
     * @param len         - field len
     * @param description symbolic descrption
     */
    public IFA_LBINARY(int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, AsciiPrefixer.L);
        checkLength(len, 9);
    }

    public void setLength(int len) {
        checkLength(len, 9);
        super.setLength(len);
    }
}

