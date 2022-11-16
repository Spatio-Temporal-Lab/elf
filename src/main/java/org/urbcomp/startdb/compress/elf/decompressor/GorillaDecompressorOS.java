package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.DecompressorOptimizeStream;

import java.util.List;

public class GorillaDecompressorOS implements IDecompressor{
    private final DecompressorOptimizeStream gorillaDecompressor;
    public GorillaDecompressorOS(byte[] bytes) {
        gorillaDecompressor = new DecompressorOptimizeStream(bytes);
    }
    @Override public List<Double> decompress() {
        return gorillaDecompressor.getValues();
    }
}
