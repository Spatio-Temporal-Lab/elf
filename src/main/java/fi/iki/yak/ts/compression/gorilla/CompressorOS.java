package fi.iki.yak.ts.compression.gorilla;

import gr.aueb.delorean.chimp.OutputBitStream;

public class CompressorOS {
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private long storedVal = 0;
    private boolean first = true;
    private int size;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    private final OutputBitStream out;

    public CompressorOS() {
        out = new OutputBitStream(new byte[10000]);
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
    public int addValue(long value) {
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
    public int addValue(double value) {
        if (first) {
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

        if (xor == 0) {
            // Write 0
            out.writeBit(false);
            size += 1;
            thisSize += 1;
        } else {
            int leadingZeros = Long.numberOfLeadingZeros(xor);
            int trailingZeros = Long.numberOfTrailingZeros(xor);

            // Check overflow of leading? Can't be 32!
            if (leadingZeros >= 32) {
                leadingZeros = 31;
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
    private int writeExistingLeading(long xor) {
        out.writeBit(false);
        int significantBits = 64 - storedLeadingZeros - storedTrailingZeros;
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
    private int writeNewLeading(long xor, int leadingZeros, int trailingZeros) {
        out.writeBit(true);
        out.writeInt(leadingZeros, 5); // Number of leading zeros in the next 5 bits

        int significantBits = 64 - leadingZeros - trailingZeros;
        if (significantBits == 64) {
            out.writeInt(0, 6); // Length of meaningful bits in the next 6 bits
        } else {
            out.writeInt(significantBits, 6); // Length of meaningful bits in the next 6 bits
        }

        out.writeLong(xor >>> trailingZeros, significantBits); // Store the meaningful bits of XOR

        storedLeadingZeros = leadingZeros;
        storedTrailingZeros = trailingZeros;

        size += 1 + 5 + 6 + significantBits;
        return 1 + 5 + 6 + significantBits;
    }

    public int getSize() {
        return size;
    }
}
