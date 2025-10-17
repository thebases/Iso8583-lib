package com.base.iso.models;

import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.connector.filter.iBaseFilter;
import com.base.iso.interfaces.connector.filter.iFilteredConnector;
import com.base.iso.interfaces.connector.filter.iBaseFilter.VetoException;
import com.base.iso.util.log.LogEvent;

/**
 * Filtered Channel Base 
 * @see iFilteredConnector
 */

@SuppressWarnings("unchecked")
public abstract class aFilteredBase extends Observable
    implements iFilteredConnector, Cloneable
{
    protected Vector incomingFilters, outgoingFilters;

    public aFilteredBase () {
        super();
        incomingFilters = new Vector();
        outgoingFilters = new Vector();
    }

    /**
     * @param filter filter to add
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    private void addFilter (iBaseFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING :
                incomingFilters.add (filter);
                break;
            case ISOMsg.OUTGOING :
                outgoingFilters.add (filter);
                break;
            case 0 :
                incomingFilters.add (filter);
                outgoingFilters.add (filter);
                break;
        }
    }
    /**
     * @param filter incoming filter to add
     */
    public void addIncomingFilter (iBaseFilter filter) {
        addFilter (filter, ISOMsg.INCOMING);
    }
    /**
     * @param filter outgoing filter to add
     */
    public void addOutgoingFilter (iBaseFilter filter) {
        addFilter (filter, ISOMsg.OUTGOING);
    }

    /**
     * @param filter filter to add (both directions, incoming/outgoing)
     */
    public void addFilter (iBaseFilter filter) {
        addFilter (filter, 0);
    }
    /**
     * @param filter filter to remove
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    private void removeFilter (iBaseFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING :
                incomingFilters.remove (filter);
                break;
            case ISOMsg.OUTGOING :
                outgoingFilters.remove (filter);
                break;
            case 0 :
                incomingFilters.remove (filter);
                outgoingFilters.remove (filter);
                break;
        }
    }
    /**
     * @param filter filter to remove (both directions)
     */
    public void removeFilter (iBaseFilter filter) {
        removeFilter (filter, 0);
    }
    /**
     * @param filter incoming filter to remove
     */
    public void removeIncomingFilter (iBaseFilter filter) {
        removeFilter (filter, ISOMsg.INCOMING);
    }
    /**
     * @param filter outgoing filter to remove
     */
    public void removeOutgoingFilter (iBaseFilter filter) {
        removeFilter (filter, ISOMsg.OUTGOING);
    }
    protected ISOMsg applyOutgoingFilters (ISOMsg m, LogEvent evt) 
        throws VetoException
    {
        Iterator iter  = outgoingFilters.iterator();
        while (iter.hasNext()) {
            m.setDirection(ISOMsg.OUTGOING);
            m = ((iBaseFilter) iter.next()).filter (this, m, evt);
        }
        m.setDirection(ISOMsg.OUTGOING);
        setChanged ();
        notifyObservers (m);
        return m;
    }
    protected ISOMsg applyIncomingFilters (ISOMsg m, LogEvent evt) 
        throws VetoException
    {
        Iterator iter  = incomingFilters.iterator();
        while (iter.hasNext()) {
            m.setDirection(ISOMsg.INCOMING);
            m = ((iBaseFilter) iter.next()).filter (this, m, evt);
        }
        m.setDirection(ISOMsg.INCOMING);
        setChanged ();
        notifyObservers (m);
        return m;
    }
    public Collection getIncomingFilters() {
        return incomingFilters;
    }
    public Collection getOutgoingFilters() {
        return outgoingFilters;
    }
    public void setIncomingFilters (Collection filters) {
        incomingFilters = new Vector (filters);
    }
    public void setOutgoingFilters (Collection filters) {
        outgoingFilters = new Vector (filters);
    }
    
    public Object clone(){
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        throw new InternalError();
      }
    }
}

