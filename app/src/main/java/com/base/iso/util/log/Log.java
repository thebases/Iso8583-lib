package com.base.iso.util.log;

import com.base.iso.interfaces.log.iLogSource;

/**
 * Represents a LogSource and adds several helpers 
 * @see iLogSource
 */
public class Log implements iLogSource {
    protected Logger logger;
    protected String realm;

    public static final String TRACE   = "trace";
    public static final String DEBUG   = "debug";
    public static final String INFO    = "info";
    public static final String WARN    = "warn";
    public static final String ERROR   = "error";
    public static final String FATAL   = "fatal";

    public Log () {
        super();
    }
    public static Log getLog (String logName, String realm) {
        return new Log (Logger.getLogger (logName), realm);
    }
    public Log (Logger logger, String realm) {
        setLogger (logger, realm);
    }
    public void setLogger (Logger logger, String realm) {
        this.logger = logger;
        this.realm  = realm;
    }
    public String getRealm () {
        return realm;
    }
    public Logger getLogger() {
        return logger;
    }
    public void setLogger (Logger logger) {
        this.logger = logger;
    }
    public void setRealm (String realm) {
        this.realm = realm;
    }
    public void trace (Object detail) {
        Logger.log (createTrace (detail));
    }
    public void trace (Object detail, Object obj) {
        LogEvent evt = createTrace (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public void debug (Object detail) {
        Logger.log (createDebug (detail));
    }
    public void debug (Object detail, Object obj) {
        LogEvent evt = createDebug (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public void info (Object detail) {
        Logger.log (createInfo (detail));
    }
    public void info (Object detail, Object obj) {
        LogEvent evt = createInfo (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public void warn (Object detail) {
        Logger.log (createWarn (detail));
    }
    public void warn (Object detail, Object obj) {
        LogEvent evt = createWarn (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public void error (Object detail) {
        Logger.log (createError (detail));
    }
    public void error (Object detail, Object obj) {
        LogEvent evt = createError (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public void fatal (Object detail) {
        Logger.log (createFatal (detail));
    }
    public void fatal (Object detail, Object obj) {
        LogEvent evt = createFatal (detail);
        evt.addMessage (obj);
        Logger.log (evt);
    }
    public LogEvent createLogEvent (String level) {
        return new LogEvent (this, level);
    }
    public LogEvent createLogEvent (String level, Object detail) {
        return new LogEvent (this, level, detail);
    }
    public LogEvent createTrace () {
        return createLogEvent (TRACE);
    }
    public LogEvent createTrace (Object detail) {
        return createLogEvent (TRACE, detail);
    }
    public LogEvent createDebug() {
        return createLogEvent (DEBUG);
    }
    public LogEvent createDebug(Object detail) {
        return createLogEvent (DEBUG, detail);
    }
    public LogEvent createInfo () {
        return createLogEvent (INFO);
    }
    public LogEvent createInfo (Object detail) {
        return createLogEvent (INFO, detail);
    }
    public LogEvent createWarn () {
        return createLogEvent (WARN);
    }
    public LogEvent createWarn (Object detail) {
        return createLogEvent (WARN, detail);
    }
    public LogEvent createError () {
        return createLogEvent (ERROR);
    }
    public LogEvent createError (Object detail) {
        return createLogEvent (ERROR, detail);
    }
    public LogEvent createFatal () {
        return createLogEvent (FATAL);
    }
    public LogEvent createFatal (Object detail) {
        return createLogEvent (FATAL, detail);
    }
}

