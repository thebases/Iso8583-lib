package com.base.iso.util;


import java.util.Timer;


public class DefaultTimer {
    private static volatile Timer defaultTimer = null;

    private DefaultTimer() { }

    public static Timer getTimer() {
        if (defaultTimer == null) {
            synchronized (DefaultTimer.class) {
                if (defaultTimer == null) 
                    defaultTimer = new Timer(true);
            }
        }
        return defaultTimer;
    }
}
