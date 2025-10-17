package com.base.iso.interfaces;

import com.base.iso.fields.base.MsgFieldBase;

/**
 * Interafce enable to get current composite field number.
 * <p>
 * {@link MsgFieldBase} needs a number of current (parent) composite field
 * in order to enable nesting in its tags other composite fields (e.g.
 * {@link GenericSubFieldPackager} or even {@link GenericTaggedFieldsPackager})
 */
public interface iSubFieldBase {

    /**
     * Get current composite field number
     * @return composite field number
     */
    public int getFieldNumber();

}
