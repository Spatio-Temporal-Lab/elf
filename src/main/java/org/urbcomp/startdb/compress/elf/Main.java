package org.urbcomp.startdb.compress.elf;

import com.github.Tranway.buff.BuffCompressor;
import com.github.Tranway.buff.BuffDecompressor;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        double[] vs = new double[]{
                93.85,
                38.88,
                -532.64,
                326.52,
                1107.21,
                -211.1,
                9.34,
                -238.77,
                103.549888
        };
        BuffCompressor compressor = new BuffCompressor();
        compressor.compress(vs);
        byte[] result = compressor.getOut();
        BuffDecompressor decompressor = new BuffDecompressor(result);
        System.out.println(Arrays.toString(decompressor.decompress()));
    }
}
