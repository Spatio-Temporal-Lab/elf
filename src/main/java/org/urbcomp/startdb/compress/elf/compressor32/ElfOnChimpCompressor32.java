package org.urbcomp.startdb.compress.elf.compressor32;

import gr.aueb.delorean.chimp.Chimp32;
import gr.aueb.delorean.chimp.OutputBitStream;

public class ElfOnChimpCompressor32 extends AbstractElfCompressor32 {
    private final Chimp32 chimp32;

    public ElfOnChimpCompressor32() {
        chimp32 = new Chimp32();
    }

    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = chimp32.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = chimp32.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(int vInt) {
        return chimp32.addValue(vInt);
    }

    @Override public byte[] getBytes() {
        return chimp32.getOut();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(2,2);  // case 10
        chimp32.close();
    }
}
