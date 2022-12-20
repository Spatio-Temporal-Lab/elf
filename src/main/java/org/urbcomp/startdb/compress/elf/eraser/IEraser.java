package org.urbcomp.startdb.compress.elf.eraser;

import org.urbcomp.startdb.compress.elf.utils.function.Bool2IntFunction;

import java.util.function.IntBinaryOperator;
import java.util.function.LongToIntFunction;

public interface IEraser {
    int erase(double v, IntBinaryOperator writeInt,
                    Bool2IntFunction writeBit, LongToIntFunction xorCompress);
    void markEnd(IntBinaryOperator writeInt);
}
