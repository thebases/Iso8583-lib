package com.base.iso.fields;
import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.interfaces.iPrefixer;
import com.base.iso.models.interpreter.BCDInterpreter;
import com.base.iso.models.interpreter.Interpreter;
import com.base.iso.models.prefixer.AsciiPrefixer;


/**
 * Length is represented in ASCII (as in IFA_LL*)
 * Value is represented in BCD
 * ISOFieldPackager Binary LLNUM
 *
 * @see BaseComponent
 */
public class IFA_LLBNUM extends aISOFieldBase {
    private Interpreter interpreter;
    private iPrefixer prefixer;
    
    public IFA_LLBNUM () {
        super();
        interpreter = BCDInterpreter.LEFT_PADDED;
        prefixer = AsciiPrefixer.LL;
    }
    /**
     * @param len - field len
     * @param description symbolic descrption
     */
    public  IFA_LLBNUM (int len, String description, boolean pad) {
        super(len, description);
        this.pad = pad;
        interpreter = pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED;
        prefixer = AsciiPrefixer.LL;
    }

    public void setPad(boolean pad)
    {
        this.pad = pad;
        interpreter = pad ? BCDInterpreter.LEFT_PADDED : BCDInterpreter.RIGHT_PADDED;
    }

    /**
     * @param c - a component
     * @return packed component
     * @exception BaseException
     */
    public byte[] pack (BaseComponent c) throws BaseException {
        String s = (String) c.getValue();
        int len = s.length();
        if (len > getLength() || len>99)   // paranoia settings
            throw new BaseException (
                "invalid len "+len +" packing IFA_LLBNUM field " + c.getKey()
            );

        byte[] b = new byte[2 + ((len+1) >> 1)];
        prefixer.encodeLength(len + 1 >> 1 << 1, b);
        interpreter.interpret(s, b, 2);
        return b;
    }

    /**
     * @param c - the Component to unpack
     * @param b - binary image
     * @param offset - starting offset within the binary image
     * @return consumed bytes
     * @exception BaseException
     */
    public int unpack (BaseComponent c, byte[] b, int offset)
        throws BaseException
    {
        int len = prefixer.decodeLength(b, offset);
        c.setValue (interpreter.uninterpret(b, offset + 2, len));
        return 2 + (++len >> 1);
    }
    public void unpack (BaseComponent c, InputStream in) 
        throws IOException, BaseException
    {
        int len = prefixer.decodeLength(readBytes (in, 2), 2);
        c.setValue (interpreter.uninterpret(readBytes (in, len+2 >> 1), 0, len));
    }
    public int getMaxPackedLength() {
        return 1 + (getLength()+1 >> 1);
    }
}
