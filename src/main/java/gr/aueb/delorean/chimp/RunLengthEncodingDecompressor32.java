package gr.aueb.delorean.chimp;

import fi.iki.yak.ts.compression.gorilla.BitInput;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class RunLengthEncodingDecompressor32 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;
    private int runSize = 0;

    private BitInput in;

    private final static int NAN_INT = 0x7fc00000;


    public RunLengthEncodingDecompressor32(BitInput input) {
        in = input;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readValue() {
        next();
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() {
        if (first) {
        	first = false;
            storedVal = (int) in.getLong(32);
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            	return;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() {
    	if (runSize > 0) {
    		runSize--;
    		return;
    	}
        // Read value
        if (in.readBit()) {
            // else -> same value as before
            if (in.readBit()) {
                // New leading and trailing zeros
            	storedLeadingZeros = (int) in.getLong(4);
//                storedLeadingZeros = (int) in.getLong(3) * 2;

                byte significantBits = (byte) in.getLong(5);
                if(significantBits == 0) {
                    significantBits = 32;
                }
                storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
            }
            int value = (int) in.getLong(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            }

        } else {
        	if (in.readBit()) {
        		runSize = (int) in.getLong(32);
        		runSize--;
        	}
        }
    }

}