package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.DecompressorOS;

import java.util.List;

public class GorillaDecompressorOS implements IDecompressor{
    private final DecompressorOS gorillaDecompressor;
    public GorillaDecompressorOS(byte[] bytes) {
        gorillaDecompressor = new DecompressorOS(bytes);
    }
    @Override public List<Double> decompress() {
        return gorillaDecompressor.getValues();
    }
}
