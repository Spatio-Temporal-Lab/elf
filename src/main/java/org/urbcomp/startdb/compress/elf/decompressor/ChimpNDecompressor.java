package org.urbcomp.startdb.compress.elf.decompressor;

import java.util.List;

public class ChimpNDecompressor implements IDecompressor {
    private final gr.aueb.delorean.chimp.ChimpNDecompressor chimpNDecompressor;
    public ChimpNDecompressor(byte[] bytes, int previousValues) {
        chimpNDecompressor = new gr.aueb.delorean.chimp.ChimpNDecompressor(bytes, previousValues);
    }
    @Override public List<Double> decompress() {
        return chimpNDecompressor.getValues();
    }
}
