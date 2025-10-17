package com.base.iso.interfaces;

import com.base.iso.core.BaseException;

/**
 * An interface for padding and unpadding strings and byte arrays. 
 */
public interface iPadder
{
    /**
	 * Returns a padded string upto a maximum length. If the data is longer
	 * than maxLength, then the data is truncated.
	 * 
	 * @param data
	 *            The string to pad.
	 * @param maxLength
	 *            The maximum length of the padded string.
	 * @return A padded string.
     * @throws BaseException on error
	 */
    String pad(String data, int maxLength) throws BaseException;

    /**
	 * Removes the padding from a padded string.
	 * 
	 * @param paddedData
	 *            The string to unpad.
	 * @return The unpadded string.
     * @throws BaseException on error
	 */
    String unpad(String paddedData) throws BaseException;
}