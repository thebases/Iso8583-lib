package com.base.iso.fields;

import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.HexNibblesPrefixer;

/**
 * BinaryFieldBase
 *
 */
@SuppressWarnings("unused")
public class IFB_LLLHEX extends BinaryFieldBase {
    public IFB_LLLHEX() {
        super(LiteralBinaryInterpreter.INSTANCE, HexNibblesPrefixer.LLL);
    }

    public IFB_LLLHEX (int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, HexNibblesPrefixer.LLL);
        checkLength(len, 999);
    }
    public void setLength(int len) {
        checkLength(len, 999);
        super.setLength(len);
    }
}

