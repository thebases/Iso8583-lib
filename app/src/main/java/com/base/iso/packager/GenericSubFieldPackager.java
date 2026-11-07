package com.base.iso.packager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import com.base.iso.core.BaseComponent;
import com.base.iso.core.BaseException;
import com.base.iso.core.BitMapComponent;
import com.base.iso.core.ISOField;
import com.base.iso.core.ISOMsg;
import com.base.iso.fields.base.BitMapFieldBase;
import com.base.iso.fields.base.aISOFieldBase;
import com.base.iso.interfaces.iSubFieldBase;

/**
 * Packs the sub-fields INSIDE one ISO field (e.g. F48 / F63 private data). Wrapped by {@link com.base.iso.fields.base.MsgFieldBase},
 * which adds the outer field's own encoding / length prefix:
 * <pre>
 *   aISOFieldBase f63 = new MsgFieldBase(new IFB_LLLBINARY(999, "F63"),
 *           new GenericSubFieldPackager(63, subFields, true, 0, -1));
 * </pre>
 * Usually built from a layout ({@code <isofieldpackager>} in XML, {@code "subfields"} in JSON)
 * by {@link GenericJSONPackager}.
 *
 * Two modes:
 * <ul>
 *   <li><b>Bitmapped</b> — a {@link BitMapFieldBase} codec sits at {@code bitmapField}; bit i of
 *       that bitmap = sub-field i present. Sub-fields are packed in ascending order after it.
 *       Keep the bitmap codec ≤ 8 bytes when sub-field 1 is data: with {@code IFB_BITMAP}, bit 1
 *       set in a longer bitmap is read as "secondary bitmap follows".</li>
 *   <li><b>Positional</b> — no bitmap; sub-fields are packed back to back in id order.
 *       On pack, a missing sub-field BEFORE the highest present one is packed as an empty value
 *       (so later positions stay aligned). On unpack, decoding stops cleanly when
 *       the data ends, so optional trailing sub-fields may be omitted.</li>
 * </ul>
 * Stateless during pack/unpack → shareable across threads once built.
 */
public class GenericSubFieldPackager extends aBasePackager implements iSubFieldBase {

    private final int fieldNumber;
    private final boolean bitmapped;
    private final int bitmapField;
    private final int firstField;

    /**
     * @param fieldNumber parent field number (e.g. 63), reported via {@link #getFieldNumber()}
     * @param fields      sub-field codecs indexed by sub-field id (null = unused id)
     * @param bitmapped   true when {@code fields[bitmapField]} is the sub-field bitmap
     * @param bitmapField index of the bitmap codec (bitmapped mode only)
     * @param firstField  first data sub-field id; -1 = default (bitmapField + 1 when bitmapped,
     *                    otherwise the lowest defined id)
     * @throws IllegalArgumentException if the configuration is inconsistent
     */
    public GenericSubFieldPackager(int fieldNumber, aISOFieldBase[] fields, boolean bitmapped,
                                   int bitmapField, int firstField) {
        super();
        if (fields == null || fields.length == 0)
            throw new IllegalArgumentException("sub-field layout of field " + fieldNumber + " is empty");
        this.fieldNumber = fieldNumber;
        this.bitmapped = bitmapped;
        this.bitmapField = bitmapField;
        if (bitmapped) {
            if (bitmapField < 0 || bitmapField >= fields.length || !(fields[bitmapField] instanceof BitMapFieldBase))
                throw new IllegalArgumentException("field " + fieldNumber + ": bitmapped sub-layout needs a bitmap codec at id " + bitmapField);
            this.firstField = firstField >= 0 ? firstField : bitmapField + 1;
        } else {
            int lowest = -1;
            for (int i = 0; i < fields.length && lowest < 0; i++)
                if (fields[i] != null)
                    lowest = i;
            if (lowest < 0)
                throw new IllegalArgumentException("sub-field layout of field " + fieldNumber + " has no fields");
            this.firstField = firstField >= 0 ? firstField : lowest;
        }
        if (bitmapped && this.firstField <= bitmapField)
            throw new IllegalArgumentException("field " + fieldNumber + ": firstField must be after the bitmap");
        setFieldPackager(fields.clone());
    }

    /** Parent field number (iSubFieldBase). */
    @Override
    public int getFieldNumber() {
        return fieldNumber;
    }

    public boolean isBitmapped() {
        return bitmapped;
    }

    public int getBitmapField() {
        return bitmapField;
    }

