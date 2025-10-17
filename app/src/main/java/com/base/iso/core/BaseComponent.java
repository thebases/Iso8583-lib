package com.base.iso.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

/**
 * implements a <b>Component</b>
 * within a <b>Composite pattern</b>
 *
 * See 
 * <a href="/doc/javadoc/overview-summary.html">Overview</a> for details.

 * @see ISOMsg
 * @see ISOField
 * @see BaseException
 */
public abstract class BaseComponent implements Cloneable {
    /**
     * Set a field within this message
     * @param c - a component
     * @exception BaseException
     */
    public void set (BaseComponent c) throws BaseException {
        throw new BaseException ("Can't add to Leaf");
    }
    /**
     * Unset a field
     * @param fldno - the field number
     * @exception BaseException
     */
    public void unset (int fldno) throws BaseException {
        throw new BaseException ("Can't remove from Leaf");
    }
    /**
     * In order to interchange <b>Composites</b> and <b>Leafs</b> we use
     * getComposite(). A <b>Composite component</b> returns itself and
     * a Leaf returns null. The base class BaseComponent provides
     * <b>Leaf</b> functionality.
     *
     * @return BaseComponent
     */
    public BaseComponent getComposite() {
        return null;
    }
    /**
     * valid on Leafs only.
     * The value returned is used by ISOMsg as a key
     * to this field.
     *
     * @return object representing the field number
     * @exception BaseException
     */
    public Object getKey() throws BaseException {
        throw new BaseException ("N/A in Composite");
    }
    /**
     * valid on Leafs only.
     * @return object representing the field value
     * @exception BaseException
     */
    public Object getValue() throws BaseException {
        throw new BaseException ("N/A in Composite");
    }
    /**
     * get Value as bytes (when possible)
     * @return byte[] representing this field
     * @exception BaseException
     */
    public byte[] getBytes() throws BaseException {
        throw new BaseException ("N/A in Composite");
    }
    /**
     * a Composite must override this function
     * @return the max field number associated with this message
     */
    public int getMaxField() {
        return 0;
    }
    /**
     * dummy behaviour - return 0 elements Hashtable
     * @return children (in this case 0 children)
     */
    public Map getChildren() {
        return new Hashtable();
    }
    /**
     * changes this Component field number<br>
     * Use with care, this method does not change
     * any reference held by a Composite.
     * @param fieldNumber new field number
     */
    public abstract void setFieldNumber (int fieldNumber);
    public abstract int getFieldNumber ();
    public abstract void setValue(Object obj) throws BaseException;
    public abstract byte[] pack() throws BaseException;
    public abstract int unpack(byte[] b) throws BaseException;
    public abstract void dump (PrintStream p, String indent);
    public void pack (OutputStream out) throws IOException, BaseException {
        out.write (pack ());
    }
    public abstract void unpack (InputStream in) throws IOException, BaseException;
}
