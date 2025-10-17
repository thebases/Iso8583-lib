package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.EbcdicInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BinaryPrefixer;

/** 
 * @see BaseComponent
 * @see IFB_LLHECHAR
 */
public class IFB_LLLHECHAR extends StringFieldBase {
    public IFB_LLLHECHAR() {
        super(NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, BinaryPrefixer.BB);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLLHECHAR (int len, String description) {
        super(len, description, NullPadder.INSTANCE, EbcdicInterpreter.INSTANCE, BinaryPrefixer.BB);
        checkLength(len, 65535);
    }

    public void setLength(int len)
    {
        checkLength(len, 65535);
        super.setLength(len);
    }
}
