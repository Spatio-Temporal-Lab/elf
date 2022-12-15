package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.eraser.ElfPlusEraser;
import org.urbcomp.startdb.compress.elf.eraser.EraserResult;
import org.urbcomp.startdb.compress.elf.eraser.IEraser;

public abstract class AbstractElfPlusCompressor implements ICompressor {

    private int size = 0;

    private final IEraser eraser = new ElfPlusEraser();

    public void addValue(double v) {
        EraserResult er = eraser.erase(v, this::writeInt, this::writeBit);
        size += er.getSize();
        size += xorCompress(er.getVPrimeLong());
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong);
}
