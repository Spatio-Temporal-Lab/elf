package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.Chimp;

public class ChimpCompressor implements ICompressor {
    private final Chimp chimp;
    public ChimpCompressor() {
        chimp = new Chimp();
    }
    @Override public void addValue(double v) {
        chimp.addValue(v);
    }

    @Override public int getSize() {
        return chimp.getSize();
    }

    @Override public byte[] getBytes() {
        return chimp.getOut();
    }

    @Override public void close() {
        chimp.close();
    }
}
