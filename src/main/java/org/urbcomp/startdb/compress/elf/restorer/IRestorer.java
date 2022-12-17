package org.urbcomp.startdb.compress.elf.restorer;

import java.util.function.IntFunction;
import java.util.function.Function;

public interface IRestorer {
    Double restore(IntFunction<Integer> readInt, Function<Integer, Double> xorDecompress);
}
