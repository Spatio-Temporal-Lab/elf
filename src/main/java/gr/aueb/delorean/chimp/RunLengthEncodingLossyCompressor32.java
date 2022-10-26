package gr.aueb.delorean.chimp;

import fi.iki.yak.ts.compression.gorilla.BitOutput;

/**
 * Implements the time series compression as described in the Facebook's Gorilla Paper. Value compression
 * is for floating points only.
 *
 * @author Michael Burman
 */
public class RunLengthEncodingLossyCompressor32 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private int size;
    private int cases[];
    private float trailingDiff;
    private float leadingDiff;
    private int runSize;

    private BitOutput out;
    private int logOfError;

    public RunLengthEncodingLossyCompressor32(BitOutput output, int logOfError) {
        this.out = output;
        this.size = 0;
        this.logOfError = logOfError;
        int cases[] = {0, 0, 0};
        this.cases = cases;
        this.trailingDiff = 0;
        this.leadingDiff = 0;
        this.runSize = 0;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param timestamp Timestamp which is inside the allowed time block (default 24 hours with millisecond precision)
     * @param value next floating point value in the series
     */
    public void addValue(int value) {
        if(first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param timestamp Timestamp which is inside the allowed time block (default 24 hours with millisecond precision)
     * @param value next floating point value in the series
     */
    public void addValue(float value) {
        if(first) {
            writeFirst(Float.floatToRawIntBits(value));
        } else {
            compressValue(Float.floatToRawIntBits(value));
        }
    }

    private void writeFirst(int value) {
    	first = false;
        storedVal = value;
        out.writeBits(storedVal, 32);
        size += 32;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
    	if (runSize > 1600) {
    		out.skipBit();
    		out.writeBit();
    		out.writeBits(runSize, 4);
    		runSize = 0;
    	} else if (runSize > 0) {
    		for (int i = 0; i < runSize; i++) {
    			out.skipBit();
    			out.skipBit();
    		}
    		runSize = 0;
    	}
    	addValue(Float.NaN);
        out.skipBit();
        out.flush();
    }

    private void compressValue(int value) {
    	// if values is within error wrt the previous value, use the previous value
    	if (Math.abs(Float.intBitsToFloat(value) - Float.intBitsToFloat(storedVal)) < Math.pow(2, this.logOfError)) {
    		// Write 0
        	cases[0] += 1;
            //out.skipBit();
            runSize++;
            return;
    	}

    	if (runSize > 16) {
    		out.skipBit();
    		out.writeBit();
    		out.writeBits(runSize, 32);
    		size += 34;
    		runSize = 0;
    	} else if (runSize > 0) {
    		for (int i = 0; i < runSize; i++) {
    			out.skipBit();
    			out.skipBit();
    			size += 2;
    		}
    		runSize = 0;
    	}

        // TODO Fix already compiled into a big method
    	int integerDigits = (value << 1 >>> 24) - 127;
    	int space = 23 + this.logOfError - integerDigits;

    	if (space > 0) {
    		value = value >> space << space;
        	value = value | (storedVal & (2^space - 1));
    	}

    	int xor = storedVal ^ value;

        if(xor == 0) {
            // Write 0
        	cases[0] += 1;
            out.skipBit();
            size += 1;
        } else {
            int leadingZeros = Integer.numberOfLeadingZeros(xor);
            int trailingZeros = Integer.numberOfTrailingZeros(xor);

            // Check overflow of leading? Can't be 32!
            if(leadingZeros >= 16) {
                leadingZeros = 15;
            }
//            leadingZeros = leadingZeros % 2 == 0 ? leadingZeros : (leadingZeros - 1);

            // Store bit '1'
            out.writeBit();
            size += 1;

            if((leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros)
            		&& leadingZeros + trailingZeros < storedLeadingZeros + storedTrailingZeros + 10) {
            	cases[1] += 1;
            	this.trailingDiff += trailingZeros - storedTrailingZeros;
            	this.leadingDiff += leadingZeros - storedLeadingZeros;
                writeExistingLeading(xor);
            } else {
            	cases[2] += 2;
                writeNewLeading(xor, leadingZeros, trailingZeros);
            }
        }

        storedVal = value;
    }

    /**
     * If there at least as many leading zeros and as many trailing zeros as previous value, control bit = 0 (type a)
     * store the meaningful XORed value
     *
     * @param xor XOR between previous value and current
     */
    private void writeExistingLeading(int xor) {
        out.skipBit();
        int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
        out.writeBits(xor >>> storedTrailingZeros, significantBits);
        size += 1 + significantBits;
    }

    /**
     * store the length of the number of leading zeros in the next 5 bits
     * store length of the meaningful XORed value in the next 6 bits,
     * store the meaningful bits of the XORed value
     * (type b)
     *
     * @param xor XOR between previous value and current
     * @param leadingZeros New leading zeros
     * @param trailingZeros New trailing zeros
     */
    private void writeNewLeading(int xor, int leadingZeros, int trailingZeros) {
        out.writeBit();
        out.writeBits(leadingZeros, 4); // Number of leading zeros in the next 4 bits
//        out.writeBits(leadingZeros / 2, 3); // Number of leading zeros in the next 4 bits

        int significantBits = 32 - leadingZeros - trailingZeros;
        if (significantBits == 32) {
        	out.writeBits(0, 5); // Length of meaningful bits in the next 5 bits
        } else {
        	out.writeBits(significantBits, 5); // Length of meaningful bits in the next 5 bits
        }
        out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;
        size += 1 + 4 + 5 + significantBits;
    }

    public int getSize() {
    	return size;
    }

    public float getLeadingDiff() {
		return leadingDiff;
	}

    public float getTrailingDiff() {
		return trailingDiff;
	}

    public int[] getCases() {
		return cases;
	}
}
