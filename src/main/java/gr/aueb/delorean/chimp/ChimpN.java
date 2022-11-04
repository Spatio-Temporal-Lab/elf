package gr.aueb.delorean.chimp;

/**
 * Implements the Chimp128 time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class ChimpN {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private long storedValues[];
    private boolean first = true;
    private int size;
    private int perSize;
    private int previousValuesLog2;
    private int threshold;
    private int trailingZero;
    private int[] flag;

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

	private int setLsb;
	private int[] indices;
	private int index = 0;
	private int current = 0;
	private int flagOneSize;
	private int flagZeroSize;
    private int leadingZero;


    // We should have access to the series?
    public ChimpN(int previousValues) {
//        out = output;
        out = new OutputBitStream(new byte[1000 * 8]);
        size = 0;
        flag = new int[4];
        this.previousValues = previousValues;
        this.previousValuesLog2 = (int) (Math.log(previousValues) / Math.log(2));
        this.threshold = 6 + previousValuesLog2;
        this.setLsb = (int) Math.pow(2, threshold + 1) - 1;
        this.indices = new int[(int) Math.pow(2, threshold + 1)];
        this.storedValues = new long[previousValues];
        this.flagZeroSize = previousValuesLog2 + 2;
        this.flagOneSize = previousValuesLog2 + 11;
    }

    public ChimpN(int previousValues, OutputBitStream out, int size) {
//        out = output;
        this.out = out;
        this.size = size;
        flag = new int[4];
        this.previousValues = previousValues;
        this.previousValuesLog2 = (int) (Math.log(previousValues) / Math.log(2));
        this.threshold = 6 + previousValuesLog2;
        this.setLsb = (int) Math.pow(2, threshold + 1) - 1;
        this.indices = new int[(int) Math.pow(2, threshold + 1)];
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
        perSize = 0;
        if (first) {
            writeFirst(value);
        } else {
            compressValue(value);
        }
        size += perSize;
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public void addValue(double value) {
        perSize = 0;
        if (first) {
            writeFirst(Double.doubleToRawLongBits(value));
        } else {
            compressValue(Double.doubleToRawLongBits(value));
        }
        size += perSize;
    }

    private void writeFirst(long value) {
        first = false;
        storedValues[current] = value;
        out.writeLong(storedValues[current], 64);
        indices[(int) value & setLsb] = index;
        perSize += 64;
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

        int key = (int) value & setLsb;
        long xor;
        int previousIndex;
        int trailingZeros = 0;
        int currIndex = indices[key];
        if ((index - currIndex) < previousValues) {
            long tempXor = value ^ storedValues[currIndex % previousValues];
            trailingZeros = Long.numberOfTrailingZeros(tempXor);
            if (trailingZeros > threshold) {
                previousIndex = currIndex % previousValues;
                xor = tempXor;
            } else {
                previousIndex = index % previousValues;
                xor = storedValues[previousIndex] ^ value;
            }
        } else {
            previousIndex = index % previousValues;
            xor = storedValues[previousIndex] ^ value;
        }

        if (xor == 0) {

            out.writeInt(previousIndex, this.flagZeroSize);
            perSize += this.flagZeroSize;
            storedLeadingZeros = 65;
            leadingZero = 64;
            trailingZero = 64;
        } else {
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            leadingZero = Long.numberOfLeadingZeros(xor);
            trailingZero = trailingZeros;
            if (trailingZeros > threshold) {
                int significantBits = 64 - leadingZeros - trailingZeros;
                out.writeInt(512 * (previousValues + previousIndex) + 64 * leadingRepresentation[leadingZeros] + significantBits, this.flagOneSize);
                out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                perSize += significantBits + this.flagOneSize;
                storedLeadingZeros = 65;
            } else if (leadingZeros == storedLeadingZeros) {
                out.writeInt(2, 2);
                int significantBits = 64 - leadingZeros;
                out.writeLong(xor, significantBits);
                perSize += 2 + significantBits;
            } else {
                storedLeadingZeros = leadingZeros;
                int significantBits = 64 - leadingZeros;
                out.writeInt(24 + leadingRepresentation[leadingZeros], 5);
                out.writeLong(xor, significantBits);
                perSize += 5 + significantBits;
            }
        }
        current = (current + 1) % previousValues;
        storedValues[current] = value;
        index++;
        indices[key] = index;

    }

    public int getTrailingZero() {
        return trailingZero;
    }

    public int getPerSize() {
        return perSize;
    }
    public int getLeadingZero() {
        return leadingZero;
    }

    public int getSize() {
        return size;
    }
}
