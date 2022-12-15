package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfPlusDecompressor implements IDecompressor {

    private int lastBetaStar = Integer.MAX_VALUE;

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        Double v;

        if(readInt(1) == 0) {
            v = recoverVByBetaStar(lastBetaStar);   // case 0
        } else if (readInt(1) == 0) {
            v = xorDecompress();                    // case 10
        } else {
            lastBetaStar = readInt(4);           // case 11
            v = recoverVByBetaStar(lastBetaStar);
        }
        return v;
    }

    protected abstract Double xorDecompress();

    protected abstract int readInt(int len);


    private Double recoverVByBetaStar(int betaStar) {
        double v;
        Double vPrime = xorDecompress();
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
