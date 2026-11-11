package com.base.iso.connector;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/**
 * Talks with TCP based NACs
 * Sends [LEN][TPDU][ISOMSG]
 * (len=2 bytes network byte order) 
 * @see ISOMsg
 * @see BaseException
 * @see iConnector
 */
public class NACConnector extends BaseConnector {
    /**
     * Public constructor 
     */
    boolean tpduSwap = true;
    public NACConnector () {
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
    public NACConnector (String host, int port, iPackager p, byte[] TPDU) {
        super(host, port, p);
        this.header = TPDU;
    }
    /**
     * Construct server connector
     * @param p     an iPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @exception IOException on error
     * @see iPackager
     */
    public NACConnector (iPackager p, byte[] TPDU) throws IOException {
        super(p);
        this.header = TPDU;
    }
    /**
     * constructs server connector associated with a Server Socket
     * @param p     an iPackager
     * @param TPDU  an optional raw header (i.e. TPDU)
     * @param serverSocket where to accept a connection
     * @exception IOException on error
     * @see iPackager
     */
    public NACConnector (iPackager p, byte[] TPDU, ServerSocket serverSocket) 
        throws IOException
    {
        super(p, serverSocket);
        this.header = TPDU;
    }
    protected void sendMessageLength(int len) throws IOException {
        serverOut.write (len >> 8);
        serverOut.write (len);
    }
    protected int getMessageLength() throws IOException, BaseException {
        byte[] b = new byte[2];
        serverIn.readFully(b,0,2);
        return ((((int)b[0])&0xFF) << 8) | (((int)b[1])&0xFF);
    }
    protected void sendMessageHeader(ISOMsg m, int len) throws IOException { 
        byte[] h = m.getHeader();
        if (h != null) {
            if (tpduSwap && h.length == 5) {
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
            h = header;
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
    /**
     * @param tpduSwap swap TPDU source/destination addresses when sending a message whose own
     *                 header is a 5-byte TPDU (default true)
     */
    public void setTpduSwap (boolean tpduSwap) {
        this.tpduSwap = tpduSwap;
    }
    public boolean isTpduSwap () {
        return tpduSwap;
    }
    
    public void sendAscii(byte[] b, byte[] TPDU) throws IOException, BaseException {
        sendAscii(b, TPDU, false);
    }

    /**
     * Same as {@link #sendAscii(byte[], byte[])}; when {@code hexdump} is true the full frame
     * ([length][TPDU][b]) is added to the "send" log event as a hexdump.
     * <b>Debug only:</b> the frame is NOT masked.
     */
    public void sendAscii(byte[] b, byte[] TPDU, boolean hexdump) throws IOException, BaseException {
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected())
                throw new BaseException("unconnected connector");

            // 1) Build payload = TPDU + b (null-safe)
            final byte[] tpdu = (TPDU == null) ? new byte[0] : TPDU;
            final byte[] body = (b == null) ? new byte[0] : b;

            final int payloadLen = tpdu.length + body.length;  // length of TPDU+b

            // 2) Use 4 ASCII digits (0000..9999) for length
            if (payloadLen > 9999) {
                throw new BaseException("message too long for 4-digit ASCII length: " + payloadLen);
            }

            // 3) Convert length to 4 ASCII digits
            final byte[] lenAscii = String.format("%04d", payloadLen)
                    .getBytes(StandardCharsets.US_ASCII);

            // 4) Final message = [4-byte ASCII length] + TPDU + b
            final byte[] out = new byte[lenAscii.length + payloadLen];
            System.arraycopy(lenAscii, 0, out, 0, lenAscii.length);
            if (tpdu.length > 0) System.arraycopy(tpdu, 0, out, lenAscii.length, tpdu.length);
            if (body.length > 0) System.arraycopy(body, 0, out, lenAscii.length + tpdu.length, body.length);

            // 5) Send
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

