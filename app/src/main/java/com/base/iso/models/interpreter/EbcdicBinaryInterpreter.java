package com.base.iso.models.interpreter;

import com.base.iso.util.ISOUtil;

/**
 * Implements EBCDIC Binary Interpreter. byte[] are converted to and from EBCDIC bytes. 
 */
@SuppressWarnings("unused")
public class EbcdicBinaryInterpreter implements BinaryInterpreter {
    public static final EbcdicBinaryInterpreter INSTANCE = new EbcdicBinaryInterpreter();

    /**
	 * (non-Javadoc)
	 *
     */
    public void interpret(byte[] data, byte[] b, int offset)
    {
        ISOUtil.asciiToEbcdic(data, b, offset);
    }

   public byte[] uninterpret(byte[] rawData, int offset, int length)
    {
        return ISOUtil.ebcdicToAsciiBytes(rawData, offset, length);
    }

   /**
    * @see com.base.iso.Interpreter#getPackedLength(int)
    */
    public int getPackedLength(int nDataUnits)
    {
        return nDataUnits;
    }
}
