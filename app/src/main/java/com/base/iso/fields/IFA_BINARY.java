package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.BinaryFieldBase;
import com.base.iso.models.interpreter.AsciiHexInterpreter;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * ISOFieldPackager ASCII Binary
 * 
 * @see BaseComponent
 */
public class IFA_BINARY extends BinaryFieldBase {
    public IFA_BINARY() {
        super(AsciiHexInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_BINARY(int len, String description) {
        super(len, description, AsciiHexInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
}
