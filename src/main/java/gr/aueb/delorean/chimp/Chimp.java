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
    private int size;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);
    public final static int THRESHOLD = 6;

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

    private final OutputBitStream out;

    // We should have access to the series?
    public Chimp() {
        out = new OutputBitStream(new byte[10000]);  // for elf, we need one more bit for each at the worst case
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(long value) {
        if(first) {
            return writeFirst(value);
        } else {
            return compressValue(value);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(double value) {
        if(first) {
            return writeFirst(Double.doubleToRawLongBits(value));
        } else {
            return compressValue(Double.doubleToRawLongBits(value));
        }
    }

    private int writeFirst(long value) {
        first = false;
        storedVal = value;
        out.writeLong(storedVal, 64);
        size += 64;
        return 64;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(long value) {
        int thisSize = 0;
        long xor = storedVal ^ value;
        if(xor == 0) {
            // Write 0
            out.writeBit(false);
            out.writeBit(false);
            size += 2;
            thisSize += 2;
            storedLeadingZeros = 65;
        } else {
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            int trailingZeros = Long.numberOfTrailingZeros(xor);

            if (trailingZeros > THRESHOLD) {
                int significantBits = 64 - leadingZeros - trailingZeros;
                out.writeBit(false);
                out.writeBit(true);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeInt(significantBits, 6);
                out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                size += 11 + significantBits;
                thisSize += 11 + significantBits;
                storedLeadingZeros = 65;
            } else if (leadingZeros == storedLeadingZeros) {
                out.writeBit(true);
                out.writeBit(false);
                int significantBits = 64 - leadingZeros;
                out.writeLong(xor, significantBits);
                size += 2 + significantBits;
                thisSize += 2 + significantBits;
            } else {
                storedLeadingZeros = leadingZeros;
                int significantBits = 64 - leadingZeros;
                out.writeBit(true);
                out.writeBit(true);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeLong(xor, significantBits);
                size += 5 + significantBits;
                thisSize += 5 + significantBits;
            }
        }
        storedVal = value;
        return thisSize;
    }

    public int getSize() {
        return size;
    }

    public byte[] getOut() {
        return out.buffer;
    }
}
