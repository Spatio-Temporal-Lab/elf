package org.urbcomp.startdb.compress.elf.compressor;

import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import fi.iki.yak.ts.compression.gorilla.Compressor;

public class GorillaCompressor implements ICompressor {
    private final Compressor gorilla;

    public GorillaCompressor() {
        this.gorilla = new Compressor(new ByteBufferBitOutput());
    }

    @Override public void addValue(double v) {
        gorilla.addValue(v);
    }

    @Override public int getSize() {
        return gorilla.getSize();
    }
}
