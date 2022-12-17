package org.urbcomp.startdb.compress.elf.compressor;

import org.urbcomp.startdb.compress.elf.eraser.IEraser;

public abstract class AbstractElfCompressor implements ICompressor {

    private int size = 0;

    private final IEraser eraser;

    public AbstractElfCompressor(IEraser eraser) {
        this.eraser = eraser;
    }

    public void addValue(double v) {
        size += eraser.erase(v, this::writeInt, this::writeBit, this::xorCompress);
    }

    public int getSize() {
        return size;
    }

    protected abstract int writeInt(int n, int len);

    protected abstract int writeBit(boolean bit);

    protected abstract int xorCompress(long vPrimeLong, int betaStar);

    @Override public String getKey() {
        return getClass().getSimpleName() + "_" + this.eraser.getClass().getSimpleName();
    }

    @Override public void close() {
        this.eraser.markEnd(this::writeInt);
    }
}
