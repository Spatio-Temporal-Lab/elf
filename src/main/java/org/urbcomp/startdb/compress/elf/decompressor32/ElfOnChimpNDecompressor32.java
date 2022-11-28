package org.urbcomp.startdb.compress.elf.decompressor32;

import gr.aueb.delorean.chimp.InputBitStream;
import gr.aueb.delorean.chimp.ChimpNDecompressor32;
import java.io.IOException;

public class ElfOnChimpNDecompressor32 extends AbstractElfDecompressor32{
    private final ChimpNDecompressor32 chimpNDecompressor32;

    public ElfOnChimpNDecompressor32(byte[] bytes, int previousValues) {
        chimpNDecompressor32 = new ChimpNDecompressor32(bytes, previousValues);
    }

    @Override protected Float xorDecompress() {
        return chimpNDecompressor32.readValue();
    }

    @Override protected int readInt(int len) {
        InputBitStream in = chimpNDecompressor32.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
