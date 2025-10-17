package com.base.iso.fields.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;

import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOField;
import com.base.iso.interfaces.iTagMapper;
import com.base.iso.util.ISOUtil;

/**
 * Base class and template for handling tagged fields.
 * <p/>
 * This should support both fixed length and variable length tags.
 */
public abstract class aTaggedFieldBase extends aISOFieldBase {

    private iTagMapper tagMapper;
    private aISOFieldBase delegate;
    private int parentFieldNumber;
    private boolean packingLenient = false;
    private boolean unpackingLenient = false;

    public aTaggedFieldBase() {
        super();
    }

    /**
     * @param len         -
     *                    field len
     * @param description symbolic description
     */
    public aTaggedFieldBase(int len, String description) {
        super(len, description);
        this.delegate = getDelegate(len, description);
    }

    /**
     * @param c -
     *          a component
     * @return packed component
     * @throws com.base.iso.BaseException
     */
    @Override
    public byte[] pack(BaseComponent c) throws BaseException {
        byte[] packed;
        if (c.getValue() == null) {
            packed = new byte[0];
        } else {
            String tag = getTagMapper().getTagForField(getParentFieldNumber(), (Integer) c.getKey());
            if (tag == null) {
                if (!isPackingLenient()) {
                    throw new BaseException("No tag mapping found for field: " + parentFieldNumber + "." + c.getKey());
                }
                packed = new byte[0];
            } else {
                byte[] tagBytes = tag.getBytes(ISOUtil.CHARSET);
                byte[] message = getDelegate().pack(c);
                packed = new byte[tagBytes.length + message.length];
                System.arraycopy(tagBytes, 0, packed, 0, tagBytes.length);
                System.arraycopy(message, 0, packed, tagBytes.length, message.length);
            }
        }
        return packed;
    }

    @Override
    public void pack(BaseComponent c, ObjectOutput out) throws IOException, BaseException {
        if (c.getValue() != null) {
            super.pack(c, out);
        }
    }

    /**
     * @param c      -
     *               the Component to unpack
     * @param b      -
     *               binary image
     * @param offset -
     *               starting offset within the binary image
     * @return consumed bytes
     * @throws BaseException
     */
    @Override
    public int unpack(BaseComponent c, byte[] b, int offset) throws BaseException {
        int consumed;
        byte[] tagBytes = new byte[getTagNameLength()];
        System.arraycopy(b, offset, tagBytes, 0, getTagNameLength());
        String tag = new String(tagBytes, ISOUtil.CHARSET);
        if (!(c instanceof ISOField) && !(c instanceof BinaryFieldComponent))
            throw new BaseException(c.getClass().getName()
                    + " is not an ISOField");
        Integer fieldNumber = getTagMapper().getFieldNumberForTag(getParentFieldNumber(), tag);
        if (fieldNumber == null || fieldNumber < 0) {
            if (!isUnpackingLenient()) {
                throw new BaseException("No field mapping found for tag: " + parentFieldNumber + "." + tag);
            }
            consumed = 0;
        } else {
            if (c.getKey().equals(fieldNumber)) {
                consumed = getTagNameLength() + getDelegate().unpack(c, b, offset + tagBytes.length);
            } else {
                consumed = 0;
            }
        }
        return consumed;
    }

    @Override
    public void unpack(BaseComponent c, InputStream in) throws IOException,
            BaseException {
        if (!in.markSupported()) {
            throw new BaseException("InputStream should support marking");
        }
        if (!(c instanceof ISOField))
            throw new BaseException(c.getClass().getName()
                    + " is not an ISOField");
        in.mark(getTagNameLength() + 1);
        Integer fieldNumber;
        String tag;
        tag = new String(readBytes(in, getTagNameLength()), ISOUtil.CHARSET);
        fieldNumber = getTagMapper().getFieldNumberForTag(getParentFieldNumber(), tag);
        if (fieldNumber == null || fieldNumber < 0) {
            if (!isUnpackingLenient()) {
                throw new BaseException("No field mapping found for tag: " + parentFieldNumber + "." + tag);
            }
            in.reset();
        } else {
            if (c.getKey().equals(fieldNumber)) {
                getDelegate().unpack(c, in);
            } else {
                in.reset();
            }
        }
    }

    private synchronized aISOFieldBase getDelegate() {
        if (delegate == null) {
            delegate = getDelegate(getLength(), getDescription());
        }
        return delegate;
    }

    protected abstract aISOFieldBase getDelegate(int len, String description);

    protected abstract int getTagNameLength();

    /**
     * @return A boolean value for or against lenient packing
     */
    protected boolean isPackingLenient() {
        return packingLenient;
    }

    /**
     * @return A boolean value for or against lenient unpacking
     */
    protected boolean isUnpackingLenient() {
        return unpackingLenient;
    }

    public void setPackingLenient(boolean packingLenient) {
        this.packingLenient = packingLenient;
    }

    public void setUnpackingLenient(boolean unpackingLenient) {
        this.unpackingLenient = unpackingLenient;
    }

    @Override
    public int getMaxPackedLength() {
        return getTagNameLength() + getDelegate().getMaxPackedLength();
    }

    public int getParentFieldNumber() {
        return parentFieldNumber;
    }

    public void setParentFieldNumber(int parentFieldNumber) {
        this.parentFieldNumber = parentFieldNumber;
    }

    public void setTagMapper(iTagMapper tagMapper) {
        this.tagMapper = tagMapper;
    }

    protected iTagMapper getTagMapper() {
        return tagMapper;
    }

}
