
package com.base.iso.connector;
import java.io.IOException;
import java.net.ServerSocket;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/**
 * Talks with TCP based NCCs
 * Sends [LEN][TPDU][ISOMSG]
 * (len=2 bytes HEX)
 * @see ISOMsg
 * @see BaseException
 * @see iConnector
 */
public class BCDConnector extends BaseConnector {
    /**
     * Public constructor 
     */
    public BCDConnector () {
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
    public BCDConnector (String host, int port, iPackager p, byte[] TPDU) {
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
    public BCDConnector (iPackager p, byte[] TPDU) throws IOException {
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
    public BCDConnector (iPackager p, byte[] TPDU, ServerSocket serverSocket) 
        throws IOException
    {
        super(p, serverSocket);
        this.header = TPDU;
    }
    protected void sendMessageLength(int len) throws IOException {
        try {
            serverOut.write (                                                                                                         
                ISOUtil.str2bcd (                                                                                                     
                    ISOUtil.zeropad (Integer.toString (len), 4), true
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
        try {
            return Integer.parseInt (
                ISOUtil.bcd2str (b, 0, 4, true)
            );
        } catch (NumberFormatException e) {
            // T9: stream position is now unknown → drop the connection instead of desyncing
            closeSocket();
            throw new BaseException ("invalid length prefix: " + ISOUtil.hexString (b), e);
        }
    }
    protected void sendMessageHeader(ISOMsg m, int len) throws IOException { 
        byte[] h = m.getHeader();
        if (h != null) {
            if (h.length == 5) {
                // swap src/dest address on a COPY (T7): swapping m's header in place made a
                // re-send of the same ISOMsg (retry, reversal) go out with the unswapped TPDU.
                h = h.clone();
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
     * Sets the header (TPDU) from its hex text, e.g. "6000010000".
     * @param header header as a hex string
     */
    public void setHeader (String header) {
        super.setHeader (ISOUtil.str2bcd(header, false));
    }

    public void sendBcd(byte[] b, byte[] TPDU) throws IOException, BaseException {
        sendBcd(b, TPDU, false);
    }

    /**
     * Same as {@link #sendBcd(byte[], byte[])}; when {@code hexdump} is true the full frame
     * ([length][TPDU][b]) is added to the "send" log event as a hexdump.
     * <b>Debug only:</b> the frame is NOT masked.
     */
    public void sendBcd(byte[] b, byte[] TPDU, boolean hexdump)
        throws IOException, BaseException {
    LogEvent evt = new LogEvent(this, "send");
    try {
        if (!isConnected())
            throw new BaseException("unconnected connector");

        // 1) Build payload = TPDU + b
        final byte[] tpdu = (TPDU == null) ? new byte[0] : TPDU;
        final byte[] body = (b == null) ? new byte[0] : b;

        final int payloadLen = tpdu.length + body.length;
        if (payloadLen > 9999) { // BCD supports max 4 digits (0000-9999)
            throw new BaseException("message too long: " + payloadLen + " (>9999)");
        }

        // 2) Convert length to 2-byte BCD (big-endian)
        // Example: 1234 → 0x12 0x34
        final byte[] lenBcd = new byte[2];
        String lenStr = String.format("%04d", payloadLen);
        lenBcd[0] = (byte) (((lenStr.charAt(0) - '0') << 4) | (lenStr.charAt(1) - '0'));
        lenBcd[1] = (byte) (((lenStr.charAt(2) - '0') << 4) | (lenStr.charAt(3) - '0'));

        // 3) Build final message = [2-byte BCD length] + TPDU + b
        final byte[] out = new byte[2 + payloadLen];
        System.arraycopy(lenBcd, 0, out, 0, 2);
        if (tpdu.length > 0) System.arraycopy(tpdu, 0, out, 2, tpdu.length);
        if (body.length > 0) System.arraycopy(body, 0, out, 2 + tpdu.length, body.length);

        // 4) Send the message
        if (hexdump)
            addHexdump(evt, out);
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

