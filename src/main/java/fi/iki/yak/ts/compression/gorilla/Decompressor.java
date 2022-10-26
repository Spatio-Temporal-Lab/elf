package fi.iki.yak.ts.compression.gorilla;

import java.util.LinkedList;
import java.util.List;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class Decompressor {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private BitInput in;

    private final static long NAN_LONG = 0x7ff8000000000000L;

    public Decompressor(BitInput input) {
        in = input;
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
        next();
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() {
        if (first) {
        	first = false;
            storedVal = in.getLong(64);
            if (storedVal == NAN_LONG) {
            	endOfStream = true;
            	return;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() {
        // Read value
        if (in.readBit()) {
            // else -> same value as before
            if (in.readBit()) {
                // New leading and trailing zeros
                storedLeadingZeros = (int) in.getLong(5);

                byte significantBits = (byte) in.getLong(6);
                if(significantBits == 0) {
                    significantBits = 64;
                }
                storedTrailingZeros = 64 - significantBits - storedLeadingZeros;
            }
            long value = in.getLong(64 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_LONG) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            }

        }
    }

}