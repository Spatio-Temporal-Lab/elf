package fi.iki.yak.ts.compression.gorilla;

public class CompressorForElf {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int lastEraseBits = 0;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private final static long END_SIGN = 0x0000000000000001L;

    private final BitOutput out;

    // We should have access to the series?
    public CompressorForElf(BitOutput output) {
        out = output;
        size = 0;
    }

    public BitOutput getOutputStream() {
        return out;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(long value, int eraseBits) {
        if (first) {
            return writeFirst(value, eraseBits);
        } else {
            return compressValue(value, eraseBits);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    public int addValue(double value, int eraseBits) {
        if (first) {
            return writeFirst(Double.doubleToRawLongBits(value), eraseBits);
        } else {
            return compressValue(Double.doubleToRawLongBits(value), eraseBits);
        }
    }

    private int writeFirst(long value, int eraseBits) {
        out.writeBits(value >> eraseBits, 64 - eraseBits);
        first = false;
        storedVal = value;
        size += 64 - eraseBits;
        lastEraseBits = eraseBits;
        return 64 - eraseBits;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN, 0);
        out.skipBit();
        out.flush();
    }

    private int compressValue(long value, int eraseBits) {
        int thisSize = 0;
        long xor = storedVal ^ value;

        if (xor == 0) {
            // Write 0
            out.skipBit();
            size += 1;
            thisSize += 1;
        } else {
            int leadingZeros = Long.numberOfLeadingZeros(xor);

            // Check overflow of leading? Can't be 32!
            if (leadingZeros >= 32) {
                leadingZeros = 31;
            }

            // Store bit '1'
            out.writeBit();
            size += 1;
            thisSize += 1;

            int trailingZeros = Math.min(lastEraseBits, eraseBits);

            if (leadingZeros >= storedLeadingZeros && trailingZeros >= storedTrailingZeros) {
                thisSize += writeExistingLeading(xor);
            } else {
                thisSize += writeNewLeading(xor, leadingZeros, trailingZeros);
            }
        }

        storedVal = value;
        lastEraseBits = eraseBits;
        return thisSize;
    }

    /**
     * If there at least as many leading zeros and as many trailing zeros as previous value, control bit = 0 (type a)
     * store the meaningful XORed value
     *
     * @param xor XOR between previous value and current
     */
    private int writeExistingLeading(long xor) {
        out.skipBit();
        int significantBits = 64 - storedLeadingZeros - storedTrailingZeros;
        out.writeBits(xor >>> storedTrailingZeros, significantBits);
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
    private int writeNewLeading(long xor, int leadingZeros, int trailingZeros) {
        out.writeBit();
        out.writeBits(leadingZeros, 5); // Number of leading zeros in the next 5 bits

        int significantBits = 64 - leadingZeros - trailingZeros;

        out.writeBits(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;

        size += 1 + 5 + significantBits;
        return 1 + 5 + significantBits;
    }

    public int getSize() {
        return size;
    }
}
