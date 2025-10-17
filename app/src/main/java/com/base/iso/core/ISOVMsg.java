package com.base.iso.core;
import java.util.LinkedList;
import java.util.ListIterator;

import com.base.iso.core.error.ISOVError;
import com.base.iso.core.error.ISOVErrorList;

/**
 * Wrapper class resulting from process of validating an ISOMsg
 * instance. Contains details of the original msg and validation-error
 * details too. Normally in validation process when an error is detected
 * by validator in msg, then the msg is replaced by an instance
 * of this class, containning error details. 
 */
@SuppressWarnings("unchecked")
public class ISOVMsg extends ISOMsg implements ISOVErrorList {

    private static final long serialVersionUID = 443461124206801037L;

    /**
     * Copy properties from parent.
     * @param Source original instance.
     */
    private void copyFromParent( ISOMsg Source ){
        this.packager = Source.packager;
        this.fields = Source.fields;
        this.dirty = Source.dirty;
        this.maxFieldDirty = Source.maxFieldDirty;
        this.header = Source.header;
        this.fieldNumber = Source.fieldNumber;
        this.maxField = Source.maxField;
        this.direction = Source.direction;
    }

    /**
     * Create a message from original instance adding error data.
     * @param Source Original msg instance.
     */
    public ISOVMsg( ISOMsg Source ) {
        /** @todo Try best strategy */
        copyFromParent( Source );
    }

    public ISOVMsg( ISOMsg Source, ISOVError FirstError ) {
        /** @todo Try best strategy */
        copyFromParent( Source );
        addISOVError( FirstError );
    }

    /**
     * Add an error component to the list of errors.
     * @param Error Error instance to add.
     * @return True if the list of errors change after operation.
     */
    public boolean addISOVError(ISOVError Error) {
        return errors.add( Error );
    }

    /**
     * Get an error iterator instance.
     * @return iterator.
     */
    public ListIterator errorListIterator() {
        return errors.listIterator();
    }

    /** list of errors **/
    protected LinkedList errors = new LinkedList(  );
}
