package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.DecompressorOS;
import fi.iki.yak.ts.compression.gorilla.Value;
import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;

public class ElfOnGorillaDecompressorOS extends AbstractElfDecompressor {
    private final DecompressorOS gorillaDecompressor;

    public ElfOnGorillaDecompressorOS(byte[] bytes) {
        gorillaDecompressor = new DecompressorOS(bytes);
    }

    @Override protected Double xorDecompress() {
        Value value = gorillaDecompressor.readPair();
        if (value == null) {
            return null;
        } else {
            return value.getDoubleValue();
        }
    }

    @Override protected int readInt(int len) {
        InputBitStream in = gorillaDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
