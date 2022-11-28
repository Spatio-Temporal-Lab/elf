package org.urbcomp.startdb.compress.elf.compressor32;

import gr.aueb.delorean.chimp.Chimp32;

public class ChimpCompressor32 implements ICompressor32 {
    private final Chimp32 chimp32;

    public ChimpCompressor32() {
        chimp32 = new Chimp32();
    }

    @Override
    public void addValue(float v) {
        chimp32.addValue(v);
    }

    @Override
    public int getSize() {
        return chimp32.getSize();
    }

    @Override
    public byte[] getBytes() {
        return chimp32.getOut();
    }

    @Override
    public void close() {
        chimp32.close();
    }
}
