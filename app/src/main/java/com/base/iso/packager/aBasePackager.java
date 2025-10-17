package com.base.iso.packager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BitMapComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.ISOMsg;
import com.base.iso.fields.base.BitMapFieldBase;
import com.base.iso.fields.base.MsgFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.interfaces.iPackager;
import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.util.ISOUtil;
import com.base.iso.util.log.LogEvent;
import com.base.iso.util.log.Logger;

/**
 * provides base functionality for the actual packagers
 *
 * @author apr
 */
@SuppressWarnings ("unused")
public abstract class aBasePackager implements iPackager, iLogSource {
    protected aISOFieldBase[] fld;

    protected Logger logger = null;
    protected String realm = null;
    protected int headerLength = 0;
    
    public void setFieldPackager (aISOFieldBase[] fld) {
        this.fld = fld;
    }
    /**
     * @return true if BitMap have to be emited
     */
    protected boolean emitBitMap () {
        return (fld[1] instanceof BitMapFieldBase);
    }
    /**
     * usually 2 for normal fields, 1 for bitmap-less
     * or ANSI X9.2 
     * @return first valid field
     */
    protected int getFirstField() {
        if ((!(fld[0] instanceof MsgFieldBase) ) && fld.length > 1)
            return (fld[1] instanceof BitMapFieldBase) ? 2 : 1;
        return 0;
    }
    /**
     * @param   m   the Component to pack
     * @return      Message image
     * @exception BaseException
     */
    public byte[] pack (BaseComponent m) throws BaseException {
        LogEvent evt = null;
        if (logger != null)
            evt = new LogEvent (this, "pack");
        try {
            if (m.getComposite() != m) 
                throw new BaseException ("Can't call packager on non Composite");

            BaseComponent c;
            ArrayList<byte[]> v = new ArrayList<byte[]>(128);
            Map fields = m.getChildren();
            int len = 0;
            int first = getFirstField();

            c = (BaseComponent) fields.get (0);
            byte[] b;

            if (m instanceof ISOMsg && headerLength>0) 
            {
            	byte[] h = ((ISOMsg) m).getHeader();
            	if (h != null) 
            		len += h.length;
            }
            
            if (first > 0 && c != null) {
                b = fld[0].pack(c);
                len += b.length;
                v.add (b);
            }

            if (emitBitMap()) {
                // BITMAP (-1 in HashTable)
                c = (BaseComponent) fields.get (-1);
                b = getBitMapfieldPackager().pack(c);
                len += b.length;
                v.add (b);
            }

            // if Field 1 is a BitMap then we are packing an
            // ISO-8583 message so next field is fld#2.
            // else we are packing an ANSI X9.2 message, first field is 1
            int tmpMaxField=Math.min (m.getMaxField(), 128);

            for (int i=first; i<=tmpMaxField; i++) {
                if ((c=(BaseComponent) fields.get (i)) != null)
                {
                    try {
                        aISOFieldBase fp = fld[i];
                        if (fp == null)
                            throw new BaseException ("null field "+i+" packager");
                        b = fp.pack(c);
                        len += b.length;
                        v.add (b);
                    } catch (BaseException e) {
                        if (evt != null) {
                            evt.addMessage ("error packing field "+i);
                            evt.addMessage (c);
                            evt.addMessage (e);
                        }
                        throw new BaseException("error packing field "+i, e);
                    }
                }
            }
        
            if(m.getMaxField()>128 && fld.length > 128) {
                for (int i=1; i<=64; i++) {
                    if ((c = (BaseComponent) 
                        fields.get (i + 128)) != null)
                    {
                        try {
                            b = fld[i+128].pack(c);
                            len += b.length;
                            v.add (b);
                        } catch (BaseException e) {
                            if (evt != null) {
                                evt.addMessage ("error packing field "+(i+128));
                                evt.addMessage (c);
                                evt.addMessage (e);
                            }
                            throw e;
                        }
                    }
                }
            }

            int k = 0;
            byte[] d = new byte[len];
            
            // if ISOMsg insert header 
            if (m instanceof ISOMsg && headerLength>0) 
            {
            	byte[] h = ((ISOMsg) m).getHeader();
            	if (h != null) {
                    System.arraycopy(h, 0, d, k, h.length);
                    k += h.length;
                }
            }
            for (byte[] bb : v) {
                System.arraycopy(bb, 0, d, k, bb.length);
                k += bb.length;
            }
            if (evt != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (d));
            return d;
        } catch (BaseException e) {
            if (evt != null)
                evt.addMessage (e);
            throw e;
        } finally {
            if (evt != null)
                Logger.log(evt);
        }
    }

