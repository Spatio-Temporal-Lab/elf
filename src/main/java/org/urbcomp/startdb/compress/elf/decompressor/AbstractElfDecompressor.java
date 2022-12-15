package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.utils.ElfUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfDecompressor implements IDecompressor {
    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        int flag = readInt(1);

        Double v;
        if (flag == 0) {
            v = xorDecompress();
        } else {
            int betaStar = readInt(4);
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
        }
        return v;
    }

    protected abstract Double xorDecompress();

    protected abstract int readInt(int len);
}
