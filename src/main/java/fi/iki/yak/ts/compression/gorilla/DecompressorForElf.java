package fi.iki.yak.ts.compression.gorilla;

import java.util.LinkedList;
import java.util.List;

public class DecompressorForElf {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int lastEraseBits = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final BitInput in;

    private final static long END_SIGN = 0x0000000000000001L;

    public DecompressorForElf(BitInput input) {
        in = input;
    }

    public BitInput getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readPair(int eraseBits) {
        next(eraseBits);
        lastEraseBits = eraseBits;
        if (endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next(int eraseBits) {
        if (first) {
            first = false;
            storedVal = in.getLong(64 - eraseBits) << eraseBits;
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue(eraseBits);
        }
    }

    private void nextValue(int eraseBits) {
        // Read value
        if (in.readBit()) {
            // else -> same value as before
            if (in.readBit()) {
                // New leading and trailing zeros
                storedLeadingZeros = (int) in.getLong(5);
                storedTrailingZeros = Math.min(lastEraseBits, eraseBits);
            }
            long value = in.getLong(64 - storedLeadingZeros - storedTrailingZeros);
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
