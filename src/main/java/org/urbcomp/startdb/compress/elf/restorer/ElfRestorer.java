package org.urbcomp.startdb.compress.elf.restorer;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.IntFunction;
import java.util.function.Function;

public class ElfRestorer implements IRestorer {
    @Override public Double restore(IntFunction<Integer> readInt, Function<Integer, Double> xorDecompress) {
        int flag = readInt.apply(1);

        Double v;
        if (flag == 0) {
            v = xorDecompress.apply(Integer.MAX_VALUE);
        } else {
            int betaStar = readInt.apply(4);
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
        }
        return v;
    }
}
