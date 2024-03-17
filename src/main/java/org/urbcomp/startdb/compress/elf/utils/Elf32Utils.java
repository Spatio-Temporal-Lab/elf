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

    private final static int[] mapSPGreater1 =
                    new int[] {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000};

    private final static float[] mapSPLess1 =
                    new float[] {1, 0.1f, 0.01f, 0.001f, 0.0001f, 0.00001f, 0.000001f, 0.0000001f, 0.00000001f,
                                    0.000000001f, 0.0000000001f};

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
        int[] spAnd10iNFlag = getSPAnd10iNFlag(v);
        int beta = getSignificantCount(v, spAnd10iNFlag[0], lastBetaStar);
        alphaAndBetaStar[0] = beta - spAnd10iNFlag[0] - 1;
        alphaAndBetaStar[1] = spAnd10iNFlag[1] == 1 ? 0 : beta;
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
        if (lastBetaStar != Integer.MAX_VALUE && lastBetaStar != 0) {
            i = Math.max(lastBetaStar - sp - 1, 1);
        } else if (lastBetaStar == Integer.MAX_VALUE) {
            i = 8 - sp - 1;
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

        // There are some bugs for those with high significand, e.g., 995455.44
        // So we should further check
        if (temp / get10iP(i) != v) {
            return 8;
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

    public static int getSP(double v) {
        if (v >= 1) {
            int i = 0;
            while (i < mapSPGreater1.length - 1) {
                if (v < mapSPGreater1[i + 1]) {
                    return i;
                }
                i++;
            }
        } else {
            int i = 1;
            while (i < mapSPLess1.length) {
                if (v >= mapSPLess1[i]) {
                    return -i;
                }
                i++;
            }
        }
        return (int) Math.floor(Math.log10(v));
    }

    private static int[] getSPAnd10iNFlag(double v) {
        int[] spAnd10iNFlag = new int[2];
        if (v >= 1) {
            int i = 0;
            while (i < mapSPGreater1.length - 1) {
                if (v < mapSPGreater1[i + 1]) {
                    spAnd10iNFlag[0] = i;
                    return spAnd10iNFlag;
                }
                i++;
            }
        } else {
            int i = 1;
            while (i < mapSPLess1.length) {
                if (v >= mapSPLess1[i]) {
                    spAnd10iNFlag[0] = -i;
                    spAnd10iNFlag[1] = v == mapSPLess1[i] ? 1 : 0;
                    return spAnd10iNFlag;
                }
                i++;
            }
        }
        double log10v = Math.log10(v);
        spAnd10iNFlag[0] = (int) Math.floor(log10v);
        spAnd10iNFlag[1] = log10v == (long)log10v ? 1 : 0;
        return spAnd10iNFlag;
    }
}
