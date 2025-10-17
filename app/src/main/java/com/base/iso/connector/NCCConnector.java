package com.base.iso.connector;


import java.io.IOException;
import java.net.ServerSocket;

import com.base.iso.config.Configuration;
import com.base.iso.config.ConfigurationException;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/**
 * Talks with TCP based NCCs
 * Sends [LEN][TPDU][ISOMSG]
 * (len=2 bytes BCD) 
 * @see ISOMsg
 * @see BaseException
 * @see iConnector
 */
public class NCCConnector extends BaseConnector {
    /**
     * Public constructor 
     */
    boolean tpduSwap = true;
    public NCCConnector () {
        super();
    }
    /**
     * Construct client ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     * @param p     an ISOPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @see iPackager
     */
    public NCCConnector (String host, int port, iPackager p, byte[] TPDU) {
        super(host, port, p);
        this.header = TPDU;
    }
    /**
     * Construct server ISOChannel
     * @param p     an ISOPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @exception IOException
     * @see iPackager
     */
    public NCCConnector (iPackager p, byte[] TPDU) throws IOException {
        super(p);
        this.header = TPDU;
    }
    /**
     * constructs server ISOChannel associated with a Server Socket
     * @param p     an ISOPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see iPackager
     */
    public NCCConnector (iPackager p, byte[] TPDU, ServerSocket serverSocket) 
        throws IOException
    {
        super(p, serverSocket);
        this.header = TPDU;
    }
    protected void sendMessageLength(int len) throws IOException {
        try {
            serverOut.write (
                ISOUtil.str2bcd (
                    ISOUtil.zeropad (Integer.toString (len % 10000), 4), true
                )
            );
        } 
        catch (BaseException e) {
            Logger.log (new LogEvent (this, "send-message-length", e));
        }
    }
    protected int getMessageLength() throws IOException, BaseException {
        byte[] b = new byte[2];
        serverIn.readFully(b,0,2);
        return Integer.parseInt (
            ISOUtil.bcd2str (b, 0, 4, true)
        );
    }
    protected void sendMessageHeader(ISOMsg m, int len) throws IOException { 
        byte[] h = m.getHeader();
        if (h != null) {
            if (tpduSwap && h.length == 5) {
                // swap src/dest address
                byte[] tmp = new byte[2];
                System.arraycopy (h,   1, tmp, 0, 2);
                System.arraycopy (h,   3,   h, 1, 2);
                System.arraycopy (tmp, 0,   h, 3, 2);
            }
        }
        else
            h = header ;
        if (h != null) 
            serverOut.write(h);
    }
    /**
     * New QSP compatible signature (see QSP's ConfigChannel)
     * @param header String as seen by QSP
     */
    public void setHeader (String header) {
        super.setHeader (ISOUtil.str2bcd(header, false));
    }
    public void setConfiguration (Configuration cfg) 
        throws ConfigurationException
    {
        super.setConfiguration (cfg);
        tpduSwap = cfg.getBoolean ("tpdu-swap", true);
    }
}

