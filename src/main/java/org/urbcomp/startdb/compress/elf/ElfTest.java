package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.Chimp;
import gr.aueb.delorean.chimp.OutputBitStream;
import sun.misc.DoubleConsts;

import java.io.IOException;
import java.util.BitSet;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.*;
import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.computeFn;

public class ElfTest {
    private final int EXPONENTIAL_DIGIT = 52;
    private final int SIGN_DIGIT = 63;
    private BitSet rawBitSet;
    private int flag;
    private int sign;
    private int exp;
    private int fn;
    private int eraser_bits;
    private int precision;
    private long result;
    private long result_eraser;
    private int size;
    private OutputBitStream out;
    private Chimp chimp;

    public ElfTest() {
        out = new OutputBitStream(new byte[1000 * 8]);
        size = 0;
        chimp = new Chimp(out);
    }
    public void addValue(double value) throws IOException {
        compressWithChimp(value);
    }



    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        out.writeBit(false);
        chimp.close();
    }

    public void compressWithChimp(double value) throws IOException {
        compressParameter(value);
        out.writeBit(flag);
        size += 1;
        if (flag != 0) {
            out.writeInt(precision, 4);
            size += 4;
        }
        chimp.addValue(result);
        size += chimp.getPerSize();
    }

    public void compressParameter(double value) {
        rawBitSet = doubleToBitSet(value);
        if (Double.isNaN(value)) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        } else if (Double.isInfinite(value)) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        } else if (value == 0) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        } else if ((Double.doubleToLongBits(value) & DoubleConsts.EXP_BIT_MASK) == 0) {
            getSubNormalParameter(value);
            if (eraser_bits < 4) {
                flag = 0;
                result = Double.doubleToLongBits(value);
            } else {
                flag = 1;
            }
        } else {
            getNormalParameter(value);
            if (eraser_bits < 4) {
                flag = 0;
                result = Double.doubleToLongBits(value);
            } else {
                flag = 1;
                result_eraser = Double.doubleToLongBits(value) >>> eraser_bits;
                result = result_eraser << eraser_bits;
            }
        }
    }

    public void getNormalParameter(double value) {
        precision = getNumberMeaningDigits(value);
        exp = getExpValue(value);
        fn = computeFn(value);
        eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
    }

    public void getSubNormalParameter(double value) {
        exp = 1;
        fn = computeFn(value);
        eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
    }

    public double getDecompressedValue() {
        return Double.longBitsToDouble(result << eraser_bits);
    }

    public long getResult() {
        return result;
    }

    public int getEraser_bits() {
        return eraser_bits;
    }

    public byte[] getOut() {
        return out.getBuffer();
    }

    public int getSize() {
        return size;
    }

    public int[] getFlag(){
        return chimp.getFlag();
    }
}
