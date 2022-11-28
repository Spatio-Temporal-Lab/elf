package org.urbcomp.startdb.compress.elf.compressor32;

import gr.aueb.delorean.chimp.Compressor32OS;

public class GorillaCompressor32OS implements ICompressor32{
    private final Compressor32OS gorilla;

    public GorillaCompressor32OS(){
        gorilla = new Compressor32OS();
    }

    @Override public void addValue(float v) {
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
