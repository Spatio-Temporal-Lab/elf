package org.urbcomp.startdb.compress.elf.decompressor32;

import gr.aueb.delorean.chimp.Decompressor32OS;

import java.util.List;

public class GorillaDecompressor32OS implements IDecompressor32{
    private final Decompressor32OS gorillaDecompressor32;
    public GorillaDecompressor32OS(byte[] bytes) {
        gorillaDecompressor32 = new Decompressor32OS(bytes);
    }
    @Override public List<Float> decompress() {
        return gorillaDecompressor32.getValues();
    }
}
