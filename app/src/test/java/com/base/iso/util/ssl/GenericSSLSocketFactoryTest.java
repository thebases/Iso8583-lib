package com.base.iso.util.ssl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.SSLSocket;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.base.iso.config.ConfigurationException;
import com.base.iso.config.SimpleConfiguration;

/**
 * T11 — TLS is secure by default: untrusted certs and host-name mismatches are rejected,
 * trust-all only on explicit opt-in, key material injected as KeyStore (no file paths).
 * A self-signed server cert (SAN dns:localhost) is generated with the JDK keytool.
 * (Test code may read files; the library may not.)
 */
class GenericSSLSocketFactoryTest {

    private static final char[] PW = "changeit".toCharArray();
    private static KeyStore serverKs;
    private static KeyStore trust;
    private static ServerSocket server;
    private static Thread acceptor;

    @BeforeAll
    static void startServer(@TempDir Path dir) throws Exception {
        File p12 = dir.resolve("server.p12").toFile();
        String keytool = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
        Process pr = new ProcessBuilder(keytool, "-genkeypair", "-alias", "srv", "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "2", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost",
                "-storetype", "PKCS12", "-keystore", p12.getAbsolutePath(),
                "-storepass", new String(PW), "-keypass", new String(PW))
                .redirectErrorStream(true).start();
        pr.getInputStream().readAllBytes();
        assertEquals(0, pr.waitFor(), "keytool failed");

        serverKs = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(p12)) {
            serverKs.load(in, PW);
        }
        trust = KeyStore.getInstance(KeyStore.getDefaultType());
        trust.load(null, null);
        trust.setCertificateEntry("srv", serverKs.getCertificate("srv"));

        GenericSSLSocketFactory sf = new GenericSSLSocketFactory();
        sf.setKeyStore(serverKs, PW);
        server = sf.createServerSocket(0);
        acceptor = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket s = server.accept()) {
                    ((SSLSocket) s).startHandshake();
                    s.getOutputStream().write(42);
                    s.getOutputStream().flush();
                } catch (IOException ignored) {
                    // client rejected the cert / closed — expected in negative tests
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    @AfterAll
    static void stopServer() throws Exception {
        server.close();
    }

    private static int port() {
        return server.getLocalPort();
    }

    @Test
    void defaultRejectsUntrustedCertificate() {
        GenericSSLSocketFactory f = new GenericSSLSocketFactory(); // platform trust store
        assertThrows(IOException.class, () -> f.createSocket("localhost", port()));
    }

    @Test
    void injectedTrustStoreAccepted() throws Exception {
        GenericSSLSocketFactory f = new GenericSSLSocketFactory();
        f.setTrustStore(trust);
        try (Socket s = f.createSocket("localhost", port())) {
            assertEquals(42, s.getInputStream().read());
        }
    }

    @Test
    void hostNameMismatchRejected() {
        GenericSSLSocketFactory f = new GenericSSLSocketFactory();
        f.setTrustStore(trust);
        // cert names only dns:localhost; connecting by IP without serverName must fail
        assertThrows(IOException.class, () -> f.createSocket("127.0.0.1", port()));
        f.setServerName("wrong.example");
        assertThrows(IOException.class, () -> f.createSocket("localhost", port()));
    }

    @Test
    void serverNameOverrideAllowsConnectingByIp() throws Exception {
        GenericSSLSocketFactory f = new GenericSSLSocketFactory();
        f.setTrustStore(trust);
        f.setServerName("localhost");
        try (Socket s = f.createSocket("127.0.0.1", port())) {
            assertEquals(42, s.getInputStream().read());
        }
    }

    @Test
    void trustAllOnlyOnExplicitOptIn() throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration(new Properties());
        cfg.put("insecure-trust-all", "true");
        SunJSSESocketFactory f = new SunJSSESocketFactory(); // legacy name, same secure class
        f.setConfiguration(cfg);
        assertTrue(f.isInsecureTrustAll());
        try (Socket s = f.createSocket("127.0.0.1", port())) {
            assertEquals(42, s.getInputStream().read());
        }
    }

    @Test
    void keystorePathConfigIsRejected() {
        SimpleConfiguration cfg = new SimpleConfiguration(new Properties());
        cfg.put("keystore", "/home/user/.keystore");
        assertThrows(ConfigurationException.class, () -> new GenericSSLSocketFactory().setConfiguration(cfg));
    }
}
