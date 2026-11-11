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
 * Talks with TCP based hosts using a binary length prefix.
 * Sends [LEN][TPDU][ISOMSG]
 * (len=2 bytes binary, network byte order / big-endian, max 65535 — e.g. 300 bytes → 01 2C)
 * <p>
 * Same as {@link BCDConnector} (TPDU handling, 5-byte header always swapped) except for the
 * length prefix, which is framed like {@link NACConnector}.
 * Not the old HEXConnector (4 ASCII hex characters) — that one is now {@link ASCIIConnector}.
 * @see ISOMsg
 * @see BaseException
 * @see iConnector
 */
public class HEXConnector extends BaseConnector {
    /**
     * Public constructor
     */
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
    /** Writes the length (TPDU + message) as 2 bytes, high byte first — same as NACConnector. */
    protected void sendMessageLength(int len) throws IOException {
        serverOut.write (len >> 8);
        serverOut.write (len);
    }
    /** Reads the 2-byte big-endian length. Every value 0..65535 is valid, so no corrupt-prefix check. */
    protected int getMessageLength() throws IOException, BaseException {
        byte[] b = new byte[2];
        serverIn.readFully(b,0,2);
        return ((((int)b[0])&0xFF) << 8) | (((int)b[1])&0xFF);
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

    public void sendHex(byte[] b, byte[] TPDU) throws IOException, BaseException {
        sendHex(b, TPDU, false);
    }

    /**
     * Sends a pre-packed message framed as [2-byte binary length][TPDU][b], bypassing the
     * packager and filters. When {@code hexdump} is true the full frame is added to the
     * "send" log event as a hexdump.
     * <b>Debug only:</b> the frame is NOT masked.
     */
    public void sendHex(byte[] b, byte[] TPDU, boolean hexdump)
        throws IOException, BaseException {
    LogEvent evt = new LogEvent(this, "send");
    try {
        if (!isConnected())
            throw new BaseException("unconnected connector");

        // 1) Build payload = TPDU + b
        final byte[] tpdu = (TPDU == null) ? new byte[0] : TPDU;
        final byte[] body = (b == null) ? new byte[0] : b;

        final int payloadLen = tpdu.length + body.length;
        if (payloadLen > 0xFFFF) { // 2 binary bytes hold at most 65535
            throw new BaseException("message too long: " + payloadLen + " (> 65535)");
        }

        // 2) Convert length to 2-byte binary (big-endian)
        // Example: 300 → 0x01 0x2C
        final byte[] len2 = new byte[2];
        len2[0] = (byte) ((payloadLen >>> 8) & 0xFF);
        len2[1] = (byte) (payloadLen & 0xFF);

        // 3) Build final message = [2-byte binary length] + TPDU + b
        final byte[] out = new byte[2 + payloadLen];
        System.arraycopy(len2, 0, out, 0, 2);
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
