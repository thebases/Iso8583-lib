package com.base.iso.interfaces.connector;

import java.io.IOException;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iSource;
import com.base.iso.interfaces.iPackager;

/**
 * allows the transmision and reception of ISO 8583 Messages 
 */
public interface iConnector extends iSource {
    public static final int CONNECT      = 0;
    public static final int TX           = 1;
    public static final int RX           = 2;
    public static final int SIZEOF_CNT   = 3;

    /**
     * Associate a packager with this channel
     * @param p     an ISOPackager
     */
    public void setPackager(iPackager p);

    /**
     * Connects ISOChannel 
     * @exception IOException
     */
    public void connect () throws IOException;

    /**
     * disconnects ISOChannel
     * @exception IOException
     */
    public void disconnect () throws IOException;

    /**
     * Reconnect channel
     * @exception IOException
     */
    public void reconnect() throws IOException;

    /**
     * @return true if Channel is connected and usable
     */
    public boolean isConnected();

    /**
     * Receives an ISOMsg
     * @return the Message received
     * @exception IOException
     * @exception BaseException
     */
    public ISOMsg receive() throws IOException, BaseException;

    /**
     * sends an ISOMsg over the TCP/IP session
     * @param m the Message to be sent
     * @exception IOException
     * @exception BaseException
     */
    public void send (ISOMsg m) throws IOException, BaseException;
    
    /**
     * sends a byte[] over the TCP/IP session
     * @param b the byte array to be sent
     * @exception IOException
     * @exception BaseException
     */
    public void send (byte[] b) throws IOException, BaseException;

    /**
     * @param b - usable state
     */
    public void setUsable(boolean b);

    /**
     * associates this ISOChannel with a name on NameRegistrar
     * @param name name to register
     */
    public void setName (String name);

   /**
    * @return this ISOChannel's name ("" if no name was set)
    */
    public String getName();

   /**
    * @return current packager
    */
    public iPackager getPackager();

   /**
    * Expose channel clonning interface
    */
    public Object clone();
    
}

