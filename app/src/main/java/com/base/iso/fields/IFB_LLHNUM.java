package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BinaryPrefixer;

/**
 * ISOFieldPackager Binary LL Hex NUM
 * Almost the same as IFB_LLNUM but len is encoded as a binary
 * value. A len of 16 is encoded as 0x10 instead of 0x16 
 * @see BaseComponent
 */
public class IFB_LLHNUM extends StringFieldBase {
    public IFB_LLHNUM() {
        super(NullPadder.INSTANCE, BCDInterpreter.RIGHT_PADDED, BinaryPrefixer.B);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLHNUM(int len, String description, boolean pad) {
        super(len, description, NullPadder.INSTANCE,
                pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED,
                BinaryPrefixer.B);
        this.pad = pad;
        checkLength(len, 255);
    }

    public void setLength(int len)
    {
        checkLength(len, 255);
        super.setLength(len);
    }
    
    public void setPad(boolean pad) {
        this.pad = pad;
        setInterpreter(pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED);
    }
}
