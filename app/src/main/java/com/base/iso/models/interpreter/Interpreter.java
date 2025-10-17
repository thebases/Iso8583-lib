package com.base.iso.models.interpreter;

import com.base.iso.core.BaseException;

/**
 * Implementations convert Strings into byte arrays and vice versa. 
 */
public interface Interpreter
{
    /**
	 * Converts the string data into a different interpretation. Standard
	 * interpretations are ASCII, EBCDIC, BCD and LITERAL.
	 * 
     * @throws BaseException on error
	 */
    void interpret(String data, byte[] b, int offset) throws BaseException;

    /**
	 * Converts the byte array into a String. This reverses the interpret
	 * method.
	 * 
	 * @param rawData
	 *            The interpreted data.
	 * @param offset
	 *            The index in rawData to start interpreting at.
	 * @param length
	 *            The number of data units to interpret.
     * @throws BaseException on error
	 * @return The uninterpreted data.
	 */
    String uninterpret(byte[] rawData, int offset, int length) throws BaseException;

    /**
	 * Returns the number of bytes required to interpret a String of length
	 * nDataUnits.
	 */
    int getPackedLength(int nDataUnits);
}
