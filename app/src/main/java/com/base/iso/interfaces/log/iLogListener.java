package com.base.iso.interfaces.log;


import java.util.EventListener;

import com.base.iso.util.log.LogEvent;


public interface iLogListener extends EventListener {
    public LogEvent log (LogEvent ev);
}

