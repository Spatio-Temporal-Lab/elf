package org.urbcomp.startdb.compress.elf.eraser;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ElfEraser implements IEraser {
    @Override public EraserResult erase(double v, BiFunction<Integer, Integer, Integer> writeInt,
                    Function<Boolean, Integer> writeBit) {
        long vLong = Double.doubleToLongBits(v);    //doubleToLongBits can normalize NaN
        long vPrimeLong;
        int size = 0;

        if (v == 0.0 || Double.isInfinite(v) || Double.isNaN(v)) {
            size += writeBit.apply(false);
            vPrimeLong = vLong;
        } else {
            int[] alphaAndBetaStar = ElfUtils.getAlphaAndBetaStar(v);
            int e = ((int) (vLong >> 52)) & 0x7ff;
            int gAlpha = ElfUtils.getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {
                size += writeInt.apply(alphaAndBetaStar[1] | 0x10, 5);
                vPrimeLong = mask & vLong;
            } else {
                size += writeBit.apply(false);
                vPrimeLong = vLong;
            }
        }
        return new EraserResult(size, vPrimeLong);
    }
}
