package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.EbcdicInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary Hex EBCDIC LLCHAR
 * IFB_LLHCHAR with EBCDIC conversion
 * (VISA's SMS field 54, additional amounts) 
 * @see BaseComponent
 * @see IFB_LLHCHAR
 */
public class IFB_LLHECHAR extends StringFieldBase {
    public IFB_LLHECHAR() {
        super(NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, BinaryPrefixer.B);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLHECHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, BinaryPrefixer.B);
        checkLength(len, 255);
    }

    public void setLength(int len)
    {
        checkLength(len, 255);
        super.setLength(len);
    }
}
