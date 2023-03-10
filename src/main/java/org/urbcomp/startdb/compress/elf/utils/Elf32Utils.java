package org.urbcomp.startdb.compress.elf.utils;

public class Elf32Utils {
    // αlog_2(10) for look-up
    private final static int[] f =
                    new int[]{0, 4, 7, 10, 14, 17, 20, 24, 27, 30, 34, 37, 40, 44, 47, 50, 54, 57,
                                    60, 64, 67};

    private final static float[] map10iP =
                    new float[]{1.0f, 1.0E1f, 1.0E2f, 1.0E3f, 1.0E4f, 1.0E5f, 1.0E6f, 1.0E7f,
                                    1.0E8f, 1.0E9f, 1.0E10f, 1.0E11f, 1.0E12f, 1.0E13f, 1.0E14f,
                                    1.0E15f, 1.0E16f, 1.0E17f, 1.0E18f, 1.0E19f, 1.0E20f};

    private final static float[] map10iN =
                    new float[] {1.0f, 1.0E-1f, 1.0E-2f, 1.0E-3f, 1.0E-4f, 1.0E-5f, 1.0E-6f, 1.0E-7f,
                                    1.0E-8f, 1.0E-9f, 1.0E-10f, 1.0E-11f, 1.0E-12f, 1.0E-13f, 1.0E-14f,
                                    1.0E-15f, 1.0E-16f, 1.0E-17f, 1.0E-18f, 1.0E-19f, 1.0E-20f};

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

    public static int[] getAlphaAndBetaStar(float v, int lastBetaStar) {
        if (v < 0) {
            v = -v;
        }
        int[] alphaAndBetaStar = new int[2];
        float log10v = (float) Math.log10(v);
        int sp = (int) Math.floor(log10v);
        int beta = getSignificantCount(v, sp, lastBetaStar);
        alphaAndBetaStar[0] = beta - sp - 1;
        alphaAndBetaStar[1] = (v < 1 && sp == log10v) ? 0 : beta;
        return alphaAndBetaStar;
    }

    public static float roundUp(float v, int alpha) {
        float scale = get10iP(alpha);
        if (v < 0) {
            return (float) (Math.floor(v * scale) / scale);
        } else {
            return (float) (Math.ceil(v * scale) / scale);
        }
    }

    private static int getSignificantCount(float v, int sp, int lastBetaStar) {
        int i;
        if(lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
            i = Math.max(lastBetaStar - sp - 1, 1);
        } else if (sp >= 0) {
            i = 1;
        } else {
            i = -sp;
        }

        float temp = v * get10iP(i);
        int tempInt = (int) temp;
        while (tempInt != temp) {
            i++;
            temp = v * get10iP(i);
            tempInt = (int) temp;
        }

        // There are some bugs for those with high significand, e.g., 0.23911204406033099
        // So we should further check
        if (temp / get10iP(i) != v) {
            return 7;
        } else {
            while (i > 0 && tempInt % 10 == 0) {
                i--;
                tempInt = tempInt / 10;
            }
            return sp + i + 1;
        }
    }

    private static float get10iP(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (i >= map10iP.length) {
            return Float.parseFloat("1.0E" + i);
        } else {
            return map10iP[i];
        }
    }

    public static float get10iN(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (i >= map10iN.length) {
            return Float.parseFloat("1.0E-" + i);
        } else {
            return map10iN[i];
        }
    }
}
