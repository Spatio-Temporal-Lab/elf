package org.urbcomp.startdb.compress.elf.xordecompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.io.IOException;

public class ElfPlusXORDecompressor {
    private long storedVal = 0;
    private int storedApproximateTrailingZeros = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private final InputBitStream in;

    private final static long END_SIGN = Double.doubleToLongBits(Double.NaN);

    private final static short[] leadingRepresentationSE = {0, 2, 8, 10};

    private final static short[] leadingRepresentationM = {0, 2, 4, 6, 8, 10, 12, 14};

    public ElfPlusXORDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public InputBitStream getInputStream() {
        return in;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Double readValue(int betaStar) {
        try {
            next(betaStar);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next(int betaStar) throws IOException {
        if (first) {
            first = false;
            boolean isSpLower = in.readInt(1) == 1;
            int se = in.readInt(12);
            int e = se & 0x7ff;
            storedApproximateTrailingZeros = getApproximateTrailingZeros(e, betaStar, isSpLower);
            storedVal = (in.readLong(52 - storedApproximateTrailingZeros) << storedApproximateTrailingZeros) | (((long)se) << 52);
            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue(betaStar);
        }
    }

    private void nextValue(int betaStar) throws IOException {
        if (in.readInt(1) == 1) {
            // read se
            int storedSe = (int) (storedVal >>> 52);
            int thisSe;
            if (in.readInt(1) == 0) {
                thisSe = storedSe;
            } else {
                int leadingZerosCountSe = leadingRepresentationSE[in.readInt(2)];
                int nonLeadingBits = in.readInt(12 - leadingZerosCountSe);
                thisSe = storedSe ^ nonLeadingBits;
            }

            boolean isSpLower = in.readInt(1) == 1;
            int thisE = thisSe & 0x7ff;
            int thisApproximateTrailingZeros = getApproximateTrailingZeros(thisE, betaStar, isSpLower);
            int approximateTrailingZeros = Math.min(thisApproximateTrailingZeros, storedApproximateTrailingZeros);

            // read M
            long storedM = storedVal & 0xfffffffffffffL;
            long thisM;
            if (in.readInt(1) == 0) {
                thisM = storedM;
            } else {
                int leadingZerosCountM = leadingRepresentationM[in.readInt(3)];
                long centerBits = in.readLong(52 - leadingZerosCountM - approximateTrailingZeros);
                thisM = storedM ^ (centerBits << approximateTrailingZeros);
            }

            storedVal = (((long) thisSe) << 52) | thisM;
            storedApproximateTrailingZeros = thisApproximateTrailingZeros;

            if (storedVal == END_SIGN) {
                endOfStream = true;
            }
        }
    }

    private int getApproximateTrailingZeros(int e, int betaStar, boolean isSpLower) {
        if (betaStar == 0) {
            return 52;
        }
        if (betaStar == Integer.MAX_VALUE) {
            return 0;
        }
        int sp;
        if (isSpLower) {
            sp = (int) Math.floor((e - 1023) * Math.log10(2));
        } else {
            sp = (int) Math.floor((e - 1022) * Math.log10(2));
        }
        int alpha = betaStar - sp - 1;
        int gAlpha = ElfUtils.getFAlpha(alpha) + e - 1023;
        return 52 - gAlpha;
    }
}
