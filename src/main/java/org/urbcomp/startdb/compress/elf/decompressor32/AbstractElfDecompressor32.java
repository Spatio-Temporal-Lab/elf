package org.urbcomp.startdb.compress.elf.decompressor32;

import org.urbcomp.startdb.compress.elf.utils.Elf32Utils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfDecompressor32 implements IDecompressor32 {
    private int lastBetaStar = Integer.MAX_VALUE;

    public List<Float> decompress() {
        List<Float> values = new ArrayList<>(1024);
        Float value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Float nextValue() {
        Float v;

        if (readInt(1) == 0) {
            v = recoverVByBetaStar();               // case 0
        } else if (readInt(1) == 0) {
            v = xorDecompress();                    // case 10
        } else {
            lastBetaStar = readInt(3);          // case 11
            v = recoverVByBetaStar();
        }
        return v;
    }

    private float recoverVByBetaStar() {
        float v;
        Float vPrime = xorDecompress();
        int sp = Elf32Utils.getSP(Math.abs(vPrime));
        if (lastBetaStar == 0) {
            v = Elf32Utils.get10iN(-sp - 1);
            if (vPrime < 0) {
                v = -v;
            }
        } else {
            int alpha = lastBetaStar - sp - 1;
            v = Elf32Utils.roundUp(vPrime, alpha);
        }
        return v;
    }

    protected abstract Float xorDecompress();

    protected abstract int readInt(int len);

}
