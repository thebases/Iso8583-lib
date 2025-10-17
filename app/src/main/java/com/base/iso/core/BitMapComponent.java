package com.base.iso.core;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.BitSet;



/**
 * implements <b>Leaf</b> for Bitmap field 
 * @see BaseComponent
 */
public class BitMapComponent extends BaseComponent implements Cloneable {
    protected int fieldNumber;
    protected BitSet value;

    /**
     * @param n - the FieldNumber
     */
    public BitMapComponent (int n) {
        fieldNumber = n;
    }
    /**
     * @param n - fieldNumber
     * @param v - field value (Bitset)
     * @see BitSet
     */
    public BitMapComponent (int n, BitSet v) {
        fieldNumber = n;
        value = v;
    }
    /**
     * changes this Component field number<br>
     * Use with care, this method does not change
     * any reference held by a Composite.
     * @param fieldNumber new field number
     */
    public void setFieldNumber (int fieldNumber) {
        this.fieldNumber = fieldNumber;
    }

    @Override
    public int getFieldNumber () {
        return fieldNumber;
    }

    /**
     * not available on Leaf - always throw ISOException
     * @exception BaseException
     */
    public byte[] pack() throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * not available on Leaf - always throw ISOException
     * @exception BaseException
     */
    public int unpack(byte[] b) throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * not available on Leaf - always throw ISOException
     * @exception BaseException
     */
    public void unpack(InputStream in) throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * @return Object representing this field number
     */
    public Object getKey() {
        return fieldNumber;
    }
    /**
     * @return Object representing this field value
     */
    public Object getValue() {
        return value;
    }
    /**
     * @param obj - Object representing this field value
     * @exception BaseException
     */
    public void setValue(Object obj) throws BaseException {
        value = (BitSet) obj;
    }
    /**
     * dump this field to PrintStream.
     * @param p - print stream
     * @param indent - optional indent string
     */
    public void dump (PrintStream p, String indent) {
        p.println (indent +"\""+BaseConstant.TYPE_BITMAP+"\": " + value+", "
            // BaseConstant.TYPE_ATTR +"=\"" + BaseConstant.TYPE_BITMAP+ "\"/>"
        );
    }
}
