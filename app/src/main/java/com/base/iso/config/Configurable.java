package com.base.iso.config;


/**
 * Object is Configurable
 */
public interface Configurable {
   /**
    * @param cfg Configuration object
    * @throws ConfigurationException
    */
   void setConfiguration(Configuration cfg)
        throws ConfigurationException;
}
