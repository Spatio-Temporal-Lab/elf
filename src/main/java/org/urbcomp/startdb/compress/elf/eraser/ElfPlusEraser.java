package org.urbcomp.startdb.compress.elf.eraser;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ElfPlusEraser implements IEraser {
    private int lastBetaStar = Integer.MAX_VALUE;
    @Override public EraserResult erase(double v, BiFunction<Integer, Integer, Integer> writeInt,
                    Function<Boolean, Integer> writeBit) {
        long vLong = Double.doubleToLongBits(v);   //doubleToLongBits can normalize NaN
        long vPrimeLong;
        int size = 0;

        if (v == 0.0 || Double.isInfinite(v) || Double.isNaN(v)) {
            size += writeInt.apply(2, 2); // case 10
            vPrimeLong = vLong;
        } else {
            // C1: v is a normal or subnormal
            int[] alphaAndBetaStar = ElfUtils.getAlphaAndBetaStar(v);
            int e = ((int) (vLong >> 52)) & 0x7ff;
            int gAlpha = ElfUtils.getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {  // C2
                if(alphaAndBetaStar[1] == lastBetaStar) {
                    size += writeBit.apply(false);     // case 0
                } else {
                    size += writeInt.apply(alphaAndBetaStar[1] | 0x30, 6);    // case 11
                    lastBetaStar = alphaAndBetaStar[1];
                }
                vPrimeLong = mask & vLong;
            } else {
                size += writeInt.apply(2, 2); // case 10
                vPrimeLong = vLong;
            }
        }
        return new EraserResult(size, vPrimeLong);
    }
}
