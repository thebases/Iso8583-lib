package com.base.iso.connector;


import java.io.IOException;
import java.net.ServerSocket;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/**
 * Sends a four ASCII hex characters indicating message length (up to 0xffff) 
 * @see ISOMsg
 * @see BaseException
 * @see iConnector
 */
public class HEXConnector extends BaseConnector {
    public HEXConnector () {
        super();
    }
    /**
     * Construct client connector
     * @param host  server TCP Address
     * @param port  server port number
     * @param p     an iPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @see iPackager
     */
    public HEXConnector (String host, int port, iPackager p, byte[] TPDU) {
        super(host, port, p);
        this.header = TPDU;
    }
    /**
     * Construct server connector
     * @param p     an iPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @exception IOException
     * @see iPackager
     */
    public HEXConnector (iPackager p, byte[] TPDU) throws IOException {
        super(p);
        this.header = TPDU;
    }
    /**
     * constructs server connector associated with a Server Socket
     * @param p     an iPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see iPackager
     */
    public HEXConnector (iPackager p, byte[] TPDU, ServerSocket serverSocket) 
        throws IOException
    {
        super(p, serverSocket);
        this.header = TPDU;
    }
    protected void sendMessageLength(int len) throws IOException {
        if (len > 0xFFFF)
            throw new IOException (len + " exceeds maximum length");
        try {
            serverOut.write (
                ISOUtil.zeropad (Integer.toString (len % 0xFFFF,16), 4).getBytes()
            );
        } 
        catch (BaseException e) {
            Logger.log (new LogEvent (this, "send-message-length", e));
        }
    }
    protected int getMessageLength() throws IOException, BaseException {
        byte[] b = new byte[4];
        serverIn.readFully(b,0,4);
        try {
            return Integer.parseInt (new String(b),16);
        } catch (NumberFormatException e) {
            // T9: stream position is now unknown → drop the connection instead of desyncing
            closeSocket();
            throw new BaseException ("invalid length prefix: " + ISOUtil.hexString (b), e);
        }
    }
}

