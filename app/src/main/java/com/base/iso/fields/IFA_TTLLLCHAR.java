package com.base.iso.fields;

import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.fields.base.aTaggedFieldBase;

/**
 * Packager for TTLLLCHAR TLV subfields
 *
 */
public class IFA_TTLLLCHAR extends aTaggedFieldBase {

    @Override
    protected int getTagNameLength() {
        return 2;
    }

    protected aISOFieldBase getDelegate(int length, String description) {
        return new IFA_LLLCHAR(length, description);
    }
}
