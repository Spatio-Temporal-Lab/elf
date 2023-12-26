package org.urbcomp.startdb.compress.elf.compressor32;

import org.urbcomp.startdb.compress.elf.utils.Elf32Utils;

public abstract class AbstractElfCompressor32 implements ICompressor32 {
    private int size = 0;

    private int lastBetaStar = Integer.MAX_VALUE;

    public void addValue(float v) {
        int vInt = Float.floatToRawIntBits(v);
        int vPrimeInt;

        if (v == 0.0 || Float.isInfinite(v)) {
            size += writeInt(2, 2); // case 10
            vPrimeInt = vInt;
        } else if (Float.isNaN(v)) {
            size += writeInt(2, 2); // case 10
            vPrimeInt = 0x7fc00000;
        } else {
            // C1: v is a normal or subnormal
            int[] alphaAndBetaStar = Elf32Utils.getAlphaAndBetaStar(v, lastBetaStar);
            int e = (vInt >> 23) & 0xff;
            int gAlpha = Elf32Utils.getFAlpha(alphaAndBetaStar[0]) + e - 127;
            int eraseBits = 23 - gAlpha;
            int mask = 0xffffffff << eraseBits;
            int delta = (~mask) & vInt;
            if (delta != 0 && eraseBits > 3) {
                if(alphaAndBetaStar[1] == lastBetaStar) {
                    size += writeBit(false);    // case 0
                } else {
                    size += writeInt(alphaAndBetaStar[1] | 0x18, 5);  // case 11, 2 + 3 = 5
                    lastBetaStar = alphaAndBetaStar[1];
                }
                vPrimeInt = mask & vInt;
            } else {
                size += writeInt(2, 2); // case 10
                vPrimeInt = vInt;
            }
        }
        size += xorCompress(vPrimeInt);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(int vPrimeInt);

}
