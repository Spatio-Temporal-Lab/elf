package org.urbcomp.startdb.compress.elf.xordecompressor;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElfXORDecompressorPre1O {
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};

    public ElfXORDecompressorPre1O(byte[] bs) {
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
        if (in.readInt(1) == 0) {
            int leadingZeros = leadingRepresentation[in.readInt(3)];
            int centerBits = in.readInt(4);
            if (centerBits == 0) {
                centerBits = 16;
            }
            int trailingZeros = 64 - leadingZeros - centerBits;
            long value = in.readLong(centerBits) << trailingZeros;
            value = storedVal ^ value;
            if (value == END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        } else if (in.readInt(1) == 1) {
            int leadingZeros = leadingRepresentation[in.readInt(3)];
            int centerBits = in.readInt(6);
            if (centerBits == 0) {
                centerBits = 64;
            }
            int trailingZeros = 64 - leadingZeros - centerBits;
            long value = in.readLong(centerBits) << trailingZeros;
            value = storedVal ^ value;
            if (value == END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        }
    }
}
