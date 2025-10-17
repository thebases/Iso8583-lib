package com.base.iso.config;

import com.base.iso.core.BaseException;

public class ConfigurationException extends BaseException {

    private static final long serialVersionUID = -5605240786314946532L;
    public ConfigurationException () {
        super();
    }
    public ConfigurationException (String detail) {
        super (detail);
    }
    public ConfigurationException (Throwable nested) {
        super (nested);
    }
    public ConfigurationException (String detail, Throwable nested) {
        super (detail, nested);
    }
}
