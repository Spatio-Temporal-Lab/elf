package org.urbcomp.startdb.compress.elf.compressor;

public abstract class AbstractElfCompressor implements ICompressor {
    // αlog_2(10) for look-up
    private final static int[] f =
                    new int[] {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57,
                                    60, 64, 67};
    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    private int size = 0;

    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v)) {
            size += writeBit(false);
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            size += writeBit(false);
            vPrimeLong = 0xfff8000000000000L & vLong;
        } else {
            int[] alphaAndBetaStar = getAlphaAndBetaStar(v);
            int e = getE(vLong);
            int gAlpha = getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {
                size += writeInt(alphaAndBetaStar[1] | 0x10, 5);
                vPrimeLong = mask & vLong;
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
        if (alpha <= 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (alpha >= f.length) {
            return (int) Math.ceil(alpha * LOG_2_10);
        } else {
            return f[alpha];
        }
    }

    private static int[] getAlphaAndBetaStar(double v) {
        if (v < 0) {
            v = -v;
        }
        int[] alphaAndBetaStar = new int[2];
        double log10v = Math.log10(v);
        int beta = getSignificantCount(v);
        alphaAndBetaStar[0] = beta - (int) Math.floor(log10v) - 1;
        alphaAndBetaStar[1] = (v < 1 && log10v % 1 == 0) ? 0 : beta;
        return alphaAndBetaStar;
    }

    private static int getSignificantCount(double v) {
        String vString = Double.toString(v);
        int len = vString.length();
        int i = 0;
        // omit the former 0 and point
        while (i < len) {
            if (vString.charAt(i) != '0' && vString.charAt(i) != '.') {
                break;
            }
            i++;
        }
        int sig = 0;
        while (i < len) {
            if (vString.charAt(i) == '.') {
                i++;
            } else if(vString.charAt(i) != 'E') {
                sig++;
                i++;
            } else {
                break;
            }
        }
        return sig;
    }

    private static int getE(long vLong) {
        return ((int) (vLong >> 52)) & 0x7ff;
    }
}
