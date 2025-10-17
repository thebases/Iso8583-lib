package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.HEXInterpreter;
import com.base.iso.models.padder.LeftPadder;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * Simpilar to IFB_NUMERIC (BCD) with support for HEX characters 
 * @see BaseComponent
 */
public class IFB_HEX extends StringFieldBase {
    public IFB_HEX() {
        super(LeftPadder.ZERO_PADDER, HEXInterpreter.RIGHT_PADDED, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_HEX (int len, String description, boolean isLeftPadded) {
        super(len, description, LeftPadder.ZERO_PADDER,
                isLeftPadded ? HEXInterpreter.LEFT_PADDED : HEXInterpreter.RIGHT_PADDED_F,
                NullPrefixer.INSTANCE);
    }

    /** Must override ISOFieldPackager method to set the Interpreter correctly */
    public void setPad(boolean pad)
    {
        setInterpreter(pad ? HEXInterpreter.LEFT_PADDED : HEXInterpreter.RIGHT_PADDED_F);
        this.pad = pad;
    }
}
