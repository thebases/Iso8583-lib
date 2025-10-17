package com.base.iso.core;

import java.util.LinkedList;
import java.util.ListIterator;

import com.base.iso.core.error.ISOVError;
import com.base.iso.core.error.ISOVErrorList;

/**
 * Wrapper class resulting from process of validating an ISOField
 * instance. Contains details of the original field and validation-error
 * details too. Normally in validation process when an error is detected
 * by validator in some field, then the field is replaced by an instance
 * of this class, containning error details. 
 */
@SuppressWarnings("unchecked")
public class ISOVField extends ISOField implements ISOVErrorList {

    private static final long serialVersionUID = -2503711799295775875L;

    /**
     * Creates the vfield.
     * @param Source original field instance.
     */
    public ISOVField( ISOField Source ) {
        super();
        this.fieldNumber = Source.fieldNumber;
        this.value = Source.value;
    }

    public ISOVField( ISOField Source, ISOVError FirstError ) {
        this( Source );
        this.errors.addLast( FirstError );
    }

    public boolean addISOVError(ISOVError Error) {
        return errors.add( Error );
    }

    public ListIterator errorListIterator() {
        return errors.listIterator();
    }

    /** list of errors **/
    protected LinkedList errors = new LinkedList(  );
}
