package org.urbcomp.startdb.compress.elf.decompressor32;


import java.util.List;

public class ChimpDecompressor32 implements IDecompressor32 {
    private final gr.aueb.delorean.chimp.ChimpDecompressor32 chimpDecompressor32;

    public ChimpDecompressor32(byte[] bytes) {
        chimpDecompressor32 = new gr.aueb.delorean.chimp.ChimpDecompressor32(bytes);
    }

    @Override
    public List<Float> decompress() {
        return chimpDecompressor32.getValues();
    }
}
