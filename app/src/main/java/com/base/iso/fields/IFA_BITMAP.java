package com.base.iso.fields;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.BitMapFieldBase;
import com.base.iso.util.ISOUtil;

/**
 * ASCII packaged Bitmap
 * 
 * @see BaseComponent
 * @see BitMapFieldBase
 */
public class IFA_BITMAP extends BitMapFieldBase {
    public IFA_BITMAP() {
        super();
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public IFA_BITMAP(int len, String description) {
        super(len, description);
    }
    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
        BitSet b = (BitSet) c.getValue();
        int len =
            getLength() >= 8 ?
                    b.length()+62 >>6 <<3 : getLength();
        return ISOUtil.hexString(ISOUtil.bitSet2byte (b, len)).getBytes();
    }

    public int getMaxPackedLength() {
        return getLength() >> 2;
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
        BitSet bmap = ISOUtil.hex2BitSet (b, offset, getLength() << 3);
        c.setValue(bmap);
        len = bmap.get(1) ? 128 : 64; /* changed by Hani */
        if (getLength() > 16 && bmap.get(65)) {
            len = 192;
            bmap.clear(65);
        }
        return Math.min (getLength() << 1, len >> 2);
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        BitSet bmap = ISOUtil.hex2BitSet (new BitSet (64), readBytes (in, 16), 0);
        if (getLength() > 8 && bmap.get (1)) {
            ISOUtil.hex2BitSet (bmap, readBytes (in, 16), 64);
        }
        c.setValue(bmap);
    }
}
