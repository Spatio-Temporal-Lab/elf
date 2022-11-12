package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.InputBitStream;
import sun.misc.DoubleConsts;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.computeFn;
public class ElfDecompressor {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;
    private int exp;
    private int fn;
    private int eraser_bits;
    private int precision;
    private final int EXPONENTIAL_DIGIT = 52;
    private final static long NAN_LONG = Double.doubleToRawLongBits(DoubleConsts.MIN_VALUE);
    private InputBitStream in;


    public ElfDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
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
        if(endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }
    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.readLong(64);
            if (storedVal == NAN_LONG) {
                endOfStream = true;
                return;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        int significantBits;
        long value;
        int flag = in.readBit();
        switch (flag){
            case 0:
                in.readLong(64);
                break;
            case 1:
                precision = in.readInt(4);
                fn = computeFn(precision);
                eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
        }
    }

}
