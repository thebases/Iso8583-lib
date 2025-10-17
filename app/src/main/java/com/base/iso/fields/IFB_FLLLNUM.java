package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLLNUM 
 * @see BaseComponent
 */

public class IFB_FLLLNUM extends StringFieldBase {
    public IFB_FLLLNUM() {
        super(NullPadder.INSTANCE, BCDInterpreter.RIGHT_PADDED_F, BcdPrefixer.LLL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_FLLLNUM(int len, String description, boolean isLeftPadded) {
        super(len, description, NullPadder.INSTANCE,
                isLeftPadded ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED_F,
                BcdPrefixer.LLL);
        checkLength(len, 999);
    }

    public void setLength(int len)
    {
        checkLength(len, 999);
        super.setLength(len);
    }

    /** Must override ISOFieldPackager method to set the Interpreter correctly */
    public void setPad(boolean pad)
    {
        setInterpreter(pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED_F);
        this.pad = pad;
    }
}
