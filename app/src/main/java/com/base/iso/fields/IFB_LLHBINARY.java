package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary Hex LLBINARY 
 * @see BaseComponent
 */
public class IFB_LLHBINARY extends BinaryFieldBase {
    public IFB_LLHBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, BinaryPrefixer.B);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLHBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, BinaryPrefixer.B);
        checkLength(len, 255);
    }

    public void setLength(int len)
    {
        checkLength(len, 255);
        super.setLength(len);
    }
}
