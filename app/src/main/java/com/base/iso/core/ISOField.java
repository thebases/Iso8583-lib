package com.base.iso.core;

import java.io.*;
import com.base.iso.util.ISOUtil;

/**
 * implements <b>Leaf</b> for standard fields 
 * @see BaseComponent
 */
public class ISOField 
    extends BaseComponent 
    implements Cloneable, Externalizable
{

    private static final long serialVersionUID = -4053616930139887829L;
    protected int fieldNumber;
    protected String value;

    /**
     * No args constructor 
     * <font size="-1">(required by Externalizable support on ISOMsg)</font>
     */
    public ISOField () {
        fieldNumber = -1;
    }

    /**
     * @param n - the FieldNumber
     */
    public ISOField (int n) {
        fieldNumber = n;
    }
    /**
     * @param n - fieldNumber
     * @param v - fieldValue
     */
    public ISOField (int n, String v) {
        fieldNumber = n;
        value = v;
    }
    /**
     * not available on Leaf - always throw ISOException
     * @return
     * @exception BaseException
     */
    @Override
    public byte[] pack() throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * not available on Leaf - always throw ISOException
     * @param b
     * @return
     * @exception BaseException
     */
    @Override
    public int unpack(byte[] b) throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * not available on Leaf - always throw ISOException
     * @param in
     * @exception BaseException
     */
    @Override
    public void unpack(InputStream in) throws BaseException {
        throw new BaseException ("Not available on Leaf");
    }
    /**
     * @return Object representing this field number
     */
    @Override
    public Object getKey() {
        return fieldNumber;
    }
    /**
     * @return Object representing this field value
     */
    @Override
    public Object getValue() {
        return value;
    }
    /**
     * @param obj - Object representing this field value
     * @exception BaseException
     */
    @Override
    public void setValue(Object obj) throws BaseException {
        if (obj instanceof String)
            value = (String) obj;
        else
            value = obj.toString();
    }
    /**
     * @return byte[] representing this field
     */
    @Override
    public byte[] getBytes() {
        return (value != null) ? value.getBytes(ISOUtil.CHARSET) : new byte[] {};
    }
    /**
     * dump this field to PrintStream. The output is sorta
     * XML, intended to be easily parsed.
     * @param p - print stream
     * @param indent - optional indent string
     */
    @Override
    public void dump (PrintStream p, String indent) {
        if (value != null && value.indexOf('<') >= 0) {
            p.print (indent +"<"+BaseConstant.ISOFIELD_TAG + " " +
                BaseConstant.ID_ATTR +"=\"" +fieldNumber +"\"><![CDATA[");
            p.print (value);
            p.println ("]]></" + BaseConstant.ISOFIELD_TAG + ">");                        
        } else {
            // p.println (indent +"<"+XMLPackager.ISOFIELD_TAG + " " +
            //     XMLPackager.ID_ATTR +"=\"" +fieldNumber +"\" "+
            //     XMLPackager.VALUE_ATTR
            //     +"=\"" +ISOUtil.normalize (value) +"\"/>");
            p.println (indent +"\""+fieldNumber +"\": \"" +ISOUtil.normalize (value) +"\",");
        }
    }
    /**
     * changes this Component field number<br>
     * Use with care, this method does not change
     * any reference held by a Composite.
     * @param fieldNumber new field number
     */
    @Override
    public void setFieldNumber (int fieldNumber) {
        this.fieldNumber = fieldNumber;
    }

    @Override
    public int getFieldNumber () {
        return fieldNumber;
    }
    @Override
    public void writeExternal (ObjectOutput out) throws IOException {
        out.writeShort (fieldNumber);
        out.writeUTF (value);
    }
    @Override
    public void readExternal  (ObjectInput in) 
        throws IOException, ClassNotFoundException
    {
        fieldNumber = in.readShort ();
        value       = in.readUTF();
    }
}
