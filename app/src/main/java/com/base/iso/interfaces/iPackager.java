package com.base.iso.interfaces;

import java.io.IOException;
import java.io.InputStream;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;

/** 
 * @see BaseComponent
 */
public interface iPackager {
    /**
     * @param   m   the Component to pack
     * @return      Message image
     * @exception BaseException on error
     */
    public byte[] pack (BaseComponent m) throws BaseException;

    /**
     * @param   m   the Container of this message
     * @param   b   ISO message image
     * @return      consumed bytes
     * @exception BaseException on error
     */
    public int unpack (BaseComponent m, byte[] b) throws BaseException;

    public void unpack (BaseComponent m, InputStream in) throws IOException, BaseException;

    /**
     * @return  Packager's Description
     */
    public String getDescription();
    
    /**
     * @param   m   the Container (i.e. an ISOMsg)
     * @param   fldNumber the Field Number
     * @return  Field Description
     */
    public String getFieldDescription(BaseComponent m, int fldNumber);

    /**
     * @return an ISOMsg
     */
    public ISOMsg createISOMsg ();
}

