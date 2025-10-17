package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager ASCII variable len BINARY
 * @see BaseComponent
 */
public class IFB_LLLLBINARY extends BinaryFieldBase {
    public IFB_LLLLBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LLLL);
        checkLength(len, 9999);
    }

    public void setLength(int len)
    {
        checkLength(len, 9999);
        super.setLength(len);
    }
}

