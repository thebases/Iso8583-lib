package com.base.iso.fields.base;

import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iPadder;
import com.base.iso.interfaces.iPrefixer;
import com.base.iso.models.interpreter.Interpreter;
import com.base.iso.models.interpreter.LiteralInterpreter;
import com.base.iso.models.padder.NullPadder;
import com.base.iso.models.prefixer.NullPrefixer;
import com.base.iso.util.ISOUtil;

 
public class StringFieldBase extends aISOFieldBase
{
    private Interpreter interpreter;
    private iPadder padder;
    private iPrefixer prefixer;

    /**
     * Constructs a default StringFieldBase. There is no padding,
     * no length prefix and a literal interpretation. The set methods must be called to
     * make this ISOBaseFieldPackager useful.
     */
    public StringFieldBase()
    {
        super();
        this.padder = NullPadder.INSTANCE;
        this.interpreter = LiteralInterpreter.INSTANCE;
        this.prefixer = NullPrefixer.INSTANCE;
    }

    /**
     * Constructs an StringFieldBase with a specific Padder, Interpreter and Prefixer.
     * The length and description should be set with setLength() and setDescription methods.
     * @param padder The type of padding used.
     * @param interpreter The interpreter used to encode the field.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public StringFieldBase(iPadder padder, Interpreter interpreter, iPrefixer prefixer)
    {
        super();
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Creates an StringFieldBase.
     * @param maxLength The maximum length of the field in characters or bytes depending on the datatype.
     * @param description The description of the field. For human readable output.
     * @param interpreter The interpreter used to encode the field.
     * @param padder The type of padding used.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public StringFieldBase(int maxLength, String description, iPadder padder,
                                  Interpreter interpreter, iPrefixer prefixer)
    {
        super(maxLength, description);
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Sets the Padder.
     * @param padder The padder to use during packing and unpacking.
     */
    public void setPadder(iPadder padder)
    {
        this.padder = padder;
    }

    /**
     * Sets the Interpreter.
     * @param interpreter The interpreter to use in packing and unpacking.
     */
    public void setInterpreter(Interpreter interpreter)
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

    /**
     * Returns the prefixer's packed length and the interpreter's packed length.
     */
    public int getMaxPackedLength()
    {
        return prefixer.getPackedLength() + interpreter.getPackedLength(getLength());
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
        return this.getClass().getName() + ": Problem " + operation + " field " + fieldKey;
    }

    /**
     * Convert the component into a byte[].
     * @return byte array representation of component
     * @throws com.base.iso.BaseException
	 */
    @Override
    public byte[] pack(BaseComponent c) throws BaseException
    {
        try
        {
            String data;
            if(c.getValue() instanceof byte[])
                data = new String(c.getBytes(), ISOUtil.CHARSET); // transparent handling of complex fields
            else
                data = (String)c.getValue();

            if (data.length() > getLength())
            {
                throw new BaseException("Field length " + data.length() + " too long. Max: " + getLength());
            }
            String paddedData = padder.pad(data, getLength());
            byte[] rawData = new byte[prefixer.getPackedLength()
                    + interpreter.getPackedLength(paddedData.length())];
            prefixer.encodeLength(paddedData.length(), rawData);
            interpreter.interpret(paddedData, rawData, prefixer.getPackedLength());
            return rawData;
        } catch(Exception e)
        {
            throw new BaseException(makeExceptionMessage(c, "packing"), e);
        }
    }

    /**
     * Unpacks the byte array into the component.
     * @param c The component to unpack into.
     * @param b The byte array to unpack.
     * @param offset The index in the byte array to start unpacking from.
     * @return The number of bytes consumed unpacking the component.
     */
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
            String unpacked = interpreter.uninterpret(b, offset + lenLen, len);
            c.setValue(unpacked);
            return lenLen + interpreter.getPackedLength(len);
        } catch(Exception e)
        {
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
    }

    /**
     * Unpack the input stream into the component.
     * @param c  The Component to unpack into.
     * @param in Input stream where the packed bytes come from.
     * @exception IOException Thrown if there's a problem reading the input stream.
     */
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
            String unpacked = interpreter.uninterpret(readBytes (in, packedLen), 0, len);
            c.setValue(unpacked);
        } catch(BaseException e)
        {
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
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
