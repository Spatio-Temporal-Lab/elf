package org.urbcomp.startdb.compress.elf.decompressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitInput;
import fi.iki.yak.ts.compression.gorilla.Decompressor;
import fi.iki.yak.ts.compression.gorilla.Value;

public class ElfOnGorillaDecompressor extends AbstractElfDecompressor {
    private final Decompressor gorillaDecompressor;

    public ElfOnGorillaDecompressor(byte[] bytes) {
        gorillaDecompressor = new Decompressor(new ByteBufferBitInput(bytes));
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
        ByteBufferBitInput in = (ByteBufferBitInput) gorillaDecompressor.getInputStream();
        return (int) in.getLong(len);
    }
}
