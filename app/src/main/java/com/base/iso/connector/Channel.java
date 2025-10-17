package com.base.iso.connector;

import com.base.iso.core.ISOMsg;

/**
 * Channel interface 
 */
public interface Channel {
    /**
     * @param m message to send
     */
    public void send (ISOMsg m);
    /**
     * @return received message
     */
    public ISOMsg receive ();
    /**
     * @param timeout time to wait for a message
     * @return received message or null
     */
    public ISOMsg receive (long timeout);
}

