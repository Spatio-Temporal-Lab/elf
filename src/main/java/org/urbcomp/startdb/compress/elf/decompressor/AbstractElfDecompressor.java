package org.urbcomp.startdb.compress.elf.decompressor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfDecompressor {

    private final static double[] map10iN = new double[325];

    static {
        for (int i = 0; i < map10iN.length; i++) {
            map10iN[i] = new BigDecimal("1.0E-" + i).doubleValue();
        }
    }

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>();
        Double value;
        while((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        Double vPrime = xorDecompress();
        if(vPrime == null) {
            return null;
        }
        int flag = readInt(1);
        double v;
        if (flag == 0) {
            v = vPrime;
        } else {
            int betaStar = readInt(4);
            int sp = getStartSignificandPosition(vPrime);
            if (betaStar == 0) {
                v = get10iN(-sp - 1);
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
        return (int) Math.ceil(Math.log10(Math.abs(v)));
    }

    private static double get10iN(int i) {
        if (i <= 0 || i >= map10iN.length) {
            throw new IllegalArgumentException(
                            "The argument should be in [1, " + (map10iN.length - 1) + "]");
        }
        return map10iN[i];
    }

    private static double roundUp(double v, int alpha) {
        BigDecimal bd = new BigDecimal(v);
        return bd.setScale(alpha, RoundingMode.UP).doubleValue();
    }
}
