package gr.aueb.delorean.chimp;

/**
 * Implements the Chimp128 time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class ChimpNNoIndex {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private long storedValues[];
    private boolean first = true;
    private int size;
    private int previousValuesLog2;
    private int threshold;

    public final static short[] leadingRepresentation = {0, 0, 0, 0, 0, 0, 0, 0,
			1, 1, 1, 1, 2, 2, 2, 2,
			3, 3, 4, 4, 5, 5, 6, 6,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7
		};

    public final static short[] leadingRound = {0, 0, 0, 0, 0, 0, 0, 0,
			8, 8, 8, 8, 12, 12, 12, 12,
			16, 16, 18, 18, 20, 20, 22, 22,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24,
			24, 24, 24, 24, 24, 24, 24, 24
		};
//    public final static short FIRST_DELTA_BITS = 27;

//    private BitOutput out;
    private OutputBitStream out;
	private int previousValues;

	private int index = 0;
	private int current = 0;
	private int flagOneSize;
	private int flagZeroSize;

    // We should have access to the series?
    public ChimpNNoIndex(int previousValues) {
//        out = output;
    	out = new OutputBitStream(new byte[1000*8]);
        size = 0;
        this.previousValues = previousValues;
        this.previousValuesLog2 =  (int)(Math.log(previousValues) / Math.log(2));
        this.threshold = 6 + previousValuesLog2;
        this.storedValues = new long[previousValues];
        this.flagZeroSize = previousValuesLog2 + 2;
        this.flagOneSize = previousValuesLog2 + 11;
    }
    
    public byte[] getOut() {
		return out.buffer;
	}

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(long value) {
        if(first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(double value) {
        if(first) {
            writeFirst(Double.doubleToRawLongBits(value));
        } else {
            compressValue(Double.doubleToRawLongBits(value));
        }
    }

    private void writeFirst(long value) {
    	first = false;
        storedValues[current] = value;
        out.writeLong(storedValues[current], 64);
        size += 64;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
    	addValue(Double.NaN);
    	out.writeBit(false);
        out.flush();
    }

    private void compressValue(long value) {
    	long xor;
    	int previousIndex;
    	int trailingZeros = 0;
    	previousIndex =  index % previousValues;
		xor = storedValues[previousIndex] ^ value;
		int maxTrailingZeros = 0;
		for (int i=0; i<previousValues; i++) {
			long tempXor = value ^ storedValues[i];
			int tempTrailingZeros = Long.numberOfTrailingZeros(tempXor);
			if (tempTrailingZeros > threshold && tempTrailingZeros > maxTrailingZeros) {
				maxTrailingZeros = tempTrailingZeros;
    			xor = tempXor;
    			trailingZeros = maxTrailingZeros;
    			previousIndex = i;

			}
		}

        if(xor == 0) {
            out.writeInt(previousIndex, this.flagZeroSize);
            size += this.flagZeroSize;
            storedLeadingZeros = 65;
        } else {
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];

            if (trailingZeros > threshold) {
                int significantBits = 64 - leadingZeros - trailingZeros;
                out.writeInt(512 * (previousValues + previousIndex) + 64 * leadingRepresentation[leadingZeros] + significantBits, this.flagOneSize);
                out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                size += significantBits + this.flagOneSize;
    			storedLeadingZeros = 65;
    		} else if (leadingZeros == storedLeadingZeros) {
    			out.writeInt(2, 2);
    			int significantBits = 64 - leadingZeros;
    			out.writeLong(xor, significantBits);
    			size += 2 + significantBits;
    		} else {
    			storedLeadingZeros = leadingZeros;
    			int significantBits = 64 - leadingZeros;
    			out.writeInt(24 + leadingRepresentation[leadingZeros], 5);
    			out.writeLong(xor, significantBits);
    			size += 5 + significantBits;
    		}
    	}
        current = (current + 1) % previousValues;
        storedValues[current] = value;
		index++;

    }

    public int getSize() {
    	return size;
    }
}
