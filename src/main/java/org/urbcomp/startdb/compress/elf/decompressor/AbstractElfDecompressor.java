package org.urbcomp.startdb.compress.elf.decompressor;

import org.urbcomp.startdb.compress.elf.restorer.ElfRestorer;
import org.urbcomp.startdb.compress.elf.restorer.IRestorer;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractElfDecompressor implements IDecompressor {

    private final IRestorer restorer = new ElfRestorer();

    public List<Double> decompress() {
        List<Double> values = new ArrayList<>(1024);
        Double value;
        while ((value = nextValue()) != null) {
            values.add(value);
        }
        return values;
    }

    private Double nextValue() {
        return restorer.restore(this::readInt, this::xorDecompress);
    }

    protected abstract Double xorDecompress();

    protected abstract int readInt(int len);
}
