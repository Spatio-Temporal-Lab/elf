package org.urbcomp.startdb.compress.elf.restorer;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface IRestorer {
    Double restore(IntFunction<Integer> readInt, Supplier<Double> xorDecompress);
}
