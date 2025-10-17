package com.base.iso.models.prefixer;

import com.base.iso.interfaces.iPrefixer;

/**
 * AsciiPrefixer constructs a prefix for ASCII messages. 
 */
public class NullPrefixer implements iPrefixer
{
    /** A handy instance of the null prefixer. */
    public static final NullPrefixer INSTANCE = new NullPrefixer();

    /** Hidden constructor */
    private NullPrefixer()
    {}

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.base.iso.Prefixer#encodeLength(int, byte[])
	 */
    public void encodeLength(int length, byte[] b)
    {}

    /**
	 * Returns -1 meaning there is no length field.
	 *
     */
    public int decodeLength(byte[] b, int offset)
    {
        return -1;
    }

    /**
     */
    public int getPackedLength()
    {
        return 0;
    }
}
