package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLBINARY 
 * @see BaseComponent
 */
public class IFB_LLBINARY extends BinaryFieldBase {
    public IFB_LLBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LL);
        checkLength(len, 99);
    }

    public void setLength(int len)
    {
        checkLength(len, 99);
        super.setLength(len);
    }
}
