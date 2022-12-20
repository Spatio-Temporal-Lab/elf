package org.urbcomp.startdb.compress.elf.restorer;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class ElfPlusRestorer implements IRestorer {
    private int lastBetaStar = Integer.MAX_VALUE;

    @Override public Double restore(IntUnaryOperator readInt, Supplier<Double> xorDecompress) {
        Double v;

        if(readInt.applyAsInt(1) == 0) {
            v = recoverVByBetaStar(lastBetaStar, xorDecompress);   // case 0
        } else if (readInt.applyAsInt(1) == 0) {
            v = xorDecompress.get();                    // case 10
        } else {
            lastBetaStar = readInt.applyAsInt(4);           // case 11
            v = recoverVByBetaStar(lastBetaStar, xorDecompress);
        }
        return v;
    }

    private Double recoverVByBetaStar(int betaStar, Supplier<Double> xorDecompress) {
        double v;
        Double vPrime = xorDecompress.get();
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
