package com.base.iso.models.prefixer;

import com.base.iso.interfaces.iPrefixer;

/**
 * EbcdicPrefixer constructs a prefix for EBCDIC messages. 
 */
public class EbcdicPrefixer implements iPrefixer
{
    /**
     * A length prefixer for upto 99 chars. The length is encoded with 2 EBCDIC
     * chars representing 2 decimal digits.
     */
    public static final EbcdicPrefixer LL = new EbcdicPrefixer(2);
    /**
     * A length prefixer for upto 999 chars. The length is encoded with 3 EBCDIC
     * chars representing 3 decimal digits.
     */
    public static final EbcdicPrefixer LLL = new EbcdicPrefixer(3);
    /**
     * A length prefixer for upto 9999 chars. The length is encoded with 4
     * EBCDIC chars representing 4 decimal digits.
     */
    public static final EbcdicPrefixer LLLL = new EbcdicPrefixer(4);

    private static byte[] EBCDIC_DIGITS = {(byte)0xF0, (byte)0xF1, (byte)0xF2,
        (byte)0xF3, (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7, (byte)0xF8, 
        (byte)0xF9}; 

    /** The number of digits allowed to express the length */
    private int nDigits;

    public EbcdicPrefixer(int nDigits)
    {
        this.nDigits = nDigits;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.base.iso.Prefixer#encodeLength(int, byte[])
	 */
    public void encodeLength(int length, byte[] b)
    {
        for (int i = nDigits - 1; i >= 0; i--)
        {
            b[i] = EBCDIC_DIGITS[length % 10];
            length /= 10;
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
        for (int i = 0; i < nDigits; i++)
        {
            len = len * 10 + (b[offset + i] & 0x0F);
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
        return nDigits;
    }
}