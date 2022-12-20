package org.urbcomp.startdb.compress.elf.restorer;

import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

public interface IRestorer {
    Double restore(IntUnaryOperator readInt, Supplier<Double> xorDecompress);
}
