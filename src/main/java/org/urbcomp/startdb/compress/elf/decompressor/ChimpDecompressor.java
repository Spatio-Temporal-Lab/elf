package org.urbcomp.startdb.compress.elf.decompressor;

import java.util.List;

public class ChimpDecompressor implements IDecompressor {
    private final gr.aueb.delorean.chimp.ChimpDecompressor chimpDecompressor;
    public ChimpDecompressor(byte[] bytes) {
        chimpDecompressor = new gr.aueb.delorean.chimp.ChimpDecompressor(bytes);
    }
    @Override public List<Double> decompress() {
        return chimpDecompressor.getValues();
    }
}
