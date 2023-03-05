package org.urbcomp.startdb.compress.elf.compressor32;

import fi.iki.yak.ts.compression.gorilla.CompressorOS;
import gr.aueb.delorean.chimp.Compressor32OS;
import gr.aueb.delorean.chimp.OutputBitStream;

public class ElfOnGorillaCompressor32OS extends AbstractElfCompressor32{
    private final Compressor32OS gorilla32;

    public ElfOnGorillaCompressor32OS(){
        gorilla32 = new Compressor32OS();
    }

    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = gorilla32.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = gorilla32.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(int vPrimeInt) {
        return gorilla32.addValue(vPrimeInt);
    }

    @Override public byte[] getBytes() {
        return gorilla32.getOutputStream().getBuffer();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(2, 2); // case 10
        gorilla32.close();
    }
}
