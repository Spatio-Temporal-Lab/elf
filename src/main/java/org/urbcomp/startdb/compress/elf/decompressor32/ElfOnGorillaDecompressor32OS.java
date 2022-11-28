package org.urbcomp.startdb.compress.elf.decompressor32;

import gr.aueb.delorean.chimp.Decompressor32OS;
import gr.aueb.delorean.chimp.InputBitStream;
import gr.aueb.delorean.chimp.Value;

import java.io.IOException;

public class ElfOnGorillaDecompressor32OS extends AbstractElfDecompressor32{
    private final Decompressor32OS gorillaDecompressor32;

    public ElfOnGorillaDecompressor32OS(byte[] bytes) {
        gorillaDecompressor32 = new Decompressor32OS(bytes);
    }

    @Override protected Float xorDecompress() {
        Value value = gorillaDecompressor32.readValue();
        if (value == null) {
            return null;
        } else {
            return value.getFloatValue();
        }
    }

    @Override protected int readInt(int len) {
        InputBitStream in = gorillaDecompressor32.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
