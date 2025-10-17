package com.base.iso.interfaces.log;

import com.base.iso.util.log.Logger;

public interface iLogSource {
    public void setLogger (Logger logger, String realm);
    public String getRealm ();
    public Logger getLogger ();
}

