package org.urbcomp.startdb.compress.elf;

import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        double[] vs = new double[] {3.14, 3.15, 2.0};
        ICompressor compressor = new ElfOnGorillaCompressor();
        for (double v : vs) {
            compressor.addValue(v);
        }
        compressor.close();

        System.out.println(compressor.getSize());

        byte[] result = compressor.getBytes();
        IDecompressor decompressor = new ElfOnGorillaDecompressor(result);
        List<Double> values = decompressor.decompress();
        for (Double v : values) {
            System.out.println(v);
        }
    }
}
