package com.base.iso.interfaces;

/**
 *
 */
public interface iTagMapper {

    public String getTagForField(int fieldNumber, int subFieldNumber);

    public Integer getFieldNumberForTag(int fieldNumber, String tag);

}
