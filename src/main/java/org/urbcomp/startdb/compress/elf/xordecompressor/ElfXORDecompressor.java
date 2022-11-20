package org.urbcomp.startdb.compress.elf.xordecompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElfXORDecompressor {
    private long storedVal = 0;
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public ElfXORDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public List<Double> getValues() {
        List<Double> list = new ArrayList<>(1024);
        Double value = readValue();
        while (value != null) {
            list.add(value);
            value = readValue();
        }
        return list;
    }

    public InputBitStream getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            int trailingZeros = in.readInt(6);
            storedVal = in.readLong(64 - trailingZeros) << trailingZeros;
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        int flag = in.readInt(2);
        int centerBits, trailingZeros;
        long value;
        switch (flag) {
            case 0:
                // case 00
                centerBits = in.readInt(4);
                if (centerBits == 0) {
                    centerBits = 16;
                }
                trailingZeros = 64 - storedLeadingZeros - centerBits;
                value = in.readLong(centerBits) << trailingZeros;
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                } else {
                    storedVal = value;
                }
                break;
            case 1:
                // case 01
//                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
//                centerBits = in.readInt(4);
                int leadingAndCenterBits = in.readInt(7);
                storedLeadingZeros = leadingRepresentation[leadingAndCenterBits >>> 4];
                centerBits = leadingAndCenterBits & 0xf;
                if (centerBits == 0) {
                    centerBits = 16;
                }
                trailingZeros = 64 - storedLeadingZeros - centerBits;
                value = in.readLong(centerBits) << trailingZeros;
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                } else {
                    storedVal = value;
                }
                break;
            case 3:
                if (in.readInt(1) != 0) {
                    // case 111
                    int leadingAndCenter = in.readInt(9);
                    storedLeadingZeros = leadingRepresentation[leadingAndCenter >>> 6];
                    centerBits = leadingAndCenter & 0x3f;
                } else {
                    //case 110
                    centerBits = in.readInt(6);
                }
                if (centerBits == 0) {
                    centerBits = 64;
                }
                trailingZeros = 64 - storedLeadingZeros - centerBits;
                value = in.readLong(centerBits) << trailingZeros;
                value = storedVal ^ value;
                if (value == END_SIGN) {
                    endOfStream = true;
                } else {
                    storedVal = value;
                }
                break;
            default:
                // case 10, we do nothing, the same value as before
                break;
        }
    }
}
