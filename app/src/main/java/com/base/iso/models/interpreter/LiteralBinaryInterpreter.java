package com.base.iso.models.interpreter;
/**
 * This interpreter does no conversion and leaves the input the same as the output.
 * @author jonathan.oconnor@xcom.de
 */
public class LiteralBinaryInterpreter implements BinaryInterpreter
{
    /**
     * The only instance of this interpreter.
     */
    public static final LiteralBinaryInterpreter INSTANCE = new LiteralBinaryInterpreter();

    /**
     * Private constructor so we don't allow multiple instances.
     */
    private LiteralBinaryInterpreter()
    {
    }

    /**
     * Copies the input to the output.
     */
    public void interpret(byte[] data, byte[] b, int offset)
    {
        System.arraycopy(data, 0, b, offset, data.length);
    }

    /**
     * Copies the data out of the byte array.
     */
    public byte[] uninterpret(byte[] rawData, int offset, int length)
    {
        byte[] ret = new byte[length];
        System.arraycopy(rawData, offset, ret, 0, length);
        return ret;
    }

    /**
     * Returns nBytes because we are not doing any conversion.
     */
    public int getPackedLength(int nBytes)
    {
        // TODO Auto-generated method stub
        return nBytes;
    }
}
