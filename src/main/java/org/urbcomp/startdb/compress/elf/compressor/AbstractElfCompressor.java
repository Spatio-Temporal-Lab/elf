package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

public abstract class AbstractElfCompressor implements ICompressor {

    private int size = 0;

    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);
        long vPrimeLong;
        int betaStar = 0;

        if (v == 0.0 || Double.isInfinite(v)) {
            size += writeBit(false);
            vPrimeLong = vLong;
        } else if (Double.isNaN(v)) {
            size += writeBit(false);
            vPrimeLong = 0xfff8000000000000L & vLong;
        } else {
            int[] alphaAndBetaStar = ElfUtils.getAlphaAndBetaStar(v);
            betaStar = alphaAndBetaStar[1];
            int e = ((int) (vLong >> 52)) & 0x7ff;
            int gAlpha = ElfUtils.getFAlpha(alphaAndBetaStar[0]) + e - 1023;
            int eraseBits = 52 - gAlpha;
            long mask = 0xffffffffffffffffL << eraseBits;
            long delta = (~mask) & vLong;
            if (alphaAndBetaStar[1] < 16 && delta != 0 && eraseBits > 4) {
                size += writeInt(alphaAndBetaStar[1] | 0x10, 5);
                vPrimeLong = mask & vLong;
            } else {
                size += writeBit(false);
                vPrimeLong = vLong;
            }
        }
        size += xorCompress(vPrimeLong, betaStar);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong, int betaStar);
}
