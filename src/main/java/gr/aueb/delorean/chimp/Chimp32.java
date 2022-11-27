package gr.aueb.delorean.chimp;

/**
 * Implements the Chimp time series compression. Value compression
 * is for floating points only.
 *
 * @author Panagiotis Liakos
 */
public class Chimp32 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedVal = 0;
    private boolean first = true;
    private int size;

    public final static int THRESHOLD = 5;

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
    public Chimp32() {
        out = new OutputBitStream(new byte[1000 * 5]);
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
    public int addValue(int value) {
        if (first) {
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
    public int addValue(float value) {
        if (first) {
            return writeFirst(Float.floatToRawIntBits(value));
        } else {
            return compressValue(Float.floatToRawIntBits(value));
        }
    }

    private int writeFirst(int value) {
        first = false;
        storedVal = value;
        out.writeInt(storedVal, 32);
        size += 32;
        return 32;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(Float.NaN);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(int value) {
        int thisSize = 0;
        int xor = storedVal ^ value;
        if (xor == 0) {
            // Write 0
            out.writeBit(false);
            out.writeBit(false);
            size += 2;
            thisSize += 2;
            storedLeadingZeros = 33;
        } else {
            int leadingZeros = leadingRound[Integer.numberOfLeadingZeros(xor)];
            int trailingZeros = Integer.numberOfTrailingZeros(xor);

            if (trailingZeros > THRESHOLD) {
                int significantBits = 32 - leadingZeros - trailingZeros;
                out.writeBit(false);
                out.writeBit(true);
                out.writeInt(leadingRepresentation[leadingZeros], 3);
                out.writeInt(significantBits, 5);
//                out.writeInt(16 * (8 + leadingRepresentation[leadingZeros]) + significantBits, 10);
                out.writeInt(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR
                size += 10 + significantBits;
                thisSize += 10 + significantBits;
                storedLeadingZeros = 33;
            } else if (leadingZeros == storedLeadingZeros) {
                out.writeBit(true);
                out.writeBit(false);
                int significantBits = 32 - leadingZeros;
                out.writeInt(xor, significantBits);
                size += 2 + significantBits;
                thisSize += 2 + significantBits;
            } else {
                storedLeadingZeros = leadingZeros;
                int significantBits = 32 - leadingZeros;
                out.writeInt(24 + leadingRepresentation[leadingZeros], 5);
                out.writeInt(xor, significantBits);
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
