package com.base.iso.connector;


import java.io.IOException;
import java.net.ServerSocket;

import com.base.iso.config.Configuration;
import com.base.iso.config.ConfigurationException;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.interfaces.connector.filter.iBaseFilter;
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

        /**
     * sends an byte[] over the TCP/IP session with TPDU, length and raw byte messsage
     * 
     * @param m the Message to be sent
     * @exception IOException
     * @exception BaseException
     * @exception iBaseFilter.VetoException;
     */
    public void sendraw(byte[] b, byte[] TPDU) throws IOException, BaseException {
    LogEvent evt = new LogEvent(this, "send");
    try {
        if (!isConnected())
            throw new BaseException("unconnected ISOChannel");

        // 1) Build payload = TPDU + b (TPDU optional)
        final byte[] tpdu = (TPDU == null) ? new byte[0] : TPDU;
        final byte[] body  = (b == null) ? new byte[0] : b;

        final int payloadLen = tpdu.length + body.length; // 2) Count length of (TPDU + b)
        if (payloadLen > 0xFFFF) {
            throw new BaseException("message too long: " + payloadLen + " (> 65535)");
        }

        // 3) Convert length to 2-byte big-endian hex (just two bytes)
        final byte[] len2 = new byte[2];
        len2[0] = (byte) ((payloadLen >>> 8) & 0xFF);
        len2[1] = (byte) (payloadLen & 0xFF);

        // 4) Build final message = [2-byte length] + TPDU + b
        final byte[] out = new byte[2 + payloadLen];
        System.arraycopy(len2, 0, out, 0, 2);
        if (tpdu.length > 0) System.arraycopy(tpdu, 0, out, 2, tpdu.length);
        if (body.length > 0) System.arraycopy(body, 0, out, 2 + tpdu.length, body.length);

        // 5) Send modified message
        synchronized (serverOutLock) {
            serverOut.write(out);
            serverOut.flush();
        }

        cnt[TX]++;
        setChanged();
    } catch (Exception e) {
        evt.addMessage(e);
        throw new BaseException("unexpected exception", e);
    } finally {
        Logger.log(evt);
    }
}

}

