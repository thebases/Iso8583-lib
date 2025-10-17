package com.base.iso.fields.base;

import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iPadder;
import com.base.iso.interfaces.iPrefixer;
import com.base.iso.models.interpreter.Interpreter;
import com.base.iso.models.interpreter.LiteralInterpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;
import com.base.iso.models.prefixer.NullPrefixer;

/**
 * Generic class for handling string fields in Tag-Len-Value format
 * <code>
 * Format TTLL....
 * Where
 *       TT is the n>0 digit field number (Tag)
 *       LL is the n>=0 digit field length (if n=0 it's means fixed length field with prefixer)
 *       .. is the field content
 * </code> 
 */
public class TagStringFieldBase extends aISOFieldBase
{
    private iPrefixer tagPrefixer;
    private Interpreter interpreter;
    private iPadder padder;
    private iPrefixer prefixer;

    /**
     * Constructs a default TagStringFieldBase. There is ASCII tag L prefixer, no padding,
     * no length prefix and a literal interpretation. The set methods must be called to
     * make this ISOBaseFieldPackager useful.
     */
    public TagStringFieldBase() {
        super();
        this.tagPrefixer = AsciiPrefixer.L;
        this.interpreter = LiteralInterpreter.INSTANCE;
        this.prefixer = NullPrefixer.INSTANCE;
    }

    /**
     * Constructs an TagStringFieldBase with a specific Padder, Interpreter and Prefixer.
     * The length and description should be set with setLength() and setDescription methods.
     * @param tagPrefixer The type of tag prefixer used to encode tag.
     * @param padder The type of padding used.
     * @param interpreter The interpreter used to encode the field.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public TagStringFieldBase(iPrefixer tagPrefixer, iPadder padder,
                                        Interpreter interpreter, iPrefixer prefixer) {
        super();
        this.tagPrefixer = tagPrefixer;
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Creates an TagStringFieldBase.
     * @param maxLength The maximum length of the field in characters or bytes depending on the datatype.
     * @param description The description of the field. For human readable output.
     * @param tagPrefixer The type of tag prefixer used to encode tag.
     * @param interpreter The interpreter used to encode the field.
     * @param padder The type of padding used.
     * @param prefixer The type of length prefixer used to encode this field.
     */
    public TagStringFieldBase(int maxLength, String description, iPrefixer tagPrefixer,
                                  iPadder padder, Interpreter interpreter, iPrefixer prefixer) {
        super(maxLength, description);
        this.tagPrefixer = tagPrefixer;
        this.padder = padder;
        this.interpreter = interpreter;
        this.prefixer = prefixer;
    }

    /**
     * Sets the Padder.
     * @param padder The padder to use during packing and unpacking.
     */
    public void setPadder(iPadder padder) {
        this.padder = padder;
    }

    /**
     * Sets the Interpreter.
     * @param interpreter The interpreter to use in packing and unpacking.
     */
    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    /**
     * Sets the length prefixer.
     * @param prefixer The length prefixer to use during packing and unpacking.
     */
    public void setPrefixer(iPrefixer prefixer) {
        this.prefixer = prefixer;
    }

    /**
     * Returns the prefixer's packed length and the interpreter's packed length.
     */
    public int getMaxPackedLength() {
        return tagPrefixer.getPackedLength() + prefixer.getPackedLength() + interpreter.getPackedLength(getLength());
    }

    /**
     * Create a nice readable message for errors
     */
    private String makeExceptionMessage(BaseComponent c, String operation) {
        Object fieldKey = "unknown";
        if (c != null)
            try{
                fieldKey = c.getKey();
            } catch (Exception ignore){}
        return this.getClass().getName() + ": Problem " + operation + " field " + fieldKey;
    }

    /**
     * Convert the component into a byte[].
     */
    public byte[] pack(BaseComponent c) throws BaseException {
        try{
            String data = (String)c.getValue();
            if (data.length() > getLength())
                throw new BaseException("Field length " + data.length() + " too long. Max: " + getLength());

            int tag = (Integer)c.getKey();
            String paddedData = padder.pad(data, getLength());
            byte[] rawData = new byte[tagPrefixer.getPackedLength()+prefixer.getPackedLength()
                    + interpreter.getPackedLength(paddedData.length())];
            tagPrefixer.encodeLength(tag, rawData);
            byte[] rawLen = new byte[prefixer.getPackedLength()];
            prefixer.encodeLength(paddedData.length(), rawLen);
            System.arraycopy(rawLen, 0, rawData, tagPrefixer.getPackedLength(), rawLen.length);
            interpreter.interpret(paddedData, rawData, tagPrefixer.getPackedLength()+rawLen.length);
            return rawData;
        } catch(Exception e) {
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
    public int unpack(BaseComponent c, byte[] b, int offset) throws BaseException {
        try{
            int tagLen = tagPrefixer.getPackedLength();
            c.setFieldNumber(tagPrefixer.decodeLength(b, offset));
            int len = prefixer.decodeLength(b, offset + tagLen);
            if (len == -1) {
                // The prefixer doesn't know how long the field is, so use
                // maxLength instead
                len = getLength();
            }
            else if (getLength() > 0 && len > getLength())
                throw new BaseException("Field length " + len + " too long. Max: " + getLength());
            
            int lenLen = prefixer.getPackedLength();
            String unpacked = interpreter.uninterpret(b, offset + tagLen + lenLen, len);
            c.setValue(unpacked);
            return tagLen + lenLen + interpreter.getPackedLength(len);
        } catch(Exception e){
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
    }

    /**
     * Unpack the input stream into the component.
     * @param c  The Component to unpack into.
     * @param in Input stream where the packed bytes come from.
     * @exception IOException Thrown if there's a problem reading the input stream.
     */
    @Override
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException {
        try{
            int tagLen = tagPrefixer.getPackedLength();
            c.setFieldNumber(tagPrefixer.decodeLength(readBytes (in, tagLen), 0));
            int lenLen = prefixer.getPackedLength ();
            int len;
            if (lenLen == 0)
                len = getLength();
            else {
                len = prefixer.decodeLength (readBytes (in, lenLen), 0);
                if (getLength() > 0 && len > 0 && len > getLength())
                    throw new BaseException("Field length " + len + " too long. Max: " + getLength());
            }
            int packedLen = interpreter.getPackedLength(len);
            String unpacked = interpreter.uninterpret(readBytes (in, packedLen), 0, len);
            c.setValue(unpacked);
        } catch(BaseException e){
            throw new BaseException(makeExceptionMessage(c, "unpacking"), e);
        }
    }

}
