package com.base.iso.fields;

import com.base.iso.core.BaseComponent;
import com.base.iso.fields.base.StringFieldBase;
import com.base.iso.models.interpreter.LiteralInterpreter;
import com.base.iso.models.padder.RightTPadder;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * The IF_CHAR packager pads to the right with spaces, truncating data that is too long.
 * It uses a literal interpreter and has no length prefix. 
 * 
 * @see BaseComponent
 */
public class IF_CHAR extends StringFieldBase {
    /** Used for the GenericPackager. */
    public IF_CHAR() {
        super(0, null, RightTPadder.SPACE_PADDER, LiteralInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }

    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IF_CHAR(int len, String description) {
        super(len, description, RightTPadder.SPACE_PADDER, LiteralInterpreter.INSTANCE, NullPrefixer.INSTANCE);
    }
}
