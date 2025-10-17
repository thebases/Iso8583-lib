package com.base.iso.util;

import java.io.Serializable;

import com.base.iso.core.BaseException;


/**
 * ISO Currency Conversion package 
 */
public class Currency implements Serializable
{
    String alphacode;
    int isocode;
    int numdecimals;

    public Currency(String alphacode, int isocode, int numdecimals)
    {
        this.alphacode = alphacode;
        this.isocode = isocode;
        this.numdecimals = numdecimals;
    }

    public int getDecimals()
    {
        return numdecimals;
    }

    public int getIsoCode()
    {
        return isocode;
    }

    public String getAlphaCode()
    {
        return alphacode;
    }

    public String formatAmountForISOMsg(double amount)
    {
        try
        {
            double m = Math.pow(10, getDecimals()) * amount;
            return ISOUtil.zeropad(String.valueOf(Math.round(m)), 12);
        }
        catch (BaseException e)
        {
            throw new IllegalArgumentException("Failed to convert amount",e);
        }
    }

    public double parseAmountFromISOMsg(String isoamount)
    {
        return new Double(isoamount)/Math.pow(10, getDecimals());
    }

    @Override
    public String toString()
    {
        return "Currency{" +
               "alphacode='" + alphacode + '\'' +
               ", isocode=" + isocode +
               ", numdecimals=" + numdecimals +
               '}';
    }
}
