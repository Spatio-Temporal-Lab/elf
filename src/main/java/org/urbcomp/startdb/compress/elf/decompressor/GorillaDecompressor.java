package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.Decompressor;

import java.util.List;

public class GorillaDecompressor implements IDecompressor {
    private final Decompressor gorillaDecompressor;

    public GorillaDecompressor(ByteBufferBitInput in) {
        gorillaDecompressor = new Decompressor(in);
    }

    @Override public List<Double> decompress() {
        return gorillaDecompressor.getValues();
    }
}
