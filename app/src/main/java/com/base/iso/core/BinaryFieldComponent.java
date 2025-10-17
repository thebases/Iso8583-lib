package com.base.iso.core;

import java.io.*;
import com.base.iso.util.ISOUtil;

/**
 * implements <b>Leaf</b> for binary fields
 *
 * See the
 * <a href="API_users_guide.html">API User's Guide</a>
 * for details. 
 * @see BaseComponent
 */
public class BinaryFieldComponent 
    extends BaseComponent 
    implements Cloneable, Externalizable
{

    private static final long serialVersionUID = 7702505828876640372L;
    protected int fieldNumber;
    protected byte[] value;

    /**
     * No args constructor 
     * <font size="-1">(required by Externalizable support on ISOMsg)</font>
     */
    public BinaryFieldComponent () {
        fieldNumber = -1;
    }
    /**
     * @param n - the FieldNumber
     */
    public BinaryFieldComponent(int n) {
        fieldNumber = n;
    }
    /**
     * @param n - fieldNumber
     * @param v - fieldValue
     */
    public BinaryFieldComponent(int n, byte[] v) {
        fieldNumber = n;
        value = v;
    }
    /**
     * @param n - fieldNumber
     * @param v - fieldValue
     * @param offset - starting offset
     * @param len    - field length
     */
    public BinaryFieldComponent(int n, byte[] v, int offset, int len) {
        byte[] b = new byte[len];
        System.arraycopy (v, offset, b, 0, len);
        fieldNumber = n;
        value = b;
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
    public void setValue (Object obj) throws BaseException {
        if (obj instanceof String)
            value = ((String) obj).getBytes();
        else
            value = (byte[]) obj;
    }
    /**
     * @return byte[] representing this field
     */
    public byte[] getBytes() {
        return value;
    }
    /**
     * dump this field to PrintStream. The output is sorta
     * XML, intended to be easily parsed.
     * @param p - print stream
     * @param indent - optional indent string
     */
    public void dump (PrintStream p, String indent) {
        p.println (indent +"<"+BaseConstant.ISOFIELD_TAG + " " +
            BaseConstant.ID_ATTR +"=\"" +fieldNumber +"\" "+
            BaseConstant.VALUE_ATTR +"=\"" +this.toString() + "\" " +
            BaseConstant.TYPE_ATTR +"=\"" + BaseConstant.TYPE_BINARY + "\"/>"
        );
    }
    public String toString() {
        return ISOUtil.hexString(value);
    }
    public void writeExternal (ObjectOutput out) throws IOException {
        out.writeShort (fieldNumber);
        out.writeShort (value.length);
        out.write (value);
    }
    public void readExternal  (ObjectInput in) 
        throws IOException, ClassNotFoundException
    {
        fieldNumber = in.readShort ();
        value = new byte[in.readShort()];
        in.readFully (value);
    }
}
