package org.urbcomp.startdb.compress.elf.restorer;

import org.urbcomp.startdb.compress.elf.utils.function.Int2IntFunction;

import java.util.function.Supplier;

public interface IRestorer {
    Double restore(Int2IntFunction readInt, Supplier<Double> xorDecompress);
}
