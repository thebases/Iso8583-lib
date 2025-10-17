package com.base.iso.interfaces.log;

public interface iLogProducer {
    public void addListener (iLogListener l);
    public void removeListener (iLogListener l);
    public void removeAllListeners ();


}
