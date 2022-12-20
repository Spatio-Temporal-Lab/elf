package org.urbcomp.startdb.compress.elf.restorer;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public class ElfRestorer implements IRestorer {
    @Override public Double restore(IntUnaryOperator readInt, Supplier<Double> xorDecompress) {
        int flag = readInt.applyAsInt(1);

        Double v;
        if (flag == 0) {
            v = xorDecompress.get();
        } else {
            int betaStar = readInt.applyAsInt(4);
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
        }
        return v;
    }
}
