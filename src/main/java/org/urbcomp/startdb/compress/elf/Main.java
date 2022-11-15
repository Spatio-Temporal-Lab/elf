package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] vs = new double[] {3.14, 3.15};
        ICompressor compressor = new ElfOnChimpCompressor();
        for (double v : vs) {
            compressor.addValue(v);
        }
        compressor.close();

        System.out.println(compressor.getSize());

        byte[] result = compressor.getBytes();
        IDecompressor decompressor = new ElfOnChimpDecompressor(result);
        List<Double> values = decompressor.decompress();
        for (Double v : values) {
            System.out.println(v);
        }
    }
}
