
package com.base.iso.connector;

import javax.net.ssl.SSLSocket;

import com.base.iso.core.BaseHeader;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOHeader;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.iClientSocketFactory;
import com.base.iso.interfaces.connector.iBaseChannel;
import com.base.iso.interfaces.connector.iClientConnector;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.interfaces.connector.iFactoryConnector;
import com.base.iso.interfaces.connector.iServerConnector;
import com.base.iso.interfaces.connector.filter.iBaseFilter;
import com.base.iso.interfaces.connector.filter.iFilteredConnector;
import com.base.iso.interfaces.connector.filter.iRawIncomingFilter;
import com.base.iso.interfaces.connector.filter.iBaseFilter.VetoException;
import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Observable;

/**
 * Base connector: transmits and receives ISO 8583 messages over a TCP/IP session
 * (socket lifecycle, framing hooks, filters, timeouts, logging). Subclasses define the
 * length prefix and header handling (NCC, BCD, NAC, HEX).
 * <p>
 * One connector per socket; send and receive are each serialized internally. Callers match
 * responses to requests themselves (F11 / F37).
 * <p>
 * Observable so UI or monitoring code can follow connect / send / receive events.
 *
 * @see ISOMsg
 * @see BaseException
 * @see Logger
 */
@SuppressWarnings("unchecked")
public abstract class BaseConnector extends Observable
        implements iFilteredConnector, Cloneable, iClientConnector, iServerConnector, iFactoryConnector,
        iLogSource, iBaseChannel {
    private Socket socket;
    private String host, localIface;
    private String[] hosts;
    private int[] ports;
    private int port, timeout, connectTimeout, localPort;
    private int maxPacketLength = 100000;
    private boolean keepAlive;
    private boolean expectKeepAlive;
    private boolean soLingerOn = true;
    private int soLingerSeconds = 5;
    protected boolean usable;
    protected boolean overrideHeader;
    private String name;
    // private int serverPort = -1;
    protected DataInputStream serverIn;
    protected DataOutputStream serverOut;
    // The lock objects should be final, and never changed, but due to the clone()
    // method, they must be set there.
    protected Object serverInLock = new Object();
    protected Object serverOutLock = new Object();
    protected iPackager packager;
    protected ServerSocket serverSocket = null;
    protected List<iBaseFilter> incomingFilters, outgoingFilters;
    protected iClientSocketFactory socketFactory = null;

    protected int[] cnt;

    protected Logger logger = null;
    protected String realm = null;
    protected String originalRealm = null;
    protected byte[] header = null;

    /**
     * constructor shared by server and client
     * connectors (which have different signatures)
     */
    public BaseConnector() {
        super();
        cnt = new int[SIZEOF_CNT];
        name = "";
        incomingFilters = new ArrayList();
        outgoingFilters = new ArrayList();
        setHost(null, 0);
    }

    /**
     * constructs a client connector
     * 
     * @param host server TCP Address
     * @param port server port number
     * @param p    an iPackager
     * @see iPackager
     */
    public BaseConnector(String host, int port, iPackager p) {
        this();
        setHost(host, port);
        setPackager(p);
    }

    /**
     * constructs a server connector
     * 
     * @param p an iPackager
     * @exception IOException on error
     * @see iPackager
     */
    public BaseConnector(iPackager p) throws IOException {
        this();
        setPackager(p);
    }

    /**
     * constructs a server connector associated with a Server Socket
     * 
     * @param p            an iPackager
     * @param serverSocket where to accept a connection
     * @exception IOException on error
     * @see iPackager
     */
    public BaseConnector(iPackager p, ServerSocket serverSocket)
            throws IOException {
        this();
        setPackager(p);
        setServerSocket(serverSocket);
    }

    /**
     * initialize an connector
     * 
     * @param host server TCP Address
     * @param port server port number
     */
    public void setHost(String host, int port) {
        this.host = host;
        this.port = port;
        this.hosts = new String[] { host };
        this.ports = new int[] { port };
    }

    /**
     * initialize an connector
     * 
     * @param iface server TCP Address
     * @param port  server port number
     */
    public void setLocalAddress(String iface, int port) {
        this.localIface = iface;
        this.localPort = port;
    }

    /**
     * @param host to connect (client connector)
     */
    public void setHost(String host) {
        this.host = host;
        this.hosts = new String[] { host };
    }

    /**
     * @param port to connect (client connector)
     */
    public void setPort(int port) {
        this.port = port;
        this.ports = new int[] { port };
    }

    /**
     * @return hostname (may be null)
     */
    public String getHost() {
        return host;
    }

    /**
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * set Packager for channel
     * 
     * @param p an iPackager
     * @see iPackager
     */
    public void setPackager(iPackager p) {
        this.packager = p;
    }

    /**
     * @return current packager
     */
    public iPackager getPackager() {
        return packager;
    }

    /**
     * Associates this connector with a server socket
     * 
     * @param sock where to accept a connection
     */
    public void setServerSocket(ServerSocket sock) {
        setHost(null, 0);
        this.serverSocket = sock;
        name = "";
    }

    /**
     * reset stat info
     */
    public void resetCounters() {
        for (int i = 0; i < SIZEOF_CNT; i++)
            cnt[i] = 0;
    }

    /**
     * @return counters
     */
    public int[] getCounters() {
        return cnt;
    }

    /**
     * @return the connection state
     */
    public boolean isConnected() {
        return socket != null && usable;
    }

    /**
     * setup I/O Streams from socket
     * 
     * @param socket a Socket (client or server)
     * @exception IOException on error
     */
    protected void connect(Socket socket)
            throws IOException {
        this.socket = socket;
        applyTimeout();
        setLogger(getLogger(), getOriginalRealm() +
                "/" + socket.getInetAddress().getHostAddress() + ":"
                + socket.getPort());
        synchronized (serverInLock) {
            serverIn = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));
        }
        synchronized (serverOutLock) {
            serverOut = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream(), 2048));
        }
        postConnectHook();
        usable = true;
        cnt[CONNECT]++;
        setChanged();
        notifyObservers();
    }

    protected void postConnectHook() throws IOException {
        // do nothing
    }

    /**
     * factory method for the client socket
     * 
     * @param host remote host
     * @param port remote port
     * @throws IOException on error
     *
     *                     Use Socket factory if exists. If it is missing create a
     *                     normal socket
     * @see iClientSocketFactory
     * @return newly created socket
     */
    protected Socket newSocket(String host, int port) throws IOException {
        try {
            if (socketFactory != null)
                return socketFactory.createSocket(host, port);
            else {
                if (connectTimeout > 0) {
                    Socket s = new Socket();
                    s.connect(
                            new InetSocketAddress(host, port),
                            connectTimeout);
                    return s;
                } else if (localIface == null && localPort == 0) {
                    return new Socket(host, port);
                } else {
                    InetAddress addr = (localIface == null) ? InetAddress.getLocalHost()
                            : InetAddress.getByName(localIface);
                    return new Socket(host, port, addr, localPort);
                }
            }
        } catch (BaseException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected Socket newSocket(String[] hosts, int[] ports, LogEvent evt)
            throws IOException {
        Socket s = null;
        for (int i = 0; i < hosts.length; i++) {
            try {
                evt.addMessage(hosts[i] + ":" + ports[i]);
                s = newSocket(hosts[i], ports[i]);
                break;
            } catch (IOException e) {
                evt.addMessage("  " + e.getMessage());
            }
        }
        if (s == null)
            throw new IOException("Unable to connect");
        return s;
    }

    /**
     * @return current socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @return current serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /**
     * sets socket timeout
     * 
     * @param timeout in milliseconds
     * @throws SocketException on error
     */
    public void setTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
        applyTimeout();
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * sets timeout, and also keep alive
     * 
     * @throws SocketException
     */
    protected void applyTimeout() throws SocketException {
        if (socket != null) {
            socket.setKeepAlive(keepAlive);
            if (timeout >= 0)
                socket.setSoTimeout(timeout);
        }
    }

    /**
     * Socket SO_LINGER option to use when closing the socket.
     * 
     * @see java.net.Socket#setSoLinger(boolean, int)
     */
    public void setSoLinger(boolean on, int linger) {
        this.soLingerOn = on;
        this.soLingerSeconds = linger;
    }

    public boolean isSoLingerOn() {
        return soLingerOn;
    }

    public int getSoLingerSeconds() {
        return soLingerSeconds;
    }

    /**
     * Connects client connector to server
     * 
     * @exception IOException
     */
    public void connect() throws IOException {
        LogEvent evt = new LogEvent(this, "connect");
        try {
            if (serverSocket != null) {
                accept(serverSocket);
                evt.addMessage("local port " + serverSocket.getLocalPort()
                        + " remote host " + socket.getInetAddress());
            } else {
                connect(newSocket(hosts, ports, evt));
            }
            applyTimeout();
            Logger.log(evt);
        } catch (ConnectException e) {
            // T8: log AND rethrow — swallowing it let callers believe they were connected.
            Logger.log(new LogEvent(this, "connection-refused",
                    getHost() + ":" + getPort()));
            throw e;
        } catch (IOException e) {
            evt.addMessage(e.getMessage());
            Logger.log(evt);
            throw e;
        }
    }

    /**
     * Accepts connection
     * 
     * @exception IOException
     */
    public void accept(ServerSocket s) throws IOException {
        // if (serverPort > 0)
        // s = new ServerSocket (serverPort);
        // else
        // serverPort = s.getLocalPort();

        Socket ss = s.accept();
        this.name = ss.getInetAddress().getHostAddress() + ":" + ss.getPort();
        connect(ss);

        // Warning - do not close the ServerSocket here: a server keeps accepting
        // further connections on it (one cloned connector per connection).
        // s.close();
    }

    /**
     * @param b - new usable state (set false to force a reconnection)
     */
    public void setUsable(boolean b) {
        Logger.log(new LogEvent(this, "usable", b));
        usable = b;
    }

    /**
     * allow subclasses to override default packager
     * on outgoing messages
     * 
     * @param m outgoing ISOMsg
     * @return iPackager
     */
    protected iPackager getDynamicPackager(ISOMsg m) {
        return packager;
    }

    /**
     * allow subclasses to override default packager
     * on outgoing messages
     * 
     * @param image incoming message image
     * @return iPackager
     */
    protected iPackager getDynamicPackager(byte[] image) {
        return packager;
    }

    /**
     * allow subclasses to override default packager
     * on outgoing messages
     * 
     * @param header message header
     * @param image  incoming message image
     * @return iPackager
     */
    protected iPackager getDynamicPackager(byte[] header, byte[] image) {
        return getDynamicPackager(image);
    }

    /**
     * Allow subclasses to override the Default header on
     * incoming messages.
     * 
     * @param image message image
     * @return ISOHeader instance
     */
    protected ISOHeader getDynamicHeader(byte[] image) {
        return image != null ? new BaseHeader(image) : null;
    }

    protected void sendMessageLength(int len) throws IOException {
    }

    protected void sendMessageHeader(ISOMsg m, int len) throws IOException {
        if (!isOverrideHeader() && m.getHeader() != null)
            serverOut.write(m.getHeader());
        else if (header != null)
            serverOut.write(header);
    }

    /**
     * @deprecated use sendMessageTrailer(ISOMsg m, byte[] b) instead.
     * @param m   a reference to the ISOMsg
     * @param len the packed image length
     * @throws IOException on error
     */
    protected void sendMessageTrailler(ISOMsg m, int len) throws IOException {
    }

    /**
     * @deprecated use sendMessageTrailer(ISOMsg m, byte[] b instead.
     */
    @SuppressWarnings("deprecation")
    protected void sendMessageTrailler(ISOMsg m, byte[] b) throws IOException {
        sendMessageTrailler(m, b.length);
    }

    /**
     * Send a Message trailer.
     *
     * @param m The unpacked ISOMsg.
     * @param b The packed ISOMsg image.
     */
    @SuppressWarnings("deprecation")
    protected void sendMessageTrailer(ISOMsg m, byte[] b) throws IOException {
        sendMessageTrailler(m, b);
    }

    /**
     * @deprecated use getMessageTrailer(ISOMsg m) instead.
     */
    protected void getMessageTrailler() throws IOException {
    }

    /**
     * Read some trailer data from this channel and optionally store it in the
     * incoming ISOMsg.
     *
     * @param m The ISOMessage to store the trailer data.
     * @see ISOMsg#setTrailer(byte[]).
     */
    @SuppressWarnings("deprecation")
    protected void getMessageTrailer(ISOMsg m) throws IOException {
        getMessageTrailler();
    }

    protected void getMessage(byte[] b, int offset, int len) throws IOException, BaseException {
        serverIn.readFully(b, offset, len);
    }

    protected int getMessageLength() throws IOException, BaseException {
        return -1;
    }

    protected int getHeaderLength() {
        return header != null ? header.length : 0;
    }

    protected int getHeaderLength(byte[] b) {
        return 0;
    }

    protected int getHeaderLength(ISOMsg m) {
        return (!overrideHeader && m.getHeader() != null) ? m.getHeader().length : getHeaderLength();
    }

    protected byte[] streamReceive() throws IOException {
        return new byte[0];
    }

    protected void sendMessage(byte[] b, int offset, int len)
            throws IOException {
        serverOut.write(b, offset, len);
    }

    /**
     * sends an ISOMsg over the TCP/IP session
     * 
     * @param m the Message to be sent
     * @exception IOException
     * @exception BaseException
     * @exception iBaseFilter.VetoException;
     */
    public void send(ISOMsg m)
            throws IOException, BaseException {
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected())
                throw new BaseException("unconnected connector");
            m.setDirection(ISOMsg.OUTGOING);
            iPackager p = getDynamicPackager(m);
            m.setPackager(p);
            m = applyOutgoingFilters(m, evt);
            evt.addMessage(m);
            m.setDirection(ISOMsg.OUTGOING); // filter may have dropped this info
            m.setPackager(p); // and could have dropped packager as well
            byte[] b = m.pack();
        
            synchronized (serverOutLock) {
                sendMessageLength(b.length + getHeaderLength(m));
                sendMessageHeader(m, b.length);
                sendMessage(b, 0, b.length);
                sendMessageTrailer(m, b);
                serverOut.flush();
            }
            cnt[TX]++;
            setChanged();
            notifyObservers(m);
        } catch (VetoException e) {
            // if a filter vets the message it was not added to the event
            evt.addMessage(m);
            evt.addMessage(e);
            throw e;
        } catch (BaseException e) {
            evt.addMessage(e);
            throw e;
        } catch (IOException e) {
            evt.addMessage(e);
            throw e;
        } catch (Exception e) {
            evt.addMessage(e);
            throw new BaseException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
    }

    /**
     * sends a byte[] over the TCP/IP session
     * 
     * @param b the byte array to be sent
     * @exception IOException
     * @exception BaseException
     * @exception iBaseFilter.VetoException;
     */
    public void send(byte[] b)
            throws IOException, BaseException {
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected())
                throw new BaseException("unconnected connector");
            synchronized (serverOutLock) {
                serverOut.write(b);
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

    /**
     * Sends a high-level keep-alive message (zero length)
     * 
     * @throws IOException on exception
     */
    public void sendKeepAlive() throws IOException {
        synchronized (serverOutLock) {
            sendMessageLength(0);
            serverOut.flush();
        }

    }

    protected boolean isRejected(byte[] b) {
        // hook for header-based rejection/ignoring (override in a subclass)
        return false;
    }

    protected boolean shouldIgnore(byte[] b) {
        // hook for header-based rejection/ignoring (override in a subclass)
        return false;
    }

    /**
     * support old factory method name for backward compatibility
     * 
     * @return newly created ISOMsg
     */
    protected ISOMsg createMsg() {
        return createISOMsg();
    }

    protected ISOMsg createISOMsg() {
        return packager.createISOMsg();
    }

    /**
     * Reads in a message header.
     *
     * @param hLen The Length og the reader to read
     * @return The header bytes that were read in
     * @throws IOException on error
     */
    protected byte[] readHeader(int hLen) throws IOException {
        byte[] header = new byte[hLen];
        serverIn.readFully(header, 0, hLen);
        return header;
    }

    /**
     * Waits and receive an ISOMsg over the TCP/IP session
     * 
     * @return the Message received
     * @throws IOException
     * @throws BaseException
     */
    public ISOMsg receive() throws IOException, BaseException {
        byte[] b = null;
        byte[] header = null;
        LogEvent evt = new LogEvent(this, "receive");
        ISOMsg m = createMsg(); // call createMsg instead of createISOMsg for
                                // backward compatibility
        m.setSource(this);
        try {
            if (!isConnected())
                throw new BaseException("unconnected connector");

            synchronized (serverInLock) {
                int len = getMessageLength();
                if (expectKeepAlive) {
                    while (len == 0) {
                        // If zero length, this is a keep alive msg
                        Logger.log(new LogEvent(this, "receive", "Zero length keep alive message received"));
                        len = getMessageLength();
                    }
                }
                int hLen = getHeaderLength();

                if (len == -1) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                    }
                    b = streamReceive();
                } else if (len > 0 && len <= getMaxPacketLength()) {
                    if (hLen > 0) {
                        // ignore message header (TPDU)
                        // Note header length is not necessarily equal to hLen (a subclass may use a variable header)
                        header = readHeader(hLen);
                        len -= header.length;
                    }
                    b = new byte[len];
                    getMessage(b, 0, len);
                    getMessageTrailer(m);
                } else
                    throw new BaseException(
                            "receive length " + len + " seems strange - maxPacketLength = " + getMaxPacketLength());
            }
            m.setPackager(getDynamicPackager(header, b));
            m.setHeader(getDynamicHeader(header));
            if (b.length > 0 && !shouldIgnore(header)) // Ignore NULL messages
                unpack(m, b);
            m.setDirection(ISOMsg.INCOMING);
            evt.addMessage(m);
            m = applyIncomingFilters(m, header, b, evt);
            m.setDirection(ISOMsg.INCOMING);
            cnt[RX]++;
            setChanged();
            notifyObservers(m);
        } catch (BaseException e) {
            evt.addMessage(e);
            if (header != null) {
                evt.addMessage("header", ISOUtil.hexString(header));
            }
            if (b != null) {
                // T12: the raw frame holds PAN / track 2 / PIN block — log its size only.
                evt.addMessage("data", "[" + b.length + " bytes]");
            }
            throw e;
        } catch (EOFException e) {
            closeSocket();
            evt.addMessage("peer-disconnect", "EOF");
            throw e;
        } catch (SocketException e) {
            closeSocket();
            if (usable)
                evt.addMessage("peer-disconnect", e.getMessage());
            throw e;
        } catch (InterruptedIOException e) {
            closeSocket();
            evt.addMessage("io-timeout", e.getMessage());
            throw e;
        } catch (IOException e) {
            closeSocket();
            if (usable)
                evt.addMessage(e);
            throw e;
        } catch (Exception e) {
            evt.addMessage(m);
            evt.addMessage(e);
            throw new BaseException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
        return m;
    }

    /**
     * Low level receive
     * 
     * @param b byte array
     * @throws IOException on error
     * @return the total number of bytes read into the buffer,
     *         or -1 if there is no more data because the end of the stream has been
     *         reached.
     */
    public int getBytes(byte[] b) throws IOException {
        return serverIn.read(b);
    }

    /**
     * disconnects the TCP/IP session. The instance is ready for
     * a reconnection. There is no need to create a new connector<br>
     * 
     * @exception IOException
     */
    public void disconnect() throws IOException {
        LogEvent evt = new LogEvent(this, "disconnect");
        if (serverSocket != null)
            evt.addMessage("local port " + serverSocket.getLocalPort()
                    + " remote host " + serverSocket.getInetAddress());
        else
            evt.addMessage(host + ":" + port);
        try {
            usable = false;
            setChanged();
            notifyObservers();
            closeSocket();
            if (serverIn != null) {
                try {
                    serverIn.close();
                } catch (IOException ex) {
                    evt.addMessage(ex);
                }
                serverIn = null;
            }
            if (serverOut != null) {
                try {
                    serverOut.close();
                } catch (IOException ex) {
                    evt.addMessage(ex);
                }
                serverOut = null;
            }
        } catch (IOException e) {
            evt.addMessage(e);
            Logger.log(evt);
            throw e;
        }
        socket = null;
    }

    /**
     * Issues a disconnect followed by a connect
     * 
     * @exception IOException
     */
    public void reconnect() throws IOException {
        disconnect();
        connect();
    }

    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm = realm;
        if (originalRealm == null)
            originalRealm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getOriginalRealm() {
        return originalRealm == null ? this.getClass().getName() : originalRealm;
    }

    /**
     * @return peer address ("ip:port") of the last accepted connection; "" for client connectors
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param filter    filter to add
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    @SuppressWarnings("unchecked")
    public void addFilter(iBaseFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING:
                incomingFilters.add(filter);
                break;
            case ISOMsg.OUTGOING:
                outgoingFilters.add(filter);
                break;
            case 0:
                incomingFilters.add(filter);
                outgoingFilters.add(filter);
                break;
        }
    }

    /**
     * @param filter incoming filter to add
     */
    public void addIncomingFilter(iBaseFilter filter) {
        addFilter(filter, ISOMsg.INCOMING);
    }

    /**
     * @param filter outgoing filter to add
     */
    public void addOutgoingFilter(iBaseFilter filter) {
        addFilter(filter, ISOMsg.OUTGOING);
    }

    /**
     * @param filter filter to add (both directions, incoming/outgoing)
     */
    public void addFilter(iBaseFilter filter) {
        addFilter(filter, 0);
    }

    /**
     * @param filter    filter to remove
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    public void removeFilter(iBaseFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING:
                incomingFilters.remove(filter);
                break;
            case ISOMsg.OUTGOING:
                outgoingFilters.remove(filter);
                break;
            case 0:
                incomingFilters.remove(filter);
                outgoingFilters.remove(filter);
                break;
        }
    }

    /**
     * @param filter filter to remove (both directions)
     */
    public void removeFilter(iBaseFilter filter) {
        removeFilter(filter, 0);
    }

    /**
     * @param filter incoming filter to remove
     */
    public void removeIncomingFilter(iBaseFilter filter) {
        removeFilter(filter, ISOMsg.INCOMING);
    }

    /**
     * @param filter outgoing filter to remove
     */
    public void removeOutgoingFilter(iBaseFilter filter) {
        removeFilter(filter, ISOMsg.OUTGOING);
    }

    protected ISOMsg applyOutgoingFilters(ISOMsg m, LogEvent evt)
            throws VetoException {
        for (iBaseFilter f : outgoingFilters)
            m = f.filter(this, m, evt);
        return m;
    }

    protected ISOMsg applyIncomingFilters(ISOMsg m, LogEvent evt)
            throws VetoException {
        return applyIncomingFilters(m, null, null, evt);
    }

    protected ISOMsg applyIncomingFilters(ISOMsg m, byte[] header, byte[] image, LogEvent evt)
            throws VetoException {
        for (iBaseFilter f : incomingFilters) {
            if (image != null && (f instanceof iRawIncomingFilter))
                m = ((iRawIncomingFilter) f).filter(this, m, header, image, evt);
            else
                m = f.filter(this, m, evt);
        }
        return m;
    }

    protected void unpack(ISOMsg m, byte[] b) throws BaseException {
        m.unpack(b);
    }

    /**
     * Sets the primary host plus fallback hosts, tried in order by {@link #connect()}
     * until one accepts. {@code hosts[0]} / {@code ports[0]} become {@link #getHost()} / {@link #getPort()}.
     *
     * @param hosts host names or IPs (at least one)
     * @param ports port per host, same length as {@code hosts}
     * @throws IllegalArgumentException if the arrays are empty or their lengths differ
     */
    public void setHosts(String[] hosts, int[] ports) {
        if (hosts == null || ports == null || hosts.length == 0 || hosts.length != ports.length)
            throw new IllegalArgumentException("hosts and ports must be non-empty and the same length");
        this.host = hosts[0];
        this.port = ports[0];
        this.hosts = hosts.clone();
        this.ports = ports.clone();
    }

    /**
     * @param keepAlive enable TCP SO_KEEPALIVE (default false); applied now if connected,
     *                  otherwise on the next connect
     * @throws SocketException if the open socket rejects the option
     */
    public void setKeepAlive(boolean keepAlive) throws SocketException {
        this.keepAlive = keepAlive;
        applyTimeout();
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * @param expectKeepAlive when true, {@link #receive()} skips zero-length frames from the
     *                        host as keep-alives (default false)
     */
    public void setExpectKeepAlive(boolean expectKeepAlive) {
        this.expectKeepAlive = expectKeepAlive;
    }

    public boolean isExpectKeepAlive() {
        return expectKeepAlive;
    }

    /**
     * @param connectTimeout TCP connect timeout in milliseconds; 0 = OS default. Not used when a
     *                       socket factory is set (the factory opens the socket).
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public Collection<iBaseFilter> getIncomingFilters() {
        return incomingFilters;
    }

    public Collection<iBaseFilter> getOutgoingFilters() {
        return outgoingFilters;
    }

    public void setIncomingFilters(Collection filters) {
        incomingFilters = new ArrayList(filters);
    }

    public void setOutgoingFilters(Collection filters) {
        outgoingFilters = new ArrayList(filters);
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    public void setHeader(String header) {
        setHeader(header.getBytes());
    }

    public byte[] getHeader() {
        return header;
    }

    public void setOverrideHeader(boolean overrideHeader) {
        this.overrideHeader = overrideHeader;
    }

    public boolean isOverrideHeader() {
        return overrideHeader;
    }

    /**
     * Gets the iClientSocketFactory (may be null)
     * 
     * @see iClientSocketFactory
     * @since 1.3.3 \
     * @return iClientSocketFactory
     */
    public iClientSocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
     * Sets the specified Socket Factory to create sockets
     * 
     * @param socketFactory the iClientSocketFactory
     * @see iClientSocketFactory
     * @since 1.3.3
     */
    public void setSocketFactory(iClientSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public int getMaxPacketLength() {
        return maxPacketLength;
    }

    public void setMaxPacketLength(int maxPacketLength) {
        this.maxPacketLength = maxPacketLength;
    }

    protected void closeSocket() throws IOException {
        Socket s = null;
        synchronized (this) {
            if (socket != null) {
                s = socket; // we don't want more than one thread
                socket = null; // attempting to close the socket
            }
        }
        if (s != null) {
            try {
                s.setSoLinger(soLingerOn, soLingerSeconds);
                if (shutdownSupportedBySocket(s) && !isSoLingerForcingImmediateTcpReset())
                    s.shutdownOutput(); // This will force a TCP FIN to be sent on regular sockets,
            } catch (SocketException e) {
                // safe to ignore - can be closed already
                // e.printStackTrace();
            }
            s.close();
        }
    }

    private boolean shutdownSupportedBySocket(Socket s) {
        return !(s instanceof SSLSocket); // we can't close output on SSL connections,
    }

    private boolean isSoLingerForcingImmediateTcpReset() {
        return soLingerOn && soLingerSeconds == 0;
    }

    public Object clone() {
        try {
            BaseConnector channel = (BaseConnector) super.clone();
            channel.cnt = cnt.clone();
            // The lock objects must also be cloned, and the DataStreams nullified, as it
            // makes no sense
            // to use the new lock objects to protect the old DataStreams.
            // This is safe because a server-side caller clones the connector and immediately
            // calls accept(ServerSocket), which does a connect() that sets the stream objects.
            channel.serverInLock = new Object();
            channel.serverOutLock = new Object();
            channel.serverIn = null;
            channel.serverOut = null;
            channel.usable = false;
            channel.socket = null;
            return channel;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
