package com.base.iso.interfaces.connector;
import java.io.IOException;

public interface iBaseChannel {
    public String getHost ();
    public void setHost (String host);
    public int    getPort ();
    public void setPort (int port);
    public boolean isConnected();
    public void connect () throws IOException;
    public void disconnect () throws IOException;
    public void reconnect () throws IOException;
}

