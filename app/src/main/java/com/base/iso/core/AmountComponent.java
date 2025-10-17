package com.base.iso.core;
import java.io.*;
import java.math.BigDecimal;
import com.base.iso.util.ISOUtil;

public class AmountComponent 
    extends BaseComponent 
    implements Cloneable, Externalizable
{
    static final long serialVersionUID = -6130248734056876225L;
    private int fieldNumber;
    private int currencyCode;
    private String value;
    private BigDecimal amount;

    public AmountComponent () {
        super();
        setFieldNumber (-1);
    }
    public AmountComponent (int fieldNumber) {
        super ();
        setFieldNumber (fieldNumber);
    }
    public AmountComponent (int fieldNumber, int currencyCode, BigDecimal amount) throws BaseException {
        super ();
        setFieldNumber(fieldNumber);
        this.amount = amount.setScale(BaseCurrency.getCurrency(currencyCode).getDecimals());
        this.currencyCode = currencyCode;
    }
    public Object getKey() {
        return fieldNumber;
    }
    public Object getValue() throws BaseException {
        if (value == null) {
            StringBuilder sb = new StringBuilder();
            sb.append (ISOUtil.zeropad (Integer.toString(currencyCode), 3));
            sb.append (Integer.toString (amount.scale()));
            sb.append (
                ISOUtil.zeropad (
                    amount.movePointRight(amount.scale()).toString(),12
                )
            );
            value = sb.toString();
        }
        return value;

    }
    public void setValue (Object obj) throws BaseException {
        if (obj instanceof String) {
            String s = (String) obj;
            if (s.length() < 12) {
                throw new BaseException (
                    "ISOAmount invalid length " + s.length()
                );
            }
            try {
                currencyCode = Integer.parseInt (s.substring(0,3));
                int dec = Integer.parseInt (s.substring(3,4));
                amount = new BigDecimal (s.substring(4)).movePointLeft (dec);
                value  = s;
            } catch (NumberFormatException e) {
                throw new BaseException (e.getMessage());
            }
        }
    }
    public void setFieldNumber (int fieldNumber) {
        this.fieldNumber = fieldNumber;
    }

    @Override
    public int getFieldNumber () {
        return fieldNumber;
    }
    public BigDecimal getAmount () {
        return amount;
    }
    public int getScale() {
        return amount.scale() % 10;
    }
    public String getScaleAsString() {
        return Integer.toString(getScale());
    }
    public int getCurrencyCode() {
        return currencyCode;
    }
    public String getCurrencyCodeAsString() throws BaseException {
        return ISOUtil.zeropad(Integer.toString(currencyCode),3);
    }
    public String getAmountAsLegacyString() throws BaseException {
        return ISOUtil.zeropad (amount.unscaledValue().toString(), 12);
    }
    public String getAmountAsString() throws BaseException {
        StringBuilder sb = new StringBuilder(16);
        sb.append (ISOUtil.zeropad (Integer.toString (currencyCode),3));
        sb.append (Integer.toString(amount.scale() % 10));
        sb.append (ISOUtil.zeropad (amount.unscaledValue().toString(), 12));
        return sb.toString();
    }
    public byte[] pack() throws BaseException {
        throw new BaseException ("Not available");
    }
    public int unpack(byte[] b) throws BaseException {
        throw new BaseException ("Not available");
    }
    public void unpack(InputStream in) throws BaseException {
        throw new BaseException ("Not available");
    }
    public void dump (PrintStream p, String indent) {
        p.println (indent +"<"+BaseConstant.ISOFIELD_TAG + " " 
          +BaseConstant.ID_ATTR +"=\"" +fieldNumber +"\" "
          +"currency=\"" +ISOUtil.zeropad (currencyCode, 3)+"\" "
          +BaseConstant.TYPE_ATTR +"=\"amount\" "
          +BaseConstant.VALUE_ATTR+"=\"" + amount.toString() +"\"/>"
        );
    }
    public void writeExternal (ObjectOutput out) throws IOException {
        out.writeShort (fieldNumber);
        try {
            out.writeUTF ((String) getValue());
        } catch (BaseException e) {
            throw new IOException (e);
        }
    }
    public void readExternal  (ObjectInput in) 
        throws IOException, ClassNotFoundException
    {
        fieldNumber = in.readShort ();
        try {
            setValue(in.readUTF());
        } catch (BaseException e) {
            throw new IOException (e.getMessage());
        }
    }       
}

