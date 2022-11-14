package org.urbcomp.startdb.compress.elf;

import java.math.BigDecimal;

public abstract class AbstractElfCompressor {
    // αlog_2(10) look-up. f[0] is undefined
    private final static int[] f =
                    {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57, 60, 64,
                                    67, 70, 74, 77, 80, 84, 87, 90, 94, 97, 100, 103, 107, 110, 113,
                                    117, 120, 123, 127, 130, 133, 137, 140, 143, 147, 150, 153, 157,
                                    160, 163, 167, 170, 173};
    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    protected void addValue(String decimalFormat) {
        double v = Double.parseDouble(decimalFormat);
        long vLong = Double.doubleToLongBits(v);
        long vPrimeLong;

        if(v == 0 || Double.isInfinite(v)) {
            writeBit(false);
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            writeBit(false);
            vPrimeLong = 0xfff8000000000000L & vLong;
        } else {
            int[] alphaAndBetaStar = getAlphaAndBetaStar(decimalFormat);
            int e = getE(vLong);
            int gAlpha = getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long delta = (~(0xffffffffffffffffL << eraseBits)) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {
                writeBit(true);
                writeBits(betaStarToBytes(alphaAndBetaStar[1]), 4);
                vPrimeLong = (0xffffffffffffffffL << eraseBits) & vLong;
            } else {
                writeBit(false);
                vPrimeLong = vLong;
            }
        }
        xorCompress(vPrimeLong);
    }

    protected abstract void writeBits(byte[] bits, int len);

    protected abstract void writeBit(boolean bit);

    protected abstract void xorCompress(long vPrimeLong);

    private byte[] betaStarToBytes(int betaStar) {
        return new byte[] {(byte) (betaStar & 0x0f)};
    }

    private int getFAlpha(int alpha) {
        if (alpha <= 0) {
            throw new IllegalArgumentException("The argument alpha should be greater than 0.");
        }
        if (alpha < f.length - 1) {
            return f[alpha];
        } else {
            return (int) Math.ceil(alpha * LOG_2_10);
        }
    }

    private int[] getAlphaAndBetaStar(String decimalFormat) {
        int[] alphaAndBetaStar = new int[2];
        BigDecimal bigDecimal = new BigDecimal(decimalFormat);
        alphaAndBetaStar[0] = bigDecimal.scale();
        alphaAndBetaStar[1] = is10i(decimalFormat) ? 0 : bigDecimal.precision();
        return alphaAndBetaStar;
    }

    private boolean is10i(String decimalFormat) {
        int i = 0;
        for (char c : decimalFormat.toCharArray()) {
            if (c == '0' || c == '.' || c == '-' || c == '+') {
                i++;
            } else {
                break;
            }
        }
        return (i == decimalFormat.length() - 1 && decimalFormat.charAt(i) == '1') || (
                        i < decimalFormat.length() - 1 && decimalFormat.charAt(i) == '1' && (
                                        decimalFormat.charAt(i + 1) == 'e'
                                                        || decimalFormat.charAt(i + 1) == 'E'));
    }

    private int getE(long vLong) {
        long eMask = 0x7ff0000000000000L;
        long e = (eMask & vLong) >> 52;
        return (int) e;
    }
}