    public int getFirstSubField() {
        return firstField;
    }

    /** @return copy of the sub-field codecs indexed by id */
    public aISOFieldBase[] getSubFields() {
        return fld.clone();
    }

    @Override
    protected boolean emitBitMap() {
        return bitmapped;
    }

    @Override
    protected int getFirstField() {
        return firstField;
    }

    @Override
    protected aISOFieldBase getBitMapfieldPackager() {
        return fld[bitmapField];
    }

    @Override
    public byte[] pack(BaseComponent m) throws BaseException {
        if (m.getComposite() != m)
            throw new BaseException("Can't call packager on non Composite");
        @SuppressWarnings("unchecked")
        Map<Integer, BaseComponent> fields = m.getChildren();
        int max = maxPresent(fields);
        List<byte[]> parts = new ArrayList<>();
        int len = 0;

        // 1. Bitmap (bitmapped mode): bit i = sub-field i present.
        if (bitmapped) {
            BitSet bs = new BitSet();
            for (int i = firstField; i <= max; i++)
                if (fields.get(i) != null)
                    bs.set(i);
            byte[] b = fld[bitmapField].pack(new BitMapComponent(-1, bs));
            parts.add(b);
            len += b.length;
        }

        // 2. Sub-fields in id order.
        for (int i = firstField; i <= max; i++) {
            BaseComponent c = fields.get(i);
            aISOFieldBase codec = i < fld.length ? fld[i] : null;
            if (c == null) {
                if (bitmapped || codec == null)
                    continue;
                c = new ISOField(i, ""); // positional gap: keep later sub-fields aligned
            }
            if (codec == null)
                throw new BaseException("field " + fieldNumber + "." + i + ": no codec in sub-field layout");
            try {
                byte[] b = codec.pack(c);
                parts.add(b);
                len += b.length;
            } catch (BaseException e) {
                throw new BaseException("error packing field " + fieldNumber + "." + i, e);
            }
        }

        byte[] out = new byte[len];
        int k = 0;
        for (byte[] b : parts) {
            System.arraycopy(b, 0, out, k, b.length);
            k += b.length;
        }
        return out;
    }

    @Override
    public int unpack(BaseComponent m, byte[] b) throws BaseException {
        if (m.getComposite() != m)
            throw new BaseException("Can't call packager on non Composite");
        int consumed = 0;
        BitSet bmap = null;
        int maxField = fld.length;

        if (bitmapped) {
            BitMapComponent bitmap = new BitMapComponent(-1);
            consumed += fld[bitmapField].unpack(bitmap, b, 0);
            bmap = (BitSet) bitmap.getValue();
            m.set(bitmap);
            maxField = Math.min(maxField, bmap.length());
        }
        // Positional: stop when the data ends (optional trailing sub-fields).
        for (int i = firstField; i < maxField && consumed < b.length; i++) {
            if (bmap != null && !bmap.get(i))
                continue;
            if (fld[i] == null) {
                if (bmap != null)
                    throw new BaseException("field " + fieldNumber + "." + i + ": bit set but no codec in sub-field layout");
                continue;
            }
            try {
                BaseComponent c = fld[i].createComponent(i);
                consumed += fld[i].unpack(c, b, consumed);
                m.set(c);
            } catch (BaseException e) {
                throw new BaseException("error unpacking field " + fieldNumber + "." + i + " consumed=" + consumed, e);
            } catch (RuntimeException e) { // e.g. data shorter than a fixed-length codec
                throw new BaseException("error unpacking field " + fieldNumber + "." + i + " consumed=" + consumed
                        + ": " + e, e);
            }
        }
        if (bmap != null) {
            int next = bmap.nextSetBit(maxField);
            if (next >= 0)
                throw new BaseException("field " + fieldNumber + "." + next + ": bit set but no codec in sub-field layout");
        }
        // Bytes left after the last known positional sub-field are ignored on purpose: failing
        // here would reject an otherwise valid host response because of one unknown trailing
        // sub-field in private data. Callers needing strictness can compare against b.length.
        return consumed;
    }

    private static int maxPresent(Map<Integer, BaseComponent> fields) {
        int max = -1;
        for (Integer k : fields.keySet())
            if (k != null && k > max)
                max = k;
        return max;
    }

    @Override
    public ISOMsg createISOMsg() {
        ISOMsg m = new ISOMsg(fieldNumber);
        m.setPackager(this);
        return m;
    }
}
