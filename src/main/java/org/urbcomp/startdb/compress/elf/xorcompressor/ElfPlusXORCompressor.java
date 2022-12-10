package org.urbcomp.startdb.compress.elf.xorcompressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

public class ElfPlusXORCompressor {
    private long storedVal = 0;
    private int storedApproximateTrailingZeros = 0;
    private boolean first = true;
    private int size;
    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    public final static short[] leadingRepresentationSE = {
                    0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 3, 3, 3
    };

    public final static short[] leadingRoundSE = {
                    0, 0, 2, 2, 2, 2, 2, 2, 8, 8, 10, 10, 10
    };

    public final static short[] leadingRepresentationM = {
                    0, 0, 1, 1, 2, 2, 3, 3,
                    4, 4, 5, 5, 6, 6, 7, 7,
                    7, 7, 7, 7, 7, 7, 7, 7,
                    7, 7, 7, 7, 7, 7, 7, 7,
                    7, 7, 7, 7, 7, 7, 7, 7,
                    7, 7, 7, 7, 7, 7, 7, 7,
                    7, 7, 7, 7
    };

    public final static short[] leadingRoundM = {
                    0, 0, 2, 2, 4, 4, 6, 6,
                    8, 8, 10, 10, 12, 12, 14, 14,
                    14, 14, 14, 14, 14, 14, 14, 14,
                    14, 14, 14, 14, 14, 14, 14, 14,
                    14, 14, 14, 14, 14, 14, 14, 14,
                    14, 14, 14, 14, 14, 14, 14, 14,
                    14, 14, 14, 14
    };

    private final OutputBitStream out;

    public ElfPlusXORCompressor() {
        out = new OutputBitStream(
                        new byte[12000]);  // for elf, we need one more bit for each at the worst case
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     * @param betaStar betaStar in the erasing step
     */
    public int addValue(long value, int betaStar) {
        if (first) {
            return writeFirst(value, betaStar);
        } else {
            return compressValue(value, betaStar);
        }
    }

    /**
     * Adds a new double value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     * @param betaStar betaStar in the erasing step
     */
    public int addValue(double value, int betaStar) {
        if (first) {
            return writeFirst(Double.doubleToRawLongBits(value), betaStar);
        } else {
            return compressValue(Double.doubleToRawLongBits(value), betaStar);
        }
    }

    private int writeFirst(long value, int betaStar) {
        first = false;
        storedVal = value;
        storedApproximateTrailingZeros = getApproximateTrailingZeros(value, betaStar);
        out.writeLong(storedVal >>> storedApproximateTrailingZeros, 64 - storedApproximateTrailingZeros);

        size += 64 - storedApproximateTrailingZeros;
        return 64 - storedApproximateTrailingZeros;
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        addValue(END_SIGN, Integer.MAX_VALUE);
        out.writeBit(false);
        out.flush();
    }

    private int compressValue(long value, int betaStar) {
        int thisSize = 0;
        long xor = storedVal ^ value;

        if (xor == 0) {
            // case 0
            out.writeInt(0, 0);

            size += 1;
            thisSize += 1;
        } else {
            int thisApproximateTrailingZeros = getApproximateTrailingZeros(value, betaStar);
            int approximateTrailingZeros = Math.min(thisApproximateTrailingZeros, storedApproximateTrailingZeros);

            long se = xor >>> 52;
            long mantissa = xor & 0xfffffffffffffL;

            out.writeInt(1, 1);
            size += 1;
            thisSize += 1;

            // store se
            if (se == 0) {
                // case 10
                out.writeInt(0, 1);

                size += 1;
                thisSize += 1;
            } else {
                // case 11
                out.writeInt(1, 1);
                int leadingZerosSE = Long.numberOfLeadingZeros(se) - 52;
                out.writeInt(leadingRepresentationSE[leadingRoundSE[leadingZerosSE]], 2);
                out.writeLong(se, 12 - leadingZerosSE);

                size += 15 - leadingZerosSE;
                thisSize += 15 - leadingZerosSE;
            }

            // store mantissa
            if (mantissa == 0) {
                // case 10
                out.writeInt(0, 1);

                size += 1;
                thisSize += 1;
            } else {
                // case 11
                out.writeInt(1, 1);
                int leadingZerosM = Long.numberOfLeadingZeros(mantissa) - 12;
                int leadingZerosRoundM = leadingRepresentationM[leadingRoundM[leadingZerosM]];
                out.writeInt(leadingZerosRoundM, 3);
                out.writeLong(mantissa >>> approximateTrailingZeros, 52 - leadingZerosRoundM - approximateTrailingZeros);

                size += 1 + 3 + 52 - leadingZerosRoundM - approximateTrailingZeros;
                thisSize += 1 + 3 + 52 - leadingZerosRoundM - approximateTrailingZeros;
            }

            storedVal = value;
            storedApproximateTrailingZeros = thisApproximateTrailingZeros;
        }

        return thisSize;
    }

    public int getSize() {
        return size;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }


    private int getApproximateTrailingZeros(long vPrimeLong, int betaStar) {
        if (betaStar == 0) {
            return 52;  // 10-i, we erase all mantissa bits.
        }
        double vPrime = Double.longBitsToDouble(vPrimeLong);
        int sp = (int) Math.floor(Math.log10(Math.abs(vPrime)));
        int alpha = betaStar - sp - 1;
        int e = ((int) (vPrimeLong >> 52)) & 0x7ff;
        int gAlpha = ElfUtils.getFAlpha(alpha) + e - 1023;
        return 52 - gAlpha;
    }
}
