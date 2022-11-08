package org.urbcomp.startdb.compress.elf;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.Decompressor;
import fi.iki.yak.ts.compression.gorilla.Value;
import sun.misc.DoubleConsts;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.*;

public class ElfOnGorillaDecompressor {
    private long storedVal = 0;
    private int exp;
    private int fn;
    private int eraser_bits;
    private int precision;
    private boolean flag;
    private final int EXPONENTIAL_DIGIT = 52;

    private ByteBufferBitInput in;
    private Decompressor gorillaDecompressor;

    public ElfOnGorillaDecompressor(ByteBufferBitInput in) {
        this.in = in;
        gorillaDecompressor = new Decompressor(in);
    }

    public List<Double> getValues() {
        List<Double> list = new LinkedList<>();
        Double value = readValue();
        while (value != null) {
            list.add(value);
            value = readValue();
        }
        return list;
    }

    public Double readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (gorillaDecompressor.getEndOfStream()) {
            return null;
        }
        if(flag){
            return decompressRawValue();
        }
        else{
            return Double.longBitsToDouble(storedVal);
        }

    }

    private void next() throws IOException {
        flag = in.readBit();
        if (flag) {
            precision = (int) in.getLong(4);
        }
        Value store = gorillaDecompressor.readPair();
        if (!gorillaDecompressor.getEndOfStream()) {
            storedVal=store.getLongValue();
        }
    }

    private Double decompressRawValue() {
        exp = getExpValue(storedVal);
        fn = computeFn(precision);
        precision = precision+getLeadingZeroDigits(Double.longBitsToDouble(storedVal));
        eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
        if(flag){
            BigDecimal rd = BigDecimal.valueOf(Double.longBitsToDouble(storedVal));
            return rd.setScale(precision, RoundingMode.UP).doubleValue();
        }else{
            return Double.longBitsToDouble(storedVal);
        }
    }

}
