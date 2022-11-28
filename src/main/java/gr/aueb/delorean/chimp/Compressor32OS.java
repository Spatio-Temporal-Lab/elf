package gr.aueb.delorean.chimp;

/**
 * Implements the time series compression as described in the Facebook's Gorilla Paper. Value compression
 * is for floating points only.
 *
 * @author Ruiyuan Li
 */
public class Compressor32OS {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private int size;
    private final static int END_SIGN = Float.floatToIntBits(Float.NaN);

    private final OutputBitStream out;

    public Compressor32OS() {
        out = new OutputBitStream(new byte[5000]);
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return out;
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
        out.writeLong(storedVal, 32);
        size += 32;
        return 32;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(int value) {
        int thisSize = 0;
        int xor = storedVal ^ value;

        if (xor == 0) {
            // Write 0
            out.writeBit(false);
            size += 1;
            thisSize += 1;
        } else {
            int leadingZeros = Integer.numberOfLeadingZeros(xor);
            int trailingZeros = Integer.numberOfTrailingZeros(xor);

            // Check overflow of leading? Can't be 32!
            if (leadingZeros >= 16) {
                leadingZeros = 15;
            }

            // Store bit '1'
            out.writeBit(true);
            size += 1;
            thisSize += 1;

            if (leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros) {
                thisSize += writeExistingLeading(xor);
            } else {
                thisSize += writeNewLeading(xor, leadingZeros, trailingZeros);
            }
        }

        storedVal = value;
        return thisSize;
    }

    /**
     * If there at least as many leading zeros and as many trailing zeros as previous value, control bit = 0 (type a)
     * store the meaningful XORed value
     *
     * @param xor XOR between previous value and current
     */
    private int writeExistingLeading(int xor) {
        out.writeBit(false);
        int significantBits = 32 - storedLeadingZeros - storedTrailingZeros;
        out.writeLong(xor >>> storedTrailingZeros, significantBits);
        size += 1 + significantBits;
        return 1 + significantBits;
    }

    /**
     * store the length of the number of leading zeros in the next 5 bits
     * store length of the meaningful XORed value in the next 6 bits,
     * store the meaningful bits of the XORed value
     * (type b)
     *
     * @param xor           XOR between previous value and current
     * @param leadingZeros  New leading zeros
     * @param trailingZeros New trailing zeros
     */
    private int writeNewLeading(int xor, int leadingZeros, int trailingZeros) {
        out.writeBit(true);
        out.writeInt(leadingZeros, 4); // Number of leading zeros in the next 5 bits

        int significantBits = 32 - leadingZeros - trailingZeros;
        if (significantBits == 32) {
            out.writeInt(0, 5); // Length of meaningful bits in the next 6 bits
        } else {
            out.writeInt(significantBits, 5); // Length of meaningful bits in the next 6 bits
        }

        out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;

        size += 1 + 4 + 5 + significantBits;
        return 1 + 4 + 5 + significantBits;
    }

    public int getSize() {
        return size;
    }
}
