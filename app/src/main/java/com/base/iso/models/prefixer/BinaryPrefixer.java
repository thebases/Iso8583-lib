package com.base.iso.models.prefixer;

import com.base.iso.interfaces.iPrefixer;

/**
 * BinaryPrefixer constructs a prefix storing the length in binary. 
 */
public class BinaryPrefixer implements iPrefixer
{
    /**
	 * A length prefixer for upto 255 chars. The length is encoded with 1 unsigned byte.
	 */
    public static final BinaryPrefixer B = new BinaryPrefixer(1);

    /**
     * A length prefixer for upto 65535 chars. The length is encoded with 2 unsigned bytes.
     */
    public static final BinaryPrefixer BB = new BinaryPrefixer(2);

    /** The number of digits allowed to express the length */
    private int nBytes;

    public BinaryPrefixer(int nBytes)
    {
        this.nBytes = nBytes;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.base.iso.Prefixer#encodeLength(int, byte[])
	 */
    public void encodeLength(int length, byte[] b)
    {
        for (int i = nBytes - 1; i >= 0; i--) {
            b[i] = (byte)(length & 0xFF);
            length >>= 8;
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.base.iso.Prefixer#decodeLength(byte[], int)
	 */
    public int decodeLength(byte[] b, int offset)
    {
        int len = 0;
        for (int i = 0; i < nBytes; i++)
        {
            len = 256 * len + (b[offset + i] & 0xFF);
        }
        return len;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.base.iso.Prefixer#getLengthInBytes()
	 */
    public int getPackedLength()
    {
        return nBytes;
    }
}