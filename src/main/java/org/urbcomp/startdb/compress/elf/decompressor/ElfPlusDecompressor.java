package org.urbcomp.startdb.compress.elf.decompressor;

import gr.aueb.delorean.chimp.InputBitStream;
import org.urbcomp.startdb.compress.elf.restorer.IRestorer;
import org.urbcomp.startdb.compress.elf.xordecompressor.ElfPlusXORDecompressor;

import java.io.IOException;

public class ElfPlusDecompressor extends AbstractElfDecompressor {
    private final ElfPlusXORDecompressor xorDecompressor;

    public ElfPlusDecompressor(IRestorer restorer, byte[] bytes) {
        super(restorer);
        xorDecompressor = new ElfPlusXORDecompressor(bytes);
    }

    @Override protected Double xorDecompress(int betaStar) {
        return xorDecompressor.readValue(betaStar);
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
