package org.urbcomp.startdb.compress.elf.eraser;

import org.urbcomp.startdb.compress.elf.utils.function.BiInt2IntFunction;
import org.urbcomp.startdb.compress.elf.utils.function.Bool2IntFunction;
import org.urbcomp.startdb.compress.elf.utils.function.Long2IntFunction;

public interface IEraser {
    int erase(double v, BiInt2IntFunction writeInt,
                    Bool2IntFunction writeBit, Long2IntFunction xorCompress);
    void markEnd(BiInt2IntFunction writeInt);
}
