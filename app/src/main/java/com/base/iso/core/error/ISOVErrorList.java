package com.base.iso.core.error;

import java.util.ListIterator;

/**
 * List of errors. It is formed during validation process. 
 */
public interface ISOVErrorList {

    /**
     * Add an ISOVError to the list.
     * @see com.base.iso.ISOVError
     * @param Error error detailed instance.
     * @return true if error list change after operation. See Collection.
     */
    public boolean addISOVError( ISOVError Error );

    /**
     * Get an error list iterator.
     * @return iterator instance.
     */
    public ListIterator errorListIterator();
}
