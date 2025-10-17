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
public class IFB_LLLCHAR extends StringFieldBase {
    public IFB_LLLCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BcdPrefixer.LLL);
        checkLength(len, 999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999);
        super.setLength(len);
    }
}
