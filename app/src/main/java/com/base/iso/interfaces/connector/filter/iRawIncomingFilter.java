
package com.base.iso.interfaces.connector.filter;

import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.util.log.LogEvent;

/**
 * Receives the header and binary image of an incoming message 
 * (suitable for MAC validation) 
 */
public interface iRawIncomingFilter extends iBaseFilter {
    /**
     * @param channel current ISOChannel instance
     * @param m ISOMsg to filter
     * @param header optional header 
     * @param image raw image
     * @param evt LogEvent
     * @return an ISOMsg (possibly parameter m)
     * @throws VetoException
     */
    public ISOMsg filter (iConnector channel, ISOMsg m, 
            byte[] header, byte[] image, LogEvent evt) 
        throws VetoException;
}

