package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.urbcomp.startdb.compress.elf.xordecompressor.ElfXORDecompressorPre1O;

import java.io.IOException;

public class ElfDecompressorPre1O extends AbstractElfDecompressor {
    private final ElfXORDecompressorPre1O xorDecompressor;

    public ElfDecompressorPre1O(byte[] bytes) {
        xorDecompressor = new ElfXORDecompressorPre1O(bytes);
    }

    @Override protected Double xorDecompress() {
        return xorDecompressor.readValue();
    }

    @Override protected int readInt(int len) {
        InputBitStream in = xorDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
