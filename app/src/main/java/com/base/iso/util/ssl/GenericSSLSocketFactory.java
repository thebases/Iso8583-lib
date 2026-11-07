package com.base.iso.util.ssl;

import javax.net.ssl.*;

import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iClientSocketFactory;
import com.base.iso.util.log.SimpleLogSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * TLS socket factory for connectors ({@code BaseConnector.setSocketFactory}) and servers.
 *
 * Secure by default (plan T11):
 * <ul>
 *   <li>Server certificates are validated against the injected trust store, or the platform
 *       trust store when none is set (Android system CAs / JVM cacerts).</li>
 *   <li>Host name is verified with {@code SSLParameters.setEndpointIdentificationAlgorithm("HTTPS")}
 *       (SAN-aware; Android API 24+, JDK 7+). {@link #setServerName(String)} overrides the name
 *       checked, e.g. when connecting by IP to a host whose certificate names a DNS host.</li>
 *   <li>Context is {@code TLS} with the default {@link SecureRandom}.</li>
 *   <li>Trust-all exists only as an explicit opt-in ({@link #setInsecureTrustAll(boolean)})
 *       and logs a warning — test labs only.</li>
 * </ul>
 * Key material is injected, never read from a path: the consumer loads its {@link KeyStore}
 * (Android KeyStore / BKS / PKCS12 asset, service secret store) and calls
 * {@link #setKeyStore(KeyStore, char[])} / {@link #setTrustStore(KeyStore)}, or supplies a
 * complete {@link SSLContext} via {@link #setSSLContext(SSLContext)}.
 *
 * Set up with setters only: {@link #setServerName}, {@link #setClientAuthNeeded},
 * {@link #setEnabledCipherSuites}, {@link #setInsecureTrustAll}.
 */
public class GenericSSLSocketFactory
        extends SimpleLogSource
        implements ISOServerSocketFactory, iClientSocketFactory
{
    private SSLContext sslc = null;
    private SSLServerSocketFactory serverFactory = null;
    private SSLSocketFactory socketFactory = null;

    private KeyStore keyStore = null;
    private char[] keyPassword = null;
    private KeyStore trustStore = null;
    private String serverName;
    private boolean clientAuthNeeded = false;
    private boolean insecureTrustAll = false;
    private String[] enabledCipherSuites;

    /**
     * Client/server key material (needed for mutual TLS or a TLS server).
     * @param ks          a loaded key store
     * @param keyPassword password of the private key entry
     */
    public synchronized void setKeyStore(KeyStore ks, char[] keyPassword) {
        this.keyStore = ks;
        this.keyPassword = keyPassword == null ? null : keyPassword.clone();
        resetContext();
    }

    /** Trust anchors used to validate the peer; {@code null} = platform default trust store. */
    public synchronized void setTrustStore(KeyStore ts) {
        this.trustStore = ts;
        resetContext();
    }

    /** Uses a fully configured context as-is (key store / trust store settings are then ignored). */
    public synchronized void setSSLContext(SSLContext context) {
        resetContext();
        this.sslc = context;
    }

    /** Host name the server certificate must match; defaults to the host passed to createSocket. */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setClientAuthNeeded(boolean clientAuthNeeded) {
        this.clientAuthNeeded = clientAuthNeeded;
    }

    /**
     * DANGEROUS: accept any server certificate and skip host name verification.
     * Allows a man-in-the-middle to read card data. Never enable in production.
     */
    public synchronized void setInsecureTrustAll(boolean insecureTrustAll) {
        this.insecureTrustAll = insecureTrustAll;
        resetContext();
    }

    public boolean isInsecureTrustAll() {
        return insecureTrustAll;
    }

    /**
     * @deprecated server authentication is always on since 2.0.0; use
     * {@link #setInsecureTrustAll(boolean)} to turn it off (test only). This setter is ignored.
     */
    @Deprecated
    public void setServerAuthNeeded(boolean serverAuthNeeded) {
        // intentionally ignored — validation can only be disabled via setInsecureTrustAll(true)
    }

    /** @deprecated see {@link #setServerAuthNeeded(boolean)}; returns {@code !isInsecureTrustAll()}. */
    @Deprecated
    public boolean getServerAuthNeeded() {
        return !insecureTrustAll;
    }

    private void resetContext() {
        sslc = null;
        serverFactory = null;
        socketFactory = null;
    }

    private TrustManager[] getTrustManagers() throws GeneralSecurityException {
        if (insecureTrustAll) {
            warning("insecure-trust-all is enabled: server certificates are NOT validated");
            return new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore); // null → platform default trust store
        return tmf.getTrustManagers();
    }

    /** Builds the TLS context from the injected stores (or returns the injected context). */
    private synchronized SSLContext getSSLContext() throws BaseException {
        if (sslc != null)
            return sslc;
        try {
            KeyManager[] kma = null;
            if (keyStore != null) {
                KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                km.init(keyStore, keyPassword);
                kma = km.getKeyManagers();
            }
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kma, getTrustManagers(), new SecureRandom());
            sslc = ctx;
            return ctx;
        } catch (GeneralSecurityException e) {
            throw new BaseException("Failed to initialize SSLContext", e);
        }
    }

    /**
     * Create a socket factory
     * @return the socket factory
     * @exception BaseException if the TLS context cannot be created
     */
    protected SSLServerSocketFactory createServerSocketFactory()
        throws BaseException
    {
        return getSSLContext().getServerSocketFactory();
    }

    /**
     * Create a socket factory
     * @return the socket factory
     * @exception BaseException if the TLS context cannot be created
     */
    protected SSLSocketFactory createSocketFactory()
        throws BaseException
    {
        return getSSLContext().getSocketFactory();
    }

    /**
     * Create a server socket on the specified port (port 0 indicates
     * an anonymous port).
     * @param  port the port number
     * @return the server socket on the specified port
     * @exception IOException should an I/O error occurs during
     * @exception BaseException should an error occurs during
     * creation
     */
    public ServerSocket createServerSocket(int port)
        throws IOException, BaseException
    {
        if (serverFactory == null) serverFactory = createServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverFactory.createServerSocket(port);
        serverSocket.setNeedClientAuth(clientAuthNeeded);
        if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
            serverSocket.setEnabledCipherSuites(enabledCipherSuites);
        }
        return serverSocket;
    }

    /**
     * Create a client socket connected to the specified host and port, and complete the TLS
     * handshake (certificate + host name checks fail here, not on the first send).
     * @param  host   the host name
     * @param  port   the port number
     * @return a socket connected to the specified host and port.
     * @exception IOException if an I/O or TLS verification error occurs
     * @exception BaseException should any other error occurs
     */
    public Socket createSocket(String host, int port)
        throws IOException, BaseException
    {
        if (socketFactory == null) socketFactory = createSocketFactory();
        String peerName = serverName != null && serverName.length() > 0 ? serverName : host;

        // Connect TCP first, then layer TLS with peerName: that name is used for SNI and for
        // endpoint identification, so a host reached by IP can still be checked by DNS name.
        Socket plain = new Socket();
        SSLSocket s;
        try {
            plain.connect(new InetSocketAddress(host, port));
            s = (SSLSocket) socketFactory.createSocket(plain, peerName, port, true);
        } catch (IOException e) {
            plain.close();
            throw e;
        }
        try {
            if (enabledCipherSuites != null && enabledCipherSuites.length > 0)
                s.setEnabledCipherSuites(enabledCipherSuites);
            if (!insecureTrustAll) {
                SSLParameters params = s.getSSLParameters();
                params.setEndpointIdentificationAlgorithm("HTTPS");
                s.setSSLParameters(params);
            }
            s.startHandshake();
            return s;
        } catch (IOException e) {
            s.close();
            throw e;
        }
    }

    public String getServerName() {
        return serverName;
    }

    public boolean getClientAuthNeeded() {
        return clientAuthNeeded;
    }

    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

}
