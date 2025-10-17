package com.base.iso.models.interpreter;

/**
 * Implements Hex Interpreter. The Hex digits are stored in ASCII. 
 */
public class AsciiHexInterpreter implements BinaryInterpreter
{
    /** An instance of this Interpreter. Only one needed for the whole system */
    public static final AsciiHexInterpreter INSTANCE = new AsciiHexInterpreter();

    /** 0-15 to ASCII hex digit lookup table. */
    private static final byte[] HEX_ASCII = new byte[] {
              0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
              0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46
    };

    /**
     * Converts the binary data into ASCII hex digits.
     */
    public void interpret(byte[] data, byte[] b, int offset)
    {
        for (int i = 0; i < data.length; i++) {
            b[offset + i * 2] = HEX_ASCII[(data[i] & 0xF0) >> 4];
            b[offset + i * 2 + 1] = HEX_ASCII[data[i] & 0x0F];
        }
    }

    /**
     * Converts the ASCII hex digits into binary data.
     */
    public byte[] uninterpret(byte[] rawData, int offset, int length)
    {
        byte[] d = new byte[length];
        for (int i=0; i<length*2; i++) {
            int shift = i%2 == 1 ? 0 : 4;
            d[i>>1] |= Character.digit((char) rawData[offset+i], 16) << shift;
        }
        return d;
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
