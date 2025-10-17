package com.base.iso.fields.base;

import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BinaryFieldComponent;
import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iPrefixer;
import com.base.iso.models.interpreter.BinaryInterpreter;
import com.base.iso.models.interpreter.LiteralBinaryInterpreter;
import com.base.iso.models.prefixer.NullPrefixer;
 
public class BinaryFieldBase extends aISOFieldBase
{
    private BinaryInterpreter interpreter;
    private iPrefixer prefixer;

    /**
     * Constructs a default BinaryFieldBase. There is no length prefix and a
     * literal interpretation. The set methods must be called to make this
     * BinaryFieldBase useful.
     */
    public BinaryFieldBase()
    {
        super();
        this.interpreter = LiteralBinaryInterpreter.INSTANCE;
        this.prefixer = NullPrefixer.INSTANCE;
    }

    /**
     * Creates an BinaryFieldBase.
     * @param maxLength The maximum length of the field in characters or bytes depending on the datatype.
     * @param description The description of the field. For human readable output.
     * @param interpreter The interpreter used to encode the field.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public BinaryFieldBase(int maxLength, String description,
                                  BinaryInterpreter interpreter, iPrefixer prefixer)
    {
        super(maxLength, description);
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Creates an BinaryFieldBase.
     * @param interpreter The interpreter used to encode the field.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public BinaryFieldBase(BinaryInterpreter interpreter, iPrefixer prefixer)
    {
        super();
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Sets the Interpreter.
     * @param interpreter The interpreter to use in packing and unpacking.
     */
    public void setInterpreter(BinaryInterpreter interpreter)
    {
        this.interpreter = interpreter;
    }

    /**
     * Sets the length prefixer.
     * @param prefixer The length prefixer to use during packing and unpacking.
     */
    public void setPrefixer(iPrefixer prefixer)
    {
        this.prefixer = prefixer;
    }

    public int getMaxPackedLength()
    {
        return prefixer.getPackedLength() + interpreter.getPackedLength(getLength());
    }

    /**
	 * Convert the component into a byte[].
	 */
    public byte[] pack(BaseComponent c) throws BaseException
    {
        try
        {
            byte[] data = c.getBytes();
            int packedLength = prefixer.getPackedLength();
            if (packedLength == 0)
            {
                if (data.length != getLength())
                {
                    throw new BaseException("Binary data length not the same as the packager length (" + data.length + "/" + getLength() + ")");
                }
            }
            byte[] ret = new byte[interpreter.getPackedLength(data.length) + packedLength];
            prefixer.encodeLength(data.length, ret);
            interpreter.interpret(data, ret, packedLength);
            return ret;
        } catch(Exception e)
        {
            throw new BaseException(makeExceptionMessage(c, "packing"), e);
        }
    }

    public int unpack(BaseComponent c, byte[] b, int offset) throws BaseException
    {
        try
        {
            int len = prefixer.decodeLength(b, offset);
            if (len == -1) {
                // The prefixer doesn't know how long the field is, so use
                // maxLength instead
                len = getLength();
            }
            else if (getLength() > 0 && len > getLength())
                throw new BaseException("Field length " + len + " too long. Max: " + getLength());
            int lenLen = prefixer.getPackedLength();
            byte[] unpacked = interpreter.uninterpret(b, offset + lenLen, len);
            c.setValue(unpacked);
            return lenLen + interpreter.getPackedLength(len);
        } catch(Exception e)
        {
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
    }

    /** Unpack from an input stream */
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        try
        {
            int lenLen = prefixer.getPackedLength ();
            int len;
            if (lenLen == 0)
            {
                len = getLength();
            } else
            {
                len = prefixer.decodeLength (readBytes (in, lenLen), 0);
                if (getLength() > 0 && len > 0 && len > getLength())
                    throw new BaseException("Field length " + len + " too long. Max: " + getLength());
            }
            int packedLen = interpreter.getPackedLength(len);
            byte[] unpacked = interpreter.uninterpret(readBytes (in, packedLen), 0, len);
            c.setValue(unpacked);
        } catch(BaseException e)
        {
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
    }

    /**
     * component factory
     * @param fieldNumber - the field number
     * @return the newly created component
     */
    public BaseComponent createComponent(int fieldNumber) {
        return new BinaryFieldComponent (fieldNumber);
    }

    /** Create a nice readable message for errors */
    private String makeExceptionMessage(BaseComponent c, String operation) {
        Object fieldKey = "unknown";
        if (c != null)
        {
            try
            {
                fieldKey = c.getKey();
            } catch (Exception ignore)
            {
            }
        }
        return getClass().getName() + ": Problem " + operation + " field " + fieldKey;
    }


    /**
     * Checks the length of the data against the maximum, and throws an IllegalArgumentException.
     * This is designed to be called from field Packager constructors and the setLength()
     * method.
     * @param len The length of the data for this field packager.
     * @param maxLength The maximum length allowed for this type of field packager.
     *          This depends on the prefixer that is used.
     * @throws IllegalArgumentException If len > maxLength.
     */
    protected void checkLength(int len, int maxLength) throws IllegalArgumentException
    {
        if (len > maxLength)
        {
            throw new IllegalArgumentException("Length " + len + " too long for " + getClass().getName());
        }
    }
}
