package com.base.iso.fields;

import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.fields.base.aTaggedFieldBase;

/**
 * Packager for TTLLCHAR TLV subfields
 *
 */
public class IFA_TTLLCHAR extends aTaggedFieldBase {

    @Override
    protected int getTagNameLength() {
        return 2;
    }

    protected aISOFieldBase getDelegate(int length, String description) {
        return new IFA_LLCHAR(length, description);
    }
}
