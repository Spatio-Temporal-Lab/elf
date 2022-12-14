package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfPlusDecompressor implements IDecompressor {

    private int lastBetaStar = Integer.MAX_VALUE;

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

        int betaStar = Integer.MAX_VALUE;

        Double v;
        if (flag == 0) {
            v = xorDecompress(betaStar);
        } else {
            flag = readInt(1);
            if(flag == 0) {
                betaStar = lastBetaStar;
            } else {
                betaStar = readInt(4);
                lastBetaStar = betaStar;
            }

            Double vPrime = xorDecompress(betaStar);
            int sp = (int) Math.floor(Math.log10(Math.abs(vPrime)));
            if (betaStar == 0) {
                v = ElfUtils.get10iN(-sp - 1);
                if (vPrime < 0) {
                    v = -v;
                }
            } else {
                int alpha = betaStar - sp - 1;
                v = ElfUtils.roundUp(vPrime, alpha);
            }
        }
        return v;
    }

    protected abstract Double xorDecompress(int betaStar);

    protected abstract int readInt(int len);

}
