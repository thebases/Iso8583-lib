package com.base.iso.core.validator;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;

/**
 * Validates a  BaseComponent. These kind of validations are
 * considered in lowest level. Validation models at higher level
 * must not be considered here. Something like field-interdependency
 * and others complex validation rules are not included in these validations.
 */
public interface ISOValidator {

    /**
     * Validate an BaseComponent.
     * @throws BaseException if break-on-error is assummed and there are
     * some errors.
     */
    public BaseComponent validate( BaseComponent m ) throws BaseException;
}
