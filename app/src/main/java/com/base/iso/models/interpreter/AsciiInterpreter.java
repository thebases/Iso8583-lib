package com.base.iso.models.interpreter;

import com.base.iso.util.ISOUtil;

/**
 * Implements ASCII Interpreter. Strings are converted to and from ASCII bytes.
 * This uses the US-ASCII encoding which all JVMs must support. 
 */
public class AsciiInterpreter implements Interpreter
{
    /** An instance of this Interpreter. Only one needed for the whole system */
    public static final AsciiInterpreter INSTANCE = new AsciiInterpreter();

    /**
	 * (non-Javadoc)
	 *
     */
    @Override
    public void interpret(String data, byte[] b, int offset)
    {
        System.arraycopy(data.getBytes(ISOUtil.CHARSET), 0, b, offset, data.length());
    }

    /**
	 * (non-Javadoc)
	 *
     */
    @Override
    public String uninterpret (byte[] rawData, int offset, int length) {
        byte[] ret = new byte[length];
        try {
            System.arraycopy(rawData, offset, ret, 0, length);
            return new String(ret, ISOUtil.CHARSET);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(
                String.format("Required %d but just got %d bytes", length, rawData.length-offset)
            );
        }
    }

    /**
	 * (non-Javadoc)
	 *
     */
    @Override
    public int getPackedLength(int nDataUnits)
    {
        return nDataUnits;
    }
}
