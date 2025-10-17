package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLLBINARY 
 * @see BaseComponent
 */
public class IFB_LLLBINARY extends BinaryFieldBase {
    public IFB_LLLBINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLBINARY (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, BcdPrefixer.LLL);
        checkLength(len, 999);
    }
    
    public void setLength(int len)
    {
        checkLength(len, 999);
        super.setLength(len);
    }
}
