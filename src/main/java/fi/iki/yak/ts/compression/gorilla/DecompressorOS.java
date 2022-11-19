package fi.iki.yak.ts.compression.gorilla;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DecompressorOS {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public DecompressorOS(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public InputBitStream getInputStream() {
        return in;
    }

    public List<Double> getValues() {
        List<Double> list = new LinkedList<>();
        Value value = readPair();
        while (value != null) {
            list.add(value.getDoubleValue());
            value = readPair();
        }
        return list;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readPair() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            first = false;
            storedVal = in.readLong(64);
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }

        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        // Read value
        if (in.readBit() == 1) {
            // else -> same value as before
            if (in.readBit() == 1) {
                // New leading and trailing zeros
                storedLeadingZeros = in.readInt(5);

                int significantBits = in.readInt(6);
                if(significantBits == 0) {
                    significantBits = 64;
                }
                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
            }
            long value = in.readLong(64 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }

        }
    }
}
