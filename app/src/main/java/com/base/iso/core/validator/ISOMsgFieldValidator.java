
package com.base.iso.core.validator;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;

/**
 * Validator for composed fields. See class comments in file
 * com.base.iso.ISOMsgFieldValidatingPackager. 
 */
public class ISOMsgFieldValidator extends ISOFieldValidator {

    public ISOMsgFieldValidator ( String Description,
                                  ISOValidator msgValidator ){
        super( Description);
        this.msgValidator = msgValidator;
    }

    public BaseComponent validate(BaseComponent m) throws BaseException {
        return (m instanceof ISOMsg)? msgValidator.validate( m ):m;
    }

    protected ISOValidator msgValidator;
}