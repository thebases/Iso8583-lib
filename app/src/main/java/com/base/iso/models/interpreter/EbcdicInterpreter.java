
package com.base.iso.models.interpreter;

import com.base.iso.util.ISOUtil;

/**
 * Implements EBCDIC Interpreter. Strings are converted to and from EBCDIC
 * bytes. 
 */
public class EbcdicInterpreter implements Interpreter
{
    /** An instance of this Interpreter. Only one needed for the whole system */
    public static final EbcdicInterpreter INSTANCE = new EbcdicInterpreter();

    /**
	 * (non-Javadoc)
	 *
     */
    public void interpret(String data, byte[] b, int offset)
    {
        ISOUtil.asciiToEbcdic(data, b, offset);
    }

    /**
	 * (non-Javadoc)
	 *
     */
    public String uninterpret(byte[] rawData, int offset, int length)
    {
        return ISOUtil.ebcdicToAscii(rawData, offset, length);
    }

    /**
	 * (non-Javadoc)
	 *
     */
    public int getPackedLength(int nDataUnits)
    {
        return nDataUnits;
    }
}
