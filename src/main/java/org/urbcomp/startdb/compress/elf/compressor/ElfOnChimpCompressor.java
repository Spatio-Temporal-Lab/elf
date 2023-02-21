package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.Chimp;
import gr.aueb.delorean.chimp.OutputBitStream;

public class ElfOnChimpCompressor extends AbstractElfCompressor {
    private final Chimp chimp;

    public ElfOnChimpCompressor() {
        chimp = new Chimp();
    }

    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = chimp.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = chimp.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(long vPrimeLong) {
        return chimp.addValue(vPrimeLong);
    }

    @Override public byte[] getBytes() {
        return chimp.getOut();
    }

    @Override public void close() {
        // we write one more bit here, for marking an end of the stream.
        writeInt(2,2);  // case 10
        chimp.close();
    }
}
