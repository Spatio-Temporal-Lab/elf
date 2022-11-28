package org.urbcomp.startdb.compress.elf.decompressor32;


import java.util.List;

public class ChimpNDecompressor32 implements IDecompressor32 {
    private final gr.aueb.delorean.chimp.ChimpNDecompressor32 chimpNDecompressor32;

    public ChimpNDecompressor32(byte[] bytes, int previousValues) {
        chimpNDecompressor32 = new gr.aueb.delorean.chimp.ChimpNDecompressor32(bytes, previousValues);
    }

    @Override
    public List<Float> decompress() {
        return chimpNDecompressor32.getValues();
    }
}
