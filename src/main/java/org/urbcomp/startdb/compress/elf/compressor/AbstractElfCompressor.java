package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.eraser.ElfEraser;
import org.urbcomp.startdb.compress.elf.eraser.IEraser;

public abstract class AbstractElfCompressor implements ICompressor {

    private int size = 0;

    private final IEraser eraser = new ElfEraser();

    public void addValue(double v) {
        size += eraser.erase(v, this::writeInt, this::writeBit, this::xorCompress);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong);
}
