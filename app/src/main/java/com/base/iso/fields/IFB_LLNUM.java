package com.base.iso.fields;

import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.BcdPrefixer;

/**
 * ISOFieldPackager Binary LLNUM
 */
public class IFB_LLNUM extends StringFieldBase {
    public IFB_LLNUM() {
        super(NullPadder.INSTANCE, BCDInterpreter.RIGHT_PADDED, BcdPrefixer.LL);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_LLNUM(int len, String description, boolean isLeftPadded) {
        super(len, description, NullPadder.INSTANCE,
                isLeftPadded ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED,
                BcdPrefixer.LL);
        checkLength(len, 99);
    }
    
    public IFB_LLNUM(int len, String description, boolean isLeftPadded, boolean fPadded) {
        super(len, description, NullPadder.INSTANCE,
                isLeftPadded ? BCDInterpreter.LEFT_PADDED :
                        fPadded ? BCDInterpreter.RIGHT_PADDED_F : BCDInterpreter.RIGHT_PADDED,
                BcdPrefixer.LL);
        checkLength(len, 99);
    }

    public void setLength(int len)
    {
        checkLength(len, 99);
        super.setLength(len);
    }

    /** Must override ISOFieldPackager method to set the Interpreter correctly */
    public void setPad (boolean pad)
    {
        setInterpreter(pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED);
        this.pad = pad;
    }
}

