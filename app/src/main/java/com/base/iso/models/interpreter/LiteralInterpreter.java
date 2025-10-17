package com.base.iso.models.interpreter;

import com.base.iso.util.ISOUtil;

/**
 * Implements a Literal Interpreter. No conversion is done. 
 */
public class LiteralInterpreter implements Interpreter
{
    /** An instance of this Interpreter. Only one needed for the whole system */
    public static final LiteralInterpreter INSTANCE = new LiteralInterpreter();

    /**
	 * (non-Javadoc)
	 *
     */
    @Override
    public void interpret(String data, byte[] b, int offset)
    {
        byte[] raw = data.getBytes(ISOUtil.CHARSET);
        System.arraycopy(raw, 0, b, offset, raw.length);
    }

    /**
	 * (non-Javadoc)
	 *
     */
    @Override
    public String uninterpret(byte[] rawData, int offset, int length) {
        return new String(rawData, offset, length, ISOUtil.CHARSET);
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
