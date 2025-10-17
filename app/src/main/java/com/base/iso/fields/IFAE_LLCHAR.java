package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.models.interpreter.EbcdicInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.AsciiPrefixer;

/**
 * Esoteric version of IFE_LLCHAR where payload is in EBCDIC but length in ASCII 
 * @see aISOFieldBase
 * @see BaseComponent
 */
public class IFAE_LLCHAR extends StringFieldBase {
    public IFAE_LLCHAR() {
        super(NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, AsciiPrefixer.LL);
    }
    /**
     * @param len - field len
     * @param description symbolic description
     */
    public IFAE_LLCHAR(int len, String description) {
        super(len, description, NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, AsciiPrefixer.LL);
        checkLength(len, 99);
    }

    public void setLength(int len)
    {
        checkLength(len, 99);
        super.setLength(len);
    }
}

