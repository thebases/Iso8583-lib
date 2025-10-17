package com.base.iso.models.padder;

import com.base.iso.core.BaseException;

/**
 * Implements the Padder interface for padding strings and byte arrays on the
 * Right. The difference between this and RightPadder is that this truncates the data
 * during packing, instead of throwing an exception. 
 */
public class RightTPadder extends RightPadder
{
    /**
	 * A padder for padding spaces on the right. This is very common in
	 * alphabetic fields.
	 */
    public static final RightTPadder SPACE_PADDER = new RightTPadder(' ');

    /**
	 * Creates a Right Truncating Padder with a specific pad character.
	 * 
	 * @param pad
	 *            The padding character. For binary padders, the pad character
	 *            is truncated to lower order byte.
	 */
    public RightTPadder(char pad)
    {
        super(pad);
    }

    /**
	 * @see com.base.iso.interfaces.iPadder#pad(java.lang.String, int)
	 */
    public String pad(String data, int maxLength) throws BaseException
    {
        if (data.length() > maxLength)
        {
            return super.pad(data.substring(0,maxLength), maxLength);
        } else
        {
            return super.pad(data, maxLength);
        }
    }
}