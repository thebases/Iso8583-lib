package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.models.padder.LeftPadder;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * ISOFieldPackager Binary Numeric
 * @see BaseComponent
 */
public class IFB_FNUMERIC extends StringFieldBase {
    public IFB_FNUMERIC() {
        super(LeftPadder.ZERO_PADDER, BCDInterpreter.RIGHT_PADDED_F, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_FNUMERIC(int len, String description, boolean isLeftPadded) {
        super(len, description, LeftPadder.ZERO_PADDER,
                isLeftPadded ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED_F,
                NullPrefixer.INSTANCE);
    }

    /** Must override ISOFieldPackager method to set the Interpreter correctly */
    public void setPad(boolean pad)
    {
        setInterpreter(pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED_F);
        this.pad = pad;
    }
}
