package com.base.iso.models.padder;

import com.base.iso.interfaces.iPadder;

/**
 * The NullPadder does not pad. It is a utility class to use Null Object
 * pattern. 
 */
public class NullPadder implements iPadder
{
    /** The only instance you need */
    public static final NullPadder INSTANCE = new NullPadder();

    /**
     */
    public String pad(String data, int maxLength)
    {
        return data;
    }

    /**
	 * (non-Javadoc)
	 *
     */
    public String unpad(String paddedData)
    {
        return paddedData;
    }
}
