package com.base.iso.interfaces;
import java.io.IOException;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.connector.filter.iBaseFilter;

/**
 * Source for an ISORequest (where to send a reply) 
 * @see ISORequestListener
 */
public interface iSource {
    /**
     * sends (or hands back) an ISOMsg
     * @param m the Message to be sent
     * @exception IOException
     * @exception BaseException
     * @exception iBaseFilter.VetoException;
     */
    public void send (ISOMsg m) 
        throws IOException, BaseException;

    /**
     * @return true if source is connected and usable
     */
    public boolean isConnected();
}