    /**
     * @param   m   the Container of this message
     * @param   b   ISO message image
     * @return      consumed bytes
     * @exception BaseException
     */
    public int unpack (BaseComponent m, byte[] b) throws BaseException {
        LogEvent evt = new LogEvent (this, "unpack");
        int consumed = 0;

        try {
            if (m.getComposite() != m) 
                throw new BaseException ("Can't call packager on non Composite");
            if (logger != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (b));

            
            // if ISOMsg and headerLength defined 
            if (m instanceof ISOMsg /*&& ((ISOMsg) m).getHeader()==null*/ && headerLength>0) 
            {
            	byte[] h = new byte[headerLength];
                System.arraycopy(b, 0, h, 0, headerLength);
            	((ISOMsg) m).setHeader(h);
            	consumed += headerLength;
            }       
            
            if (!(fld[0] == null) && !(fld[0] instanceof BitMapFieldBase))
            {
                BaseComponent mti = fld[0].createComponent(0);
                consumed  += fld[0].unpack(mti, b, consumed);
                m.set (mti);
            }
            BitSet bmap = null;
            int maxField = fld.length;
            if (emitBitMap()) {
                BitMapComponent bitmap = new BitMapComponent (-1);
                consumed += getBitMapfieldPackager().unpack(bitmap,b,consumed);
                bmap = (BitSet) bitmap.getValue();
                if (logger != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);
                maxField = Math.min(maxField, bmap.size());
            }
            for (int i=getFirstField(); i<maxField; i++) {
                try {
                    if (bmap == null && fld[i] == null)
                        continue;
                    if (maxField > 128 && i==65)
                        continue;   // ignore extended bitmap

                    if (bmap == null || bmap.get(i)) {
                        if (fld[i] == null)
                            throw new BaseException ("field packager '" + i + "' is null");

                        BaseComponent c = fld[i].createComponent(i);
                        consumed += fld[i].unpack (c, b, consumed);
                        if (logger != null) {
                            evt.addMessage ("<unpack fld=\"" + i 
                                +"\" packager=\""
                                +fld[i].getClass().getName()+ "\">");
                            if (c.getValue() instanceof ISOMsg)
                                evt.addMessage (c.getValue());
                            else if (c.getValue() instanceof byte[]) {
                                evt.addMessage ("  <value type='binary'>" 
                                    +ISOUtil.hexString((byte[]) c.getValue())
                                    + "</value>");
                            }
                            else {
                                evt.addMessage ("  <value>" 
                                    +c.getValue()
                                    + "</value>");
                            }
                            evt.addMessage ("</unpack>");
                        }
                        m.set(c);
                    }
                } catch (BaseException e) {
                    evt.addMessage (
                        "error unpacking field " + i + " consumed=" + consumed
                    );
                    evt.addMessage (e);
                    e = new BaseException (
                        String.format ("%s (%s) unpacking field=%d, consumed=%d",
                        e.getMessage(), e.getNested().toString(), i, consumed)
                    );
                    throw e;
                }
            }
            if (b.length != consumed) {
                evt.addMessage (
                    "WARNING: unpack len=" +b.length +" consumed=" +consumed
                );
            }
            return consumed;
        } catch (BaseException e) {
            evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new BaseException (e.getMessage() + " consumed=" + consumed);
        } finally {
            Logger.log (evt);
        }
    }
    public void unpack (BaseComponent m, InputStream in) 
        throws IOException, BaseException 
    {
        LogEvent evt = new LogEvent (this, "unpack");
        try {
            if (m.getComposite() != m) 
                throw new BaseException ("Can't call packager on non Composite");

            // if ISOMsg and headerLength defined 
            if (m instanceof ISOMsg && ((ISOMsg) m).getHeader()==null && headerLength>0) 
            {
            	byte[] h = new byte[headerLength];
            	in.read(h, 0, headerLength);
            	((ISOMsg) m).setHeader(h);
            }            
            
            
            if (!(fld[0] instanceof MsgFieldBase) &&
                !(fld[0] instanceof BitMapFieldBase))
            {
                BaseComponent mti = fld[0].createComponent(0);
                fld[0].unpack(mti, in);
                m.set (mti);
            }
            BitSet bmap = null;
            int maxField = fld.length;
            if (emitBitMap()) {
                BitMapComponent bitmap = new BitMapComponent (-1);
                getBitMapfieldPackager().unpack(bitmap, in);
                bmap = (BitSet) bitmap.getValue();
                if (logger != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);
                maxField = Math.min(maxField, bmap.size());
            }
                
            for (int i=getFirstField(); i<maxField; i++) {
                if (bmap == null && fld[i] == null)
                    continue;

                if (bmap == null || bmap.get(i)) {
                    if (fld[i] == null)
                        throw new BaseException ("field packager '" + i + "' is null");

                    BaseComponent c = fld[i].createComponent(i);
                    fld[i].unpack (c, in);
                    if (logger != null) {
                        evt.addMessage ("<unpack fld=\"" + i 
                            +"\" packager=\""
                            +fld[i].getClass().getName()+ "\">");
                        if (c.getValue() instanceof ISOMsg)
                            evt.addMessage (c.getValue());
                        else
                            evt.addMessage ("  <value>" 
                                +c.getValue().toString()
                                + "</value>");
                        evt.addMessage ("</unpack>");
                    }
                    m.set(c);
                }
            }
            if (bmap != null && bmap.get(65) && fld.length > 128 &&
                fld[65] instanceof BitMapFieldBase)
            {
                bmap= (BitSet) ((BaseComponent) m.getChildren().get(65)).getValue();
                for (int i=1; i<64; i++) {
                    if (bmap == null || bmap.get(i)) {
                        BaseComponent c = fld[i+128].createComponent(i);
                        fld[i+128].unpack (c, in);
                        if (logger != null) {
                            evt.addMessage ("<unpack fld=\"" + i+128
                                +"\" packager=\""
                                +fld[i+128].getClass().getName()+ "\">");
                            evt.addMessage ("  <value>" 
                                +c.getValue().toString()
                                + "</value>");
                            evt.addMessage ("</unpack>");
                        }
                        m.set(c);
                    }
                }
            }
        } catch (BaseException e) {
            evt.addMessage (e);
            throw e;
        } catch (EOFException e) {
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new BaseException (e);
        } finally {
            Logger.log (evt);
        }
    }
    /**
     * @param   m   the Container (i.e. an ISOMsg)
     * @param   fldNumber the Field Number
     * @return  Field Description
     */
    public String getFieldDescription(BaseComponent m, int fldNumber) {
        return fld[fldNumber].getDescription();
    }
    /**
     * @param   fldNumber the Field Number
     * @return  Field Packager for this field
     */
    public aISOFieldBase getFieldPackager (int fldNumber) {
        return fld != null && fldNumber < fld.length ? fld[fldNumber] : null;
    }
    /**
     * @param   fldNumber the Field Number
     * @param   fieldPackager the Field Packager
     */
    public void setFieldPackager 
        (int fldNumber, aISOFieldBase fieldPackager) 
    {
        fld[fldNumber] = fieldPackager;
    }
    public ISOMsg createISOMsg () {
        return new ISOMsg();
    }
    /**
     * @return 128 for ISO-8583, should return 64 for ANSI X9.2
     */
    protected int getMaxValidField() {
        return 128;
    }
    /**
     * @return suitable ISOFieldPackager for Bitmap
     */
    protected aISOFieldBase getBitMapfieldPackager() {
        return fld[1];
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
    public int getHeaderLength ()
    {
    	return headerLength;
    }
    public void setHeaderLength(int len)
    {
    	headerLength = len;
    }
    public String getDescription () {
        return getClass().getName();
    }
}
