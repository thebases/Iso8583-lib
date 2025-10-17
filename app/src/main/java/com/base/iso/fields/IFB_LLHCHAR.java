package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary Hex LLCHAR
 * Almost the same as IFB_LLCHAR but len is encoded as a binary
 * value. A len of 16 is encoded as 0x10 instead of 0x16 
 * @see BaseComponent
 */
public class IFB_LLHCHAR extends StringFieldBase {
    public IFB_LLHCHAR() {
        super(NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BinaryPrefixer.B);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLHCHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, AsciiInterpreter.INSTANCE, BinaryPrefixer.B);
        checkLength(len, 255);
    }

    public void setLength(int len)
    {
        checkLength(len, 255);
        super.setLength(len);
    }
}
