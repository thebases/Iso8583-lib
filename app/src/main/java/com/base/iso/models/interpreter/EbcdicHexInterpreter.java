
package com.base.iso.models.interpreter;

/**
 * Implements Hex Interpreter. The Hex digits are stored in EBCDIC. 
 */
public class EbcdicHexInterpreter implements BinaryInterpreter
{
    /** An instance of this Interpreter. Only one needed for the whole system */
    public static final EbcdicHexInterpreter INSTANCE = new EbcdicHexInterpreter();

    /** 0-15 to EBCDIC hex digit lookup table. */
    private static final byte[] HEX_EBCDIC = new byte[] {
              (byte)0xF0, (byte)0xF1, (byte)0xF2, (byte)0xF3, 
              (byte)0xF4, (byte)0xF5, (byte)0xF6, (byte)0xF7,
              (byte)0xF8, (byte)0xF9, (byte)0xC1, (byte)0xC2, 
              (byte)0xC3, (byte)0xC4, (byte)0xC5, (byte)0xC6
    };

    /**
     * Converts the binary data into EBCDIC hex digits.
     */
    public void interpret(byte[] data, byte[] b, int offset)
    {
        for (int i = 0; i < data.length; i++) {
            b[offset + i * 2] = HEX_EBCDIC[(data[i] & 0xF0) >> 4];
            b[offset + i * 2 + 1] = HEX_EBCDIC[data[i] & 0x0F];
        }
    }

    /**
     * Converts the EBCDIC hex digits into binary data.
     */
    public byte[] uninterpret(byte[] rawData, int offset, int length)
    {
        byte[] ret = new byte[length];
        for (int i = 0; i < length; i++)
        {
        	//TODO: what if the data is not EBCDIC? validation is required.
            byte hi = rawData[offset + i * 2];
            byte lo = rawData[offset + i * 2 + 1];
            int h = hi < 0xF0 ? 10 + hi - 0xC0 : hi - 0xF0;
            int l = lo < 0xF0 ? 10 + lo - 0xC0 : lo - 0xF0;
            ret[i] = (byte)(h << 4 | l);
        }
        return ret;
    }

    /**
     * Returns double nBytes because the hex representation of 1 byte needs 2 hex digits.
     *
     */
    public int getPackedLength(int nBytes)
    {
        return nBytes * 2;
    }
}
