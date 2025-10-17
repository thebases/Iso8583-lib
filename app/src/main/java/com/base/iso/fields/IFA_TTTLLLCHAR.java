package com.base.iso.fields;

import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.fields.base.aTaggedFieldBase;

/**
 * Packager for TTTLLLCHAR TLV subfields
 *
 */
public class IFA_TTTLLLCHAR extends aTaggedFieldBase {

    @Override
    protected int getTagNameLength() {
        return 3;
    }

    protected aISOFieldBase getDelegate(int length, String description) {
        return new IFA_LLLCHAR(length, description);
    }
}

