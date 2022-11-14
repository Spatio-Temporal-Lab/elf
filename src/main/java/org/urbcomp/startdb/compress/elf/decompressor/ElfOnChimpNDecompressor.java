package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.ChimpNDecompressor;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class ElfOnChimpNDecompressor extends AbstractElfDecompressor {
    private final ChimpNDecompressor chimpNDecompressor;

    public ElfOnChimpNDecompressor(byte[] bytes, int previousValues) {
        chimpNDecompressor = new ChimpNDecompressor(bytes, previousValues);
    }

    @Override protected Double xorDecompress() {
        return chimpNDecompressor.readValue();
    }

    @Override protected int readInt(int len) {
        InputBitStream in = chimpNDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
