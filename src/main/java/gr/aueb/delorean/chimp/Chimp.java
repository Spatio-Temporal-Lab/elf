package gr.aueb.delorean.chimp;

/**
 * Implements the Chimp time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class Chimp {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private long storedVal = 0;
    private boolean first = true;
    private int perSize;
    private int size;
    private int trailingZero;
    public final static int THRESHOLD = 6;
    private int leadingZero;

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

    private OutputBitStream out;

    // We should have access to the series?
    public Chimp() {
        out = new OutputBitStream(new byte[1000 * 8]);
        size = 0;
        flag = new int[4];
    }

    public Chimp(OutputBitStream out) {
        this.out = out;
        this.size = 0;
        flag = new int[4];
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
        storedVal = value;
        out.writeLong(storedVal, 64);
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
        long xor = storedVal ^ value;
        if (xor == 0) {
            flag[0]++;
            // Write 0
            out.writeBit(false);
            out.writeBit(false);
            perSize += 2;
            storedLeadingZeros = 65;

            leadingZero = 64;
            trailingZero = 64;
        } else {
            leadingZero = Long.numberOfLeadingZeros(xor);
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            int trailingZeros = Long.numberOfTrailingZeros(xor);
            trailingZero = trailingZeros;
            if (trailingZeros > THRESHOLD) {
                int significantBits = 64 - leadingZeros - trailingZeros;
                flag[1]++;
                out.writeBit(false);
                out.writeBit(true);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeInt(significantBits, 6);
                out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                perSize += 11 + significantBits;
                storedLeadingZeros = 65;
            } else if (leadingZeros == storedLeadingZeros) {
                flag[2]++;
                out.writeBit(true);
                out.writeBit(false);
                int significantBits = 64 - leadingZeros;
                out.writeLong(xor, significantBits);
                perSize += 2 + significantBits;
            } else {
                flag[3]++;
                storedLeadingZeros = leadingZeros;
                int significantBits = 64 - leadingZeros;
                out.writeBit(true);
                out.writeBit(true);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeLong(xor, significantBits);
                perSize += 5 + significantBits;
            }
        }
        storedVal = value;
    }

    public int getPerSize() {
        return perSize;
    }

    public int getTrailingZero() {
        return trailingZero;
    }

    public byte[] getOut() {
        return out.buffer;
    }

    public int getSize() {
        return size;
    }

    public int getLeadingZero() {
        return leadingZero;
    }

    public int[] getFlag() {
        return flag;
    }


}
