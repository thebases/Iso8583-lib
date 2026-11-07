package com.base.iso.core;



import com.base.iso.core.validator.ISOValidator;
import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.util.log.Logger;

/**
 * Base Validator class for composed ISOComponents (ISOMsg). 
 */
public class BaseValidator implements ISOValidator, iLogSource {

    public BaseValidator() {
        super();
    }

    /**
     * Creates the validator.
     * @param breakOnError flag indicating validation abort condition
     */
    public BaseValidator( boolean breakOnError ) {
        this.breakOnError = breakOnError;
    }

    public boolean breakOnError(){
        return breakOnError;
    }

    public void setBreakOnError( boolean breakOnErr ){
        this.breakOnError = breakOnErr;
    }

    /**
     * Validate field-interdependency.
     * @param m Component to validate
     * @return BaseComponent or ISOVComponent resulting of validation process.
     * @throws BaseException if break-on-error is true and an error succedd.
     */
    public BaseComponent validate( BaseComponent m ) throws BaseException{
        if ( m.getComposite() != m )
            throw new BaseException ( "Can't call validate on non Composite" );
        return m;
    }

    public void setLogger( Logger logger, String realm ){
        this.logger = logger;
        this.realm = realm;
    }

    public Logger getLogger(){
        return logger;
    }

    public String getRealm() {
        return realm;
    }

    protected Logger logger = null;
    protected String realm = null;
    /** Flag used to indicate if validat process break
     * on first error or keep an error set **/
    protected boolean breakOnError = false;
   
}