package org.urbcomp.startdb.compress.elf.eraser;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface IEraser {
    int erase(double v, BiFunction<Integer, Integer, Integer> writeInt,
                    Function<Boolean, Integer> writeBit, BiFunction<Long, Integer, Integer> xorCompress);
    void markEnd(BiFunction<Integer, Integer, Integer> writeInt);
}
