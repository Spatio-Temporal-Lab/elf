package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.ChimpDecompressor;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class ElfOnChimpDecompressor extends AbstractElfDecompressor {
    private final ChimpDecompressor chimpDecompressor;

    public ElfOnChimpDecompressor(byte[] bytes) {
        chimpDecompressor = new ChimpDecompressor(bytes);
    }

    @Override
    protected Double xorDecompress() {
        return chimpDecompressor.readValue();
    }

    @Override
    protected int readInt(int len) {
        InputBitStream in = chimpDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
