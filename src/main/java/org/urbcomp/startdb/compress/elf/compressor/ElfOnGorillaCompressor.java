package org.urbcomp.startdb.compress.elf.compressor;

import fi.iki.yak.ts.compression.gorilla.BitOutput;
import fi.iki.yak.ts.compression.gorilla.ByteBufferBitOutput;
import fi.iki.yak.ts.compression.gorilla.Compressor;

public class ElfOnGorillaCompressor extends AbstractElfCompressor{
    private final Compressor gorilla;

    public ElfOnGorillaCompressor() {
        this.gorilla = new Compressor(new ByteBufferBitOutput());
    }

    @Override protected int writeInt(int n, int len) {
        BitOutput os = gorilla.getOutputStream();
        os.writeBits(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        BitOutput os = gorilla.getOutputStream();
        os.writeBits(bit ? 1 : 0, 1);
        return 1;
    }

    @Override protected int xorCompress(long vPrimeLong) {
        return gorilla.addValue(vPrimeLong);
    }

    @Override public void close() {
        gorilla.close();
    }
}
