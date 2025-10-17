package com.base.iso.interfaces.connector;
/**
 * Tag this channel as a client one (from a Socket point of view)
 *
 * Please note that ISOChannel implementations may choose to
 * implement ClientChannel as well as ServerChannel, being a
 * client does not mean it can not be a server too. 
 * @see iConnector
 * @see iServerConnector
 */
public interface iClientConnector extends iConnector {
    /**
     * initialize an ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     */
    public void setHost(String host, int port);

    /**
     * @return hostname (may be null)
     */
    public String getHost();

    /**
     * @return port number (may be 0)
     */
    public int getPort();
}

