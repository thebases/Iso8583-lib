package com.base.iso.fields;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.BitMapFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * ISOFieldPackager Binary Bitmap 
 * @see BaseComponent
 * @see BitMapFieldBase
 */
public class IFB_BITMAP extends BitMapFieldBase {
    public IFB_BITMAP() {
        super();
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFB_BITMAP(int len, String description) {
        super(len, description);
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
        BitSet b = (BitSet) c.getValue();
        int len =                                           // bytes needed to encode BitSet (in 8-byte chunks)
            getLength() >= 8 ?
                    b.length()+62 >>6 <<3 : getLength();    // +62 because we don't use bit 0 in the BitSet
        return ISOUtil.bitSet2byte (b, len);
    }
    /**
     * @param c - the Component to unpack
     * @param b - binary image
     * @param offset - starting offset within the binary image
     * @return consumed bytes
     * @exception BaseException
     */
    public int unpack (BaseComponent c, byte[] b, int offset)
        throws BaseException
    {
        int len;
        BitSet bmap = ISOUtil.byte2BitSet (b, offset, getLength() << 3);
        c.setValue(bmap);
        len = bmap.get(1) ? 128 : 64;
        if (getLength() > 16 && bmap.get(1) && bmap.get(65))
            len = 192;
        return Math.min (getLength(), len >> 3);
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        BitSet bmap = ISOUtil.byte2BitSet (new BitSet (64), readBytes (in, 8), 0);
        if (getLength() > 8 && bmap.get (1)) {
            ISOUtil.byte2BitSet (bmap, readBytes (in, 8), 64); 
        }
        if (getLength() > 16 && bmap.get (65)) {
            ISOUtil.byte2BitSet (bmap, readBytes (in, 8), 128); 
        }
        c.setValue(bmap);
    }
    public int getMaxPackedLength() {
        return getLength() >> 3;
    }
}
