package org.urbcomp.startdb.compress.elf.utils;

public class Elf64Utils {
    // αlog_2(10) for look-up
    private final static int[] f =
        new int[] {0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57,
            60, 64, 67};

    private final static double[] map10iP =
        new double[] {1.0, 1.0E1, 1.0E2, 1.0E3, 1.0E4, 1.0E5, 1.0E6, 1.0E7,
            1.0E8, 1.0E9, 1.0E10, 1.0E11, 1.0E12, 1.0E13, 1.0E14,
            1.0E15, 1.0E16, 1.0E17, 1.0E18, 1.0E19, 1.0E20};

    private final static double[] map10iN =
        new double[] {1.0, 1.0E-1, 1.0E-2, 1.0E-3, 1.0E-4, 1.0E-5, 1.0E-6, 1.0E-7,
            1.0E-8, 1.0E-9, 1.0E-10, 1.0E-11, 1.0E-12, 1.0E-13, 1.0E-14,
            1.0E-15, 1.0E-16, 1.0E-17, 1.0E-18, 1.0E-19, 1.0E-20};

    private final static double LOG_2_10 = Math.log(10) / Math.log(2);

    public static int getFAlpha(int alpha) {
        if (alpha < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (alpha >= f.length) {
            return (int) Math.ceil(alpha * LOG_2_10);
        } else {
            return f[alpha];
        }
    }

    public static int[] getAlphaAndBetaStar(double v, int lastBetaStar) {
        if (v < 0) {
            v = -v;
        }
        int[] alphaAndBetaStar = new int[2];
        double log10v = Math.log10(v);
        int sp = (int) Math.floor(log10v);
        int beta = getSignificantCount(v, sp, lastBetaStar);
        alphaAndBetaStar[0] = beta - sp - 1;
        alphaAndBetaStar[1] = (v < 1 && sp == log10v) ? 0 : beta;
        return alphaAndBetaStar;
    }

    public static double roundUp(double v, int alpha) {
        double scale = get10iP(alpha);
        if (v < 0) {
            return Math.floor(v * scale) / scale;
        } else {
            return Math.ceil(v * scale) / scale;
        }
    }

    private static int getSignificantCount(double v, int sp, int lastBetaStar) {
        // when v itself is a number without any decimal part
        if((long) v == v){
            return sp + 1;
        }
        int i;
        if(sp >= 0) {
            if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
                i = Math.max(lastBetaStar - sp - 1, 1);
            } else {
                i = 1;
            }
        } else {
            if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
                i = lastBetaStar - sp - 1;
            } else {
                i = -sp;
            }
        }

        double temp = v * get10iP(i);
        long tempLong = (long) temp;
        while (tempLong != temp) {
            i++;
            temp = v * get10iP(i);
            tempLong = (long) temp;
        }

        // There are some bugs for those with high significand, i.e., 0.23911204406033099
        // So we should further check
        if (temp / get10iP(i) != v) {
            return 17;
        } else {
            while (tempLong % 10 == 0) {
                i--;
                tempLong = tempLong / 10;
            }
            return sp + i + 1;
        }
    }

    private static double get10iP(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (i >= map10iP.length) {
            return Double.parseDouble("1.0E" + i);
        } else {
            return map10iP[i];
        }
    }

    public static double get10iN(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (i >= map10iN.length) {
            return Double.parseDouble("1.0E-" + i);
        } else {
            return map10iN[i];
        }
    }
}
