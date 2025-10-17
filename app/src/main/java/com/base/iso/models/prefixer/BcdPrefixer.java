package com.base.iso.models.prefixer;

import com.base.iso.interfaces.iPrefixer;

/**
 * BcdPrefixer constructs a prefix storing the length in BCD. 
 */
@SuppressWarnings("unused")
public class BcdPrefixer implements iPrefixer
{
    /**
     * A length prefixer for up to 9 chars. The length is encoded with 1 BCD digit.
     */
    public static final BcdPrefixer L = new BcdPrefixer(1);
    /**
	 * A length prefixer for up to 99 chars. The length is encoded with 2 BCD digits.
	 */
    public static final BcdPrefixer LL = new BcdPrefixer(2);
    /**
	 * A length prefixer for up to 999 chars. The length is encoded with 3 BCD digits.
	 */
    public static final BcdPrefixer LLL = new BcdPrefixer(3);
    /**
	 * A length prefixer for up to 9999 chars. The length is encoded with 4 BCD digits.
	 */
    public static final BcdPrefixer LLLL = new BcdPrefixer(4);
    /**
     * A length prefixer for up to 99999 chars. The length is encoded with 5 BCD digits.
     */
    public static final BcdPrefixer LLLLL = new BcdPrefixer(5);
    /**
    * A length prefixer for up to 999999 chars. The length is encoded with 6 BCD digits.
    */
    public static final BcdPrefixer LLLLLL = new BcdPrefixer(6);

    /** The number of digits allowed to express the length */
    private int nDigits;

    public BcdPrefixer(int nDigits)
    {
        this.nDigits = nDigits;
    }

    @Override
    public void encodeLength(int length, byte[] b)
    {
        for (int i = getPackedLength() - 1; i >= 0; i--) {
            int twoDigits = length % 100;
            length /= 100;
            b[i] = (byte)((twoDigits / 10 << 4) + twoDigits % 10);
        }
    }

    @Override
    public int decodeLength(byte[] b, int offset)
    {
        int len = 0;
        for (int i = 0; i < (nDigits + 1) / 2; i++)
        {
            len = 100 * len + ((b[offset + i] & 0xF0) >> 4) * 10 + (b[offset + i] & 0x0F);
        }
        return len;
    }

    @Override
    public int getPackedLength()
    {
        return nDigits + 1 >> 1;
    }
}
