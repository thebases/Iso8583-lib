
package com.base.iso.util.ssl;

import java.io.IOException;
import java.net.ServerSocket;

import com.base.iso.core.BaseException;

/**
 * <code>ISOServerSocketFactory</code> is used by BaseChannel and ISOServer
 * in order to provide hooks for SSL implementations. 
 */
public interface ISOServerSocketFactory {
    /**
    * Create a server socket on the specified port (port 0 indicates
    * an anonymous port).
    * @param  port the port number
    * @return the server socket on the specified port
    * @exception IOException should an I/O error occur
    * @exception BaseException on any other error
    * creation
    */
    public ServerSocket createServerSocket(int port)
        throws IOException, BaseException;
}
