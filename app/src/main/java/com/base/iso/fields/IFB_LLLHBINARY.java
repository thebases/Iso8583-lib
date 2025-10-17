package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary LLLHBINARY 
 * @see BaseComponent
 */
public class IFB_LLLHBINARY extends BinaryFieldBase {
    public IFB_LLLHBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, BinaryPrefixer.BB);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLHBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, BinaryPrefixer.BB);
        checkLength(len, 65535);
    }

    public void setLength(int len)
    {
        checkLength(len, 65535);
        super.setLength(len);
    }
}
