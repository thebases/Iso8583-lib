package com.base.iso.interfaces.connector.filter;

import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.connector.iConnector;
import com.base.iso.util.log.LogEvent;

/**
 * An iBaseFilter has the oportunity to modify an incoming or
 * outgoing ISOMsg that is about to go thru an ISOChannel.
 * It also has the chance to Veto by throwing an Exception 
 */
public interface iBaseFilter {
    public class VetoException extends BaseException {

        private static final long serialVersionUID = -4640160572663583113L;
        public VetoException () {
            super();
        }
        public VetoException (String detail) {
            super(detail);
        }
        public VetoException (Exception nested) {
            super(nested);
        }
        public VetoException (String detail, Exception nested) {
            super(detail, nested);
        }
    }
    /**
     * @param channel current ISOChannel instance
     * @param m ISOMsg to filter
     * @param evt LogEvent
     * @return an ISOMsg (possibly parameter m)
     * @throws VetoException
     */
    public ISOMsg filter (iConnector channel, ISOMsg m, LogEvent evt) 
        throws VetoException;
}
