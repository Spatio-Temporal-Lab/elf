package org.urbcomp.startdb.compress.elf.restorer;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.IntFunction;
import java.util.function.Function;

public class ElfPlusRestorer implements IRestorer {
    private int lastBetaStar = Integer.MAX_VALUE;

    @Override public Double restore(IntFunction<Integer> readInt, Function<Integer, Double> xorDecompress) {
        Double v;

        if(readInt.apply(1) == 0) {
            v = recoverVByBetaStar(lastBetaStar, xorDecompress);   // case 0
        } else if (readInt.apply(1) == 0) {
            v = xorDecompress.apply(lastBetaStar);           // case 10
        } else {
            lastBetaStar = readInt.apply(4);           // case 11
            v = recoverVByBetaStar(lastBetaStar, xorDecompress);
        }
        return v;
    }

    private Double recoverVByBetaStar(int betaStar, Function<Integer, Double> xorDecompress) {
        double v;
        Double vPrime = xorDecompress.apply(betaStar);
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
        return v;
    }
}
