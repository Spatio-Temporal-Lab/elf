package org.urbcomp.startdb.compress.elf.eraser;

public class EraserResult {
    private final int size;
    private final long vPrimeLong;
    public EraserResult(int size, long vPrimeLong) {
        this.size = size;
        this.vPrimeLong = vPrimeLong;
    }
    public int getSize() {
        return this.size;
    }
    public long getVPrimeLong() {
        return this.vPrimeLong;
    }
}
