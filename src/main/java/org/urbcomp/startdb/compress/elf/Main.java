package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] vs = new double[] {3.14, 3.15, 2.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.MIN_NORMAL, Double.MIN_NORMAL - Double.MIN_VALUE};
        ICompressor compressor = new ChimpCompressor();
        for (double v : vs) {
            compressor.addValue(v);
        }
        compressor.close();

        System.out.println(compressor.getSize());

        byte[] result = compressor.getBytes();
        IDecompressor decompressor = new ChimpDecompressor(result);
        List<Double> values = decompressor.decompress();
        assert(values.size() == vs.length);
        for (int i = 0; i < values.size(); i++) {
            assert(vs[i] == values.get(i));
        }
    }
}
