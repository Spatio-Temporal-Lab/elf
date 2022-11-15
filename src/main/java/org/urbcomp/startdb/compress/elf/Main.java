package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] vs = new double[] {
                        93.85,
                        -38.88,
                        532.64,
                        326.52,
                        -1107.21,
                        211.1,
                        9.34,
                        238.77,
                        -103.54};
        ICompressor compressor = new ElfOnChimpCompressor();
        for (double v : vs) {
            compressor.addValue(v);
        }
        compressor.close();

        System.out.println(compressor.getSize());

        byte[] result = compressor.getBytes();
        IDecompressor decompressor = new ElfOnChimpDecompressor(result);
        List<Double> values = decompressor.decompress();
        assert(values.size() == vs.length);
        for (int i = 0; i < values.size(); i++) {
            assert(vs[i] == values.get(i));
        }
    }
}
