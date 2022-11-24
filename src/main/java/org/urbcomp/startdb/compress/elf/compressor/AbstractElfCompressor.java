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
        char[] chars = Double.toString(v).toCharArray();        //这里最慢
        alphaAndBetaStar[0] = getPrecision(chars);
        alphaAndBetaStar[1] = (v < 1 && log10v % 1 == 0) ?
                        0 :
                        alphaAndBetaStar[0] + (int) Math.floor(log10v) + 1;
        return alphaAndBetaStar;
    }

    private static int getPrecision(char[] chars) {
        int pre = 0;
        int i = 0;
        // find the point
        while (i < chars.length) {
            if (chars[i] == '.') {
                i++;
                break;
            } else {
                i++;
            }
        }

        while (i < chars.length) {
            if (chars[i] != 'E') {
                pre++;
                i++;
            } else {
                i++;
                break;
            }
        }

        if (i < chars.length) {
            boolean negative = false;
            if (chars[i] == '-') {
                negative = true;
                i++;
            }
            int e = 0;
            while (i < chars.length) {
                e = e * 10 + (chars[i] - '0');
                i++;
            }
            if (negative) {
                pre = pre + e;
            } else {
                pre = pre - e;
            }
        }
        return Math.max(pre, 1);
    }

    private static int getE(long vLong) {
        return ((int) (vLong >> 52)) & 0x7ff;
    }
}
