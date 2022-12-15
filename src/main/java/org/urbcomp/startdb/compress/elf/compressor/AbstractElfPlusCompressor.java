package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

public abstract class AbstractElfPlusCompressor implements ICompressor {

    private int size = 0;

    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(double v) {
        long vLong = Double.doubleToLongBits(v);   //doubleToLongBits can normalize NaN
        long vPrimeLong;

        if (v == 0.0 || Double.isInfinite(v) || Double.isNaN(v)) {
            size += writeInt(2, 2); // case 10
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
                    size += writeBit(false);     // case 0
                } else {
                    size += writeInt(alphaAndBetaStar[1] | 0x30, 6);    // case 11
                    lastBetaStar = alphaAndBetaStar[1];
                }
                vPrimeLong = mask & vLong;
            } else {
                size += writeInt(2, 2); // case 10
                vPrimeLong = vLong;
            }
        }
        size += xorCompress(vPrimeLong);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong);
}
