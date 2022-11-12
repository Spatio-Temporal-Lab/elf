package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.ChimpDecompressor;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.*;

public class ElfOnChimpDecompressor {
    private long storedVal = 0;
    private int exp;
    private int fn;
    private int eraser_bits;
    private int precision;
    private int flag;
    private final int EXPONENTIAL_DIGIT = 52;

    private InputBitStream in;
    private ChimpDecompressor chimpDecompressor;


    public ElfOnChimpDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
        chimpDecompressor = new ChimpDecompressor(in);
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
        if (chimpDecompressor.getEndOfStream()) {
            return null;
        }
        if(flag==1){
            return decompressRawValue();
        }
        else{
            return Double.longBitsToDouble(storedVal);
        }

    }

    private void next() throws IOException {
        flag = in.readBit();
        if (flag != 0) {
            precision = in.readInt(4);
        }
        Long store = chimpDecompressor.readLongValue();
        if (!chimpDecompressor.getEndOfStream()) {
            storedVal=store;
        }
    }

    private Double decompressRawValue() {
        exp = getExpValue(storedVal);
        fn = computeFn(precision);
        precision = precision+getLeadingZeroDigits(Double.longBitsToDouble(storedVal));
        eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
        if(flag==1){
            BigDecimal rd = BigDecimal.valueOf(Double.longBitsToDouble(storedVal));
            return rd.setScale(precision, RoundingMode.UP).doubleValue();
        }else{
            return Double.longBitsToDouble(storedVal);
        }
    }
}
