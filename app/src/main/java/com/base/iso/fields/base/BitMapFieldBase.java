package com.base.iso.fields.base;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BitMapComponent;

/**
 * IF*_BITMAP classes extends this class instead of ISOFieldPackager
 * so packagers can check if field-1 ISOFieldPackager is an instance
 * of an ISOBitMapPackager and handle differences between ANSI X9.2
 * and ISO-8583 packaging schemes. 
 *
 * @see aISOFieldBase
 */
public abstract class BitMapFieldBase extends aISOFieldBase {
    public BitMapFieldBase() {
        super();
    }
    public BitMapFieldBase(int len, String description) {
        super(len, description);
    }
    public BaseComponent createComponent(int fieldNumber) {
        return new BitMapComponent (fieldNumber);
    }
}
