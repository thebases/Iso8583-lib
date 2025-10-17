package com.base.iso.models.padder;

import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iPadder;

/**
 * Implements the Padder interface for padding strings and byte arrays on the
 * Right.
 * 
 * @author joconnor
 * @version $Revision$ $Date$
 */
public class RightPadder implements iPadder
{
    /**
	 * A padder for padding spaces on the right. This is very common in
	 * alphabetic fields.
	 */
    public static final RightPadder SPACE_PADDER = new RightPadder(' ');

    private char pad;

    /**
	 * Creates a Right Padder with a specific pad character.
	 * 
	 * @param pad
	 *            The padding character. For binary padders, the pad character
	 *            is truncated to lower order byte.
	 */
    public RightPadder(char pad)
    {
        this.pad = pad;
    }

    public String pad(String data, int maxLength) throws BaseException
    {
        int len = data.length();

        if (len < maxLength) {
            StringBuilder padded = new StringBuilder(maxLength);
            padded.append(data);
            for (; len < maxLength; len++) {
                padded.append(pad);
            }
            data = padded.toString();
        }
        else if (len > maxLength) {
            throw new BaseException("Data is too long. Max = " + maxLength);
        }
        return data;
    }

    public String unpad(String paddedData)
    {
        int len = paddedData.length();
        for (int i = len; i > 0; i--)
        {
            if (paddedData.charAt(i - 1) != pad)
            {
                return paddedData.substring(0, i);
            }
        }
        return "";
    }
}
