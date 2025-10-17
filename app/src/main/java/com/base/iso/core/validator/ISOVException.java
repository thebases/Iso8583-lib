
package com.base.iso.core.validator;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;

/**
 * This type of exception is raised while validating ISOComponents.
 * Contains an error component instance referencing to the error. 
 */
public class ISOVException extends BaseException {

    private static final long serialVersionUID = 8609716526640071611L;
    public ISOVException( String Description ) {
        super( Description );
    }

    public ISOVException( String Description, BaseComponent errComponent ) {
        super( Description );
        this.errComponent = errComponent;
    }

    public BaseComponent getErrComponent(){
        return this.errComponent;
    }

    public boolean treated() {
        return treated;
    }

    public void setErrComponent( BaseComponent c ){
        this.errComponent = c;
    }

    public void setTreated( boolean Treated ){
        treated = Treated;
    }

    /** flag indicating if the exception was catched in any
     * try/catch clause. It is used to determine if it is
     * necessary the replacement of the component by the
     * iso-error-component in the exception instance**/
    protected boolean treated = false;
    protected BaseComponent errComponent;
}