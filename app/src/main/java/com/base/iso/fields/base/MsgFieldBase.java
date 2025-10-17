package com.base.iso.fields.base;


import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.iSubFieldBase;


/**
 * MsgFieldBase is a packager able to pack compound ISOMsgs
 * (one message inside another one, and so on...) 
 * @see com.base.iso.packager.PostPackager
 */
public class MsgFieldBase extends aISOFieldBase {
    protected iPackager msgPackager;
    protected aISOFieldBase fieldPackager;

    /**
     * @param fieldPackager low level field packager
     * @param msgPackager ISOMsgField default packager
     */
    public MsgFieldBase (
            aISOFieldBase fieldPackager,
            iPackager msgPackager)
    {
        super(fieldPackager.getLength(), fieldPackager.getDescription());
        this.msgPackager = msgPackager;
        this.fieldPackager = fieldPackager;
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    @Override
    public byte[] pack (BaseComponent c) throws BaseException {
        if (c instanceof ISOMsg) {
            ISOMsg m = (ISOMsg) c;
            m.recalcBitMap();
            BinaryFieldComponent f = new BinaryFieldComponent(0, msgPackager.pack(m));
            if(fieldPackager instanceof aTaggedFieldBase &&
               msgPackager   instanceof iSubFieldBase) {
                iSubFieldBase sfp = (iSubFieldBase) msgPackager;
                f.setFieldNumber(sfp.getFieldNumber());
            }
            return fieldPackager.pack(f);
        }
        return fieldPackager.pack(c);
    }

    /**
     * @param c - the Component to unpack
     * @param b - binary image
     * @param offset - starting offset within the binary image
     * @return consumed bytes
     * @exception BaseException
     */
    @Override
    public int unpack (BaseComponent c, byte[] b, int offset)
        throws BaseException
    {
        BinaryFieldComponent f = new BinaryFieldComponent(0);
        if(msgPackager instanceof iSubFieldBase) {
            iSubFieldBase sfp = (iSubFieldBase) msgPackager;
            f.setFieldNumber(sfp.getFieldNumber());
        }
        int consumed = fieldPackager.unpack(f, b, offset);
        if (f.getValue() != null && c instanceof ISOMsg)
            msgPackager.unpack(c, (byte[]) f.getValue());
        return consumed;
    }

    /**
     * @param c  - the Component to unpack
     * @param in - input stream
     * @throws com.base.iso.BaseException
     * @throws java.io.IOException
     */
    @Override
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        BinaryFieldComponent f = new BinaryFieldComponent(0);
        if(msgPackager instanceof iSubFieldBase) {
            iSubFieldBase sfp = (iSubFieldBase) msgPackager;
            f.setFieldNumber(sfp.getFieldNumber());
        }
        fieldPackager.unpack (f, in);
        if (f.getValue() != null && c instanceof ISOMsg)
            msgPackager.unpack(c, (byte[]) f.getValue());
    }

    @Override
    public BaseComponent createComponent(int fieldNumber) {
        ISOMsg m = new ISOMsg(fieldNumber);
        m.setPackager(msgPackager);
        return m;
    }

    @Override
    public int getMaxPackedLength() {
        return fieldPackager.getLength();
    }
    public iPackager getISOMsgPackager() {
        return msgPackager;
    }
    public aISOFieldBase getISOFieldPackager() {
        return fieldPackager;
    }
}
