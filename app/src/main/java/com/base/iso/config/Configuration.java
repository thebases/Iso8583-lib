package com.base.iso.config;


import java.util.Set;

/**
 * CardAgents relies on a Configuration object to provide
 * runtime configuration parameters such as merchant number, etc.
 */
public interface Configuration {
    String get(String propertyName);
    /**
     * @param propertyName  ditto
     * @return all properties with a given name (or a zero-length array)
     */
    String[] getAll(String propertyName);
    int[] getInts(String propertyName);
    long[] getLongs(String propertyName);
    double[] getDoubles(String propertyName);
    boolean[] getBooleans(String propertyName);
    String get(String propertyName, String defaultValue);
    int getInt(String propertyName);
    int getInt(String propertyName, int defaultValue);
    long getLong(String propertyName);
    long getLong(String propertyName, long defaultValue);
    double getDouble(String propertyName);
    double getDouble(String propertyName, double defaultValue);
    boolean getBoolean(String propertyName);
    boolean getBoolean(String propertyName, boolean defaultValue);
    /**
     * @param name the Property name
     * @param value typically a String, but could be a String[] too
     */
    void put(String name, Object value);
    Set<String> keySet();
}
