package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLLCHAR 
 * @see BaseComponent
 */
public class IFB_LLLLLLCHAR extends StringFieldBase {
    public IFB_LLLLLLCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LLLLLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLLLLCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LLLLLL);
        checkLength(len, 999999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999999);
        super.setLength(len);
    }
}
