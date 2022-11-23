package org.urbcomp.startdb.compress.elf.decompressor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfDecompressor implements IDecompressor {

    private final static double[] map10iN =
                    new double[] {1.0, 1.0E-1, 1.0E-2, 1.0E-3, 1.0E-4, 1.0E-5, 1.0E-6, 1.0E-7,
                                    1.0E-8, 1.0E-9, 1.0E-10, 1.0E-11, 1.0E-12, 1.0E-13, 1.0E-14,
                                    1.0E-15, 1.0E-16, 1.0E-17, 1.0E-18, 1.0E-19, 1.0E-20};

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        int flag = readInt(1);

        Double v;
        if (flag == 0) {
            v = xorDecompress();
        } else {
            int betaStar = readInt(4);
            Double vPrime = xorDecompress();
            int sp = getStartSignificandPosition(vPrime);
            if (betaStar == 0) {
                v = get10iN(-sp - 1);
                if (vPrime < 0) {
                    v = -v;
                }
            } else {
                int alpha = betaStar - sp - 1;
                v = roundUp(vPrime, alpha);
            }
        }
        return v;
    }

    protected abstract Double xorDecompress();

    protected abstract int readInt(int len);

    private static int getStartSignificandPosition(double v) {
        return (int) Math.floor(Math.log10(Math.abs(v)));
    }

    private static double get10iN(int i) {
        if (i <= 0) {
            throw new IllegalArgumentException("The argument should be greater than 0");
        }
        if (i >= map10iN.length) {
            return new BigDecimal("1.0E-" + i).doubleValue();
        } else {
            return map10iN[i];
        }
    }

    private static double roundUp(double v, int alpha) {
        BigDecimal bd = new BigDecimal(v);
        return bd.setScale(alpha, RoundingMode.UP).doubleValue();
    }
}
