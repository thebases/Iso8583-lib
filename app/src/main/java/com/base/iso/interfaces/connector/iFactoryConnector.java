package com.base.iso.interfaces.connector;

import com.base.iso.interfaces.iClientSocketFactory;

/**
 * Channels that can use socket factories need to implement this. 
 */

public interface iFactoryConnector {
    /**
     * @param sfac a socket factory
     */
    public void setSocketFactory(iClientSocketFactory sfac);
}
