package com.base.iso.models.prefixer;

import com.base.iso.interfaces.iPrefixer;

/**
 * HexNibblesPrefixer constructs a prefix storing the length in BCD. 
 */
@SuppressWarnings("unused")
public class HexNibblesPrefixer implements iPrefixer {
    public static final HexNibblesPrefixer LL = new HexNibblesPrefixer(2);
    public static final HexNibblesPrefixer LLL = new HexNibblesPrefixer(3);
    private int nDigits;

    public HexNibblesPrefixer(int nDigits) {
        this.nDigits = nDigits;
    }

    @Override
    public void encodeLength(int length, byte[] b) {
        length <<= 1;
        for (int i = getPackedLength() - 1; i >= 0; i--) {
            int twoDigits = length % 100;
            length /= 100;
            b[i] = (byte)((twoDigits / 10 << 4) + twoDigits % 10);
        }
    }

    @Override
    public int decodeLength(byte[] b, int offset) {
        int len = 0;
        for (int i = 0; i < (nDigits + 1) / 2; i++)
        {
            len = 100 * len + ((b[offset + i] & 0xF0) >> 4) * 10 + (b[offset + i] & 0x0F);
        }
        return len >> 1;
    }

    @Override
    public int getPackedLength()
    {
        return nDigits + 1 >> 1;
    }
}
