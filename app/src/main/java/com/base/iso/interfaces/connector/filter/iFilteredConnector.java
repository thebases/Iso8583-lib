
package com.base.iso.interfaces.connector.filter;

import java.util.Collection;

import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.interfaces.connector.iServerConnector;

/**
 * Filtered Channel 
 * @see iConnector
 * @see iServerConnector
 */

public interface iFilteredConnector extends iConnector {
    /**
     * @param filter incoming filter to add
     */
    public void addIncomingFilter (iBaseFilter filter);

    /**
     * @param filter outgoing filter to add
     */
    public void addOutgoingFilter (iBaseFilter filter);

    public void addFilter (iBaseFilter filter);

    /**
     * @param filter filter to remove (both directions)
     */
    public void removeFilter (iBaseFilter filter);

    /**
     * @param filter incoming filter to remove
     */
    public void removeIncomingFilter (iBaseFilter filter);

    /**
     * @param filter outgoing filter to remove
     */
    public void removeOutgoingFilter (iBaseFilter filter);

   /**
    * @return Collection containing all incoming filters
    */
    public Collection getIncomingFilters();

   /**
    * @return Collection containing all outgoing filters
    */
    public Collection getOutgoingFilters();

   /**
    * @param filters incoming filter set
    */
    public void setIncomingFilters (Collection filters);

   /**
    * @param filters outgoing filter set
    */
    public void setOutgoingFilters (Collection filters);
}

