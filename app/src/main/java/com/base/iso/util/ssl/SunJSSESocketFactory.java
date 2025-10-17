package com.base.iso.util.ssl;

import javax.net.ssl.*;

import com.base.iso.config.Configurable;
import com.base.iso.config.Configuration;
import com.base.iso.config.ConfigurationException;
import com.base.iso.core.BaseException;
import com.base.iso.interfaces.iClientSocketFactory;
import com.base.iso.util.log.SimpleLogSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;

/*
 * Re-write to meet Android requirements
 */

public class SunJSSESocketFactory extends SimpleLogSource
        implements ISOServerSocketFactory, iClientSocketFactory, Configurable {

    private SSLContext sslContext;
    private SSLServerSocketFactory serverFactory;
    private SSLSocketFactory socketFactory;

    private String keyStore;
    private String password;
    private String keyPassword;
    private String serverName;
    private boolean clientAuthNeeded = false;
    private boolean serverAuthNeeded = false;
    private String[] enabledCipherSuites;
    private Configuration cfg;

    public ServerSocket createServerSocket(int port) throws IOException, BaseException {
        if (serverFactory == null) serverFactory = createServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverFactory.createServerSocket(port);
        serverSocket.setNeedClientAuth(clientAuthNeeded);
        if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
            serverSocket.setEnabledCipherSuites(enabledCipherSuites);
        }
        return serverSocket;
    }

    public Socket createSocket(String host, int port) throws IOException, BaseException {
        if (socketFactory == null) socketFactory = createSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port);
        verifyHostname(socket);
        return socket;
    }

    private SSLContext getSSLContext() throws BaseException {
        try {
            if (password == null) password = getPassword();
            if (keyPassword == null) keyPassword = getKeyPassword();
            if (keyStore == null || keyStore.isEmpty()) {
                keyStore = System.getProperty("user.home") + File.separator + ".keystore";
            }

            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream fis = new FileInputStream(new File(keyStore));
            ks.load(fis, password.toCharArray());
            fis.close();

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, keyPassword.toCharArray());

            TrustManager[] tma = getTrustManagers(ks);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tma, new SecureRandom());
            return sslContext;

        } catch (Exception e) {
            throw new BaseException("Failed to initialize SSLContext", e);
        } finally {
            password = null;
            keyPassword = null;
        }
    }

    private TrustManager[] getTrustManagers(KeyStore ks) throws GeneralSecurityException {
        if (serverAuthNeeded) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            return tmf.getTrustManagers();
        } else {
            return new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };
        }
    }

    protected SSLServerSocketFactory createServerSocketFactory() throws BaseException {
        if (sslContext == null) sslContext = getSSLContext();
        return sslContext.getServerSocketFactory();
    }

    protected SSLSocketFactory createSocketFactory() throws BaseException {
        if (sslContext == null) sslContext = getSSLContext();
        return sslContext.getSocketFactory();
    }

    private void verifyHostname(SSLSocket socket) throws SSLPeerUnverifiedException, UnknownHostException {
        if (!serverAuthNeeded) return;

        SSLSession session = socket.getSession();
        if (serverName == null || serverName.isEmpty()) {
            serverName = session.getPeerHost();
        }

        X509Certificate[] certs = (X509Certificate[]) session.getPeerCertificates();
        if (certs == null || certs.length == 0) {
            throw new SSLPeerUnverifiedException("No server certificates found");
        }

        String dn = certs[0].getSubjectX500Principal().getName();
        String cn = getCN(dn);
        if (!serverName.equalsIgnoreCase(cn)) {
            throw new SSLPeerUnverifiedException("Invalid server name. Expected '" +
                    serverName + "', got '" + cn + "'");
        }
    }

    private String getCN(String dn) {
        int i = dn.indexOf("CN=");
        if (i == -1) return null;
        dn = dn.substring(i + 3);
        char[] chars = dn.toCharArray();
        for (i = 0; i < chars.length; i++) {
            if (chars[i] == ',' && (i == 0 || chars[i - 1] != '\\')) break;
        }
        return dn.substring(0, i);
    }

    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
        keyStore = cfg.get("keystore");
        clientAuthNeeded = cfg.getBoolean("clientauth");
        serverAuthNeeded = cfg.getBoolean("serverauth");
        serverName = cfg.get("servername");
        password = cfg.get("storepassword", null);
        keyPassword = cfg.get("keypassword", null);
        enabledCipherSuites = cfg.getAll("addEnabledCipherSuite");
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public String getServerName() {
        return serverName;
    }

    public boolean getClientAuthNeeded() {
        return clientAuthNeeded;
    }

    public boolean getServerAuthNeeded() {
        return serverAuthNeeded;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setClientAuthNeeded(boolean clientAuthNeeded) {
        this.clientAuthNeeded = clientAuthNeeded;
    }

    public void setServerAuthNeeded(boolean serverAuthNeeded) {
        this.serverAuthNeeded = serverAuthNeeded;
    }

    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
    }

    public String[] getEnabledCipherSuites() {
        return enabledCipherSuites;
    }

    protected String getPassword() {
        return System.getProperty("base.ssl.storepass", "password");
    }

    protected String getKeyPassword() {
        return System.getProperty("base.ssl.keypass", "password");
    }
}
