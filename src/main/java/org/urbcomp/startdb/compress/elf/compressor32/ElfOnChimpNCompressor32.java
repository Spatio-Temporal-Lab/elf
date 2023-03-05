package org.urbcomp.startdb.compress.elf.compressor32;

import gr.aueb.delorean.chimp.ChimpN32;
import gr.aueb.delorean.chimp.OutputBitStream;

public class ElfOnChimpNCompressor32 extends AbstractElfCompressor32{
    private final ChimpN32 chimpN32;

    public ElfOnChimpNCompressor32(int previousValues) {
        chimpN32 = new ChimpN32(previousValues);
    }
    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = chimpN32.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = chimpN32.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(int vPrimeInt) {
        return chimpN32.addValue(vPrimeInt);
    }

    @Override public byte[] getBytes() {
        return chimpN32.getOut();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(2, 2); // case 10
        chimpN32.close();
    }
}
