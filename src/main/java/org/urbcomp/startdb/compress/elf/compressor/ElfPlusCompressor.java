package org.urbcomp.startdb.compress.elf.compressor;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.eraser.IEraser;
import org.urbcomp.startdb.compress.elf.xorcompressor.ElfPlusXORCompressor;

public class ElfPlusCompressor extends AbstractElfCompressor {
    private final ElfPlusXORCompressor xorCompressor;

    public ElfPlusCompressor(IEraser eraser) {
        super(eraser);
        xorCompressor = new ElfPlusXORCompressor();
    }

    @Override protected int writeInt(int n, int len) {
        OutputBitStream os = xorCompressor.getOutputStream();
        os.writeInt(n, len);
        return len;
    }

    @Override protected int writeBit(boolean bit) {
        OutputBitStream os = xorCompressor.getOutputStream();
        os.writeBit(bit);
        return 1;
    }

    @Override protected int xorCompress(long vPrimeLong, int betaStar) {
        return xorCompressor.addValue(vPrimeLong, betaStar);
    }

    @Override public byte[] getBytes() {
        return xorCompressor.getOut();
    }

    @Override public void close() {
        super.close();
        xorCompressor.close();
    }
}
