package com.base.iso.interfaces;

import java.io.IOException;
import java.net.Socket;

import com.base.iso.core.BaseException;

/**
 * <code>ISOClientSocketFactory</code> is used by BaseChannel and ISOServer
 * in order to provide hooks for SSL implementations. 
 */
public interface iClientSocketFactory {
    /**
    * Create a client socket connected to the specified host and port.
    * @param  host   the host name
    * @param  port   the port number
    * @return a socket connected to the specified host and port.
    * @exception IOException should an I/O error occur
    * @exception BaseException on any other error
    */
    public Socket createSocket(String host, int port)
        throws IOException, BaseException;
}
