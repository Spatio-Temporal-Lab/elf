package org.urbcomp.startdb.compress.elf.compressor;

public abstract class AbstractElfCompressor implements ICompressor {
    // αlog_2(10) for look-up
    private final static int[] f =
                    new int[] {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57,
                                    60, 64, 67, 70, 74, 77, 80, 84, 87, 90, 94, 97};
    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    private int size = 0;

    public void addValue(double v) {
        String vString = Double.toString(v);
        long vLong = Double.doubleToRawLongBits(v);
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
        if (alpha <= 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (alpha >= f.length) {
            return (int) Math.ceil(alpha * LOG_2_10);
        } else {
            return f[alpha];
        }
    }

    // Note decimalFormat is got by Double.toString()
    private static int[] getAlphaAndBetaStar(String decimalFormat) {
        int[] alphaAndBetaStar = new int[2];
        char[] chars = decimalFormat.toCharArray();
        alphaAndBetaStar[0] = getPrecision(chars);
        alphaAndBetaStar[1] = is10i(chars) ? 0 : getSignificand(chars);
        return alphaAndBetaStar;
    }

    private static int getSignificand(char[] chars) {
        int sig = 0;
        int i = 0;
        // omit the first sign, the prefix 0 and .
        while (i < chars.length) {
            if (chars[i] == '0' || chars[i] == '.' || chars[i] == '-') {
                i++;
            } else {
                break;
            }
        }
        while (i < chars.length) {
            if (chars[i] == '.') {
                i++;
            } else if (chars[i] != 'E') {
                sig++;
                i++;
            } else {
                break;
            }
        }
        return sig;
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

    // Note decimalFormat is got by Double.toString(), and i < 0
    private static boolean is10i(char[] chars) {
        int i = 0;
        for (char c : chars) {
            if (c == '0' || c == '.' || c == '-') {
                i++;
            } else {
                break;
            }
        }
        // the former is "0.0...01", the later is "1.0E-x"
        return (i == chars.length - 1 && chars[i] == '1') || (i < chars.length - 4
                        && chars[i] == '1' && chars[i + 1] == '.' && chars[i + 2] == '0'
                        && chars[i + 3] == 'E' && chars[i + 4] == '-');
    }

    private static int getE(long vLong) {
        long eMask = 0x7ff0000000000000L;
        long e = (eMask & vLong) >> 52;
        return (int) e;
    }
}
