package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.Decompressor;

import java.util.List;

public class GorillaDecompressor implements IDecompressor {
    private final Decompressor gorillaDecompressor;

    public GorillaDecompressor(byte[] bytes) {
        gorillaDecompressor = new Decompressor(new ByteBufferBitInput(bytes));
    }

    @Override public List<Double> decompress() {
        return gorillaDecompressor.getValues();
    }
}
