package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.LeftPadder;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * ISOFieldPackager ASCII NUMERIC.
 * Left padder with zeros, ASCII Interpretation, and no length prefix.  
 * @see BaseComponent
 */
public class IFA_NUMERIC extends StringFieldBase {
    public IFA_NUMERIC() {
        super(LeftPadder.ZERO_PADDER, AsciiInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_NUMERIC(int len, String description) {
        super(len, description, LeftPadder.ZERO_PADDER, AsciiInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
}
