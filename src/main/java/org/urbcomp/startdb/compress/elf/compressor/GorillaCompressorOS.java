package org.urbcomp.startdb.compress.elf.compressor;

import fi.iki.yak.ts.compression.gorilla.CompressorOS;

public class GorillaCompressorOS implements ICompressor {
    private final CompressorOS gorilla;
    public GorillaCompressorOS() {
        this.gorilla = new CompressorOS();
    }

    @Override public void addValue(double v) {
        this.gorilla.addValue(v);
    }

    @Override public int getSize() {
        return this.gorilla.getSize();
    }

    @Override public byte[] getBytes() {
        return this.gorilla.getOutputStream().getBuffer();
    }

    @Override public void close() {
        this.gorilla.close();
    }
}
