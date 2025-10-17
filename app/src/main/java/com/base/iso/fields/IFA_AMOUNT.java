package com.base.iso.fields;

import com.base.iso.fields.base.AmountFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.models.interpreter.AsciiInterpreter;
import com.base.iso.models.padder.LeftPadder;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * ISOFieldPackager ASCII AMOUNT.
 * This packager pads the amount to the left with zeros, prepends the sign amount, and
 * interprets the chars with an ASCII interpreter. It has no length prefix.
 * 
 * @see aISOFieldBase
 */
public class IFA_AMOUNT extends AmountFieldBase {
    public IFA_AMOUNT() {
        super(0, null, LeftPadder.ZERO_PADDER, AsciiInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_AMOUNT(int len, String description) {
        super(len, description, LeftPadder.ZERO_PADDER, AsciiInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
}
