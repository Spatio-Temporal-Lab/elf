package org.urbcomp.startdb.compress.elf.compressor;

import java.math.BigDecimal;

public abstract class AbstractElfCompressor implements ICompressor {
    // αlog_2(10) for look-up. We will calculate for the first time.
    private final static int[] f = new int[325];
    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    static {
        for (int i = 0; i < f.length; i++) {
            f[i] = (int) Math.ceil(i * LOG_2_10);
        }
    }

    private int size = 0;

    public void addValue(double v) {
        String vString = Double.toString(v);
        long vLong = Double.doubleToLongBits(v);
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v)) {
            size += writeBit(false);
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            size += writeBit(false);
            vPrimeLong = 0xfff8000000000000L & vLong;
        } else {
            int[] alphaAndBetaStar = getAlphaAndBetaStar(vString);
            int e = getE(vLong);
            int gAlpha = getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long delta = (~(0xffffffffffffffffL << eraseBits)) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {
                size += writeBit(true);
                size += writeInt(alphaAndBetaStar[1], 4);
                vPrimeLong = (0xffffffffffffffffL << eraseBits) & vLong;
            } else {
                size += writeBit(false);
                vPrimeLong = vLong;
            }
        }
        size += xorCompress(vPrimeLong);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong);

    private static int getFAlpha(int alpha) {
        if (alpha <= 0 || alpha >= f.length) {
            throw new IllegalArgumentException(
                            "The argument should be in [1, " + (f.length - 1) + "]");
        }
        return f[alpha];
    }

    private static int[] getAlphaAndBetaStar(String decimalFormat) {
        int[] alphaAndBetaStar = new int[2];
        BigDecimal bigDecimal = new BigDecimal(decimalFormat);
        alphaAndBetaStar[0] = bigDecimal.scale();
        alphaAndBetaStar[1] = is10i(decimalFormat) ? 0 : bigDecimal.precision();
        return alphaAndBetaStar;
    }

    private static boolean is10i(String decimalFormat) {
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

    private static int getE(long vLong) {
        long eMask = 0x7ff0000000000000L;
        long e = (eMask & vLong) >> 52;
        return (int) e;
    }
}
