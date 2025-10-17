package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLCHAR 
 * @see BaseComponent
 */
public class IFB_LLCHAR extends StringFieldBase {
    public IFB_LLCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LL);
        checkLength(len, 99);
    }

    public void setLength(int len)
    {
        checkLength(len, 99);
        super.setLength(len);
    }
}
