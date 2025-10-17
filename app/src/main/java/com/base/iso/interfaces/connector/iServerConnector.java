package com.base.iso.interfaces.connector;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Tag this channel as a server one (from a Socket point of view)
 *
 * Please note that ISOChannel implementations may choose to
 * implement ClientChannel as well as ServerChannel, being a
 * client does not mean it can not be a server too.
 * @see iConnector
 * @see iClientConnector
 */

public interface iServerConnector extends iConnector {
   /**
    * Accepts connection 
    * @exception IOException
    */
    public void accept(ServerSocket s) throws IOException;
}

