package com.base.iso.fields;

import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.HexNibblesPrefixer;

/**
 * BinaryFieldBase 
 */
@SuppressWarnings("unused")
public class IFB_LLHEX extends BinaryFieldBase {
    public IFB_LLHEX() {
        super(LiteralBinaryInterpreter.INSTANCE, HexNibblesPrefixer.LL);
    }

    public IFB_LLHEX (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, HexNibblesPrefixer.LL);
        checkLength(len, 99);
    }
    public void setLength(int len) {
        checkLength(len, 99);
        super.setLength(len);
    }
}
