package com.base.iso.fields;

import com.base.iso.core.AmountComponent;
import com.base.iso.core.BaseComponent;

public class IFB_AMOUNT2003 extends IFB_NUMERIC {
    public BaseComponent createComponent(int fieldNumber) {
        return new AmountComponent (fieldNumber);    
    }
}

