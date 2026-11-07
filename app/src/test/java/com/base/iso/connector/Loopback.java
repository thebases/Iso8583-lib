package com.base.iso.connector;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Test helper: a one-connection loopback "host". The connector under test connects to it as a
 * client; the test reads the exact bytes put on the wire and scripts the bytes sent back.
 */
final class Loopback implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Future<Socket> accepted;
    private Socket peer;

    Loopback() throws IOException {
        server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
        accepted = exec.submit(server::accept);
    }

    String host() {
        return server.getInetAddress().getHostAddress();
    }

    int port() {
        return server.getLocalPort();
    }

    /** Peer side of the connection (waits for the connector's connect()). */
    Socket peer() throws Exception {
        if (peer == null) {
            peer = accepted.get(5, TimeUnit.SECONDS);
            peer.setSoTimeout(5000);
        }
        return peer;
    }

    /** Reads exactly n bytes the connector sent. */
    byte[] read(int n) throws Exception {
        byte[] b = new byte[n];
        new DataInputStream(peer().getInputStream()).readFully(b);
        return b;
    }

    /** Writes a scripted frame to the connector. */
    void write(byte[] b) throws Exception {
        OutputStream out = peer().getOutputStream();
        out.write(b);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            if (peer != null)
                peer.close();
        } finally {
            server.close();
            exec.shutdownNow();
        }
    }
}
