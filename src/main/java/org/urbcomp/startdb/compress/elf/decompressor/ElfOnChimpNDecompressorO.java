package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.ChimpNDecompressorO;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class ElfOnChimpNDecompressorO extends AbstractElfDecompressor {
    private final ChimpNDecompressorO chimpNDecompressor;

    public ElfOnChimpNDecompressorO(byte[] bytes, int previousValues) {
        chimpNDecompressor = new ChimpNDecompressorO(bytes, previousValues);
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
