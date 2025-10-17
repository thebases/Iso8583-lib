package com.base.iso.interfaces;

import com.base.iso.core.BaseException;

/**
 * This interface is used to encode and decode length prefixes. 
 */
public interface iPrefixer
{
    /**
	 * Fills a byte array with the field length data in raw form.
	 * 
	 * @param length
	 *            The length to be encoded.
	 * @param b
	 *            The byte array to fill with the encoded length.
	 */
    void encodeLength(int length, byte[] b) throws BaseException;

    /**
	 * Decodes an encoded length.
	 * 
	 * @param b
	 *            The byte array to scan for the length.
	 * @param offset
	 *            The offset to start scanning from.
	 * @return The length in chars of the field data to follow this
	 *         LengthPrefix.
	 */
    int decodeLength(byte[] b, int offset) throws BaseException;

    /**
	 * Returns the number of bytes taken up by the length encoding.
	 */
    int getPackedLength();
}