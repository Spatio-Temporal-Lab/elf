package org.urbcomp.startdb.compress.elf.decompressor32;

import gr.aueb.delorean.chimp.ChimpDecompressor32;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class ElfOnChimpDecompressor32 extends AbstractElfDecompressor32 {
    private final ChimpDecompressor32 chimpDecompressor32;

    public ElfOnChimpDecompressor32(byte[] bytes) {
        chimpDecompressor32 = new ChimpDecompressor32(bytes);
    }

    @Override
    protected Float xorDecompress() {
        return chimpDecompressor32.readValue();
    }

    @Override
    protected int readInt(int len) {
        InputBitStream in = chimpDecompressor32.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
