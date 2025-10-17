package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * ISOFieldPackager Binary Field 
 * @see BaseComponent
 */

public class IFB_BINARY extends BinaryFieldBase {
    public IFB_BINARY() {
        super(LiteralBinaryInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_BINARY(int len, String description) {
        super(len, description, LiteralBinaryInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
}
