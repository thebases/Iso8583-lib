package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary Hex LLLCHAR 
 * @see BaseComponent
 */
public class IFB_LLLHCHAR extends StringFieldBase {
    public IFB_LLLHCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BinaryPrefixer.BB);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLHCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BinaryPrefixer.BB);
        checkLength(len, 65535);
    }

    public void setLength(int len)
    {
        checkLength(len, 65535);
        super.setLength(len);
    }
}

