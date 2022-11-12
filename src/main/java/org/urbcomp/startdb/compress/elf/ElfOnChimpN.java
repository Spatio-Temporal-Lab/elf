package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.ChimpN;
import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.CompressorHelper;
import sun.misc.DoubleConsts;

import java.io.IOException;
import java.util.BitSet;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.*;

public class ElfOnChimpN {
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
    private ChimpN chimpN;

    public ElfOnChimpN(int previousValues) {
        out = new OutputBitStream(new byte[1000 * 8]);
        size = 0;
        chimpN = new ChimpN(previousValues,out, size);
    }
    public void addValue(double value) throws IOException {
        compressWithChimpN(value);
    }

    public void compressWithChimpN(double value) throws IOException {
        compressParameter(value);
        out.writeBit(flag);
        size += 1;
        if (flag != 0) {
            out.writeInt(precision, 4);
            size += 4;
        }
        chimpN.addValue(result);
        size += chimpN.getPerSize();
    }
    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    public void close() {
        out.writeBit(false);
        chimpN.close();
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

    public static void main(String[] args) throws IOException {
        BitSet a = doubleToBitSet(Double.NaN);
        System.out.println(CompressorHelper.bitSetToBinaryString(a));
        a.set(52, 63, false);
        System.out.println(CompressorHelper.bitSetToBinaryString(a));
        boolean b;
        double d = 13424.15241;
        System.out.println(CompressorHelper.bitSetToBinaryString(doubleToBitSet(d)));
        System.out.println(Double.doubleToLongBits(d) & DoubleConsts.EXP_BIT_MASK);
        System.out.println(getNumberMeaningDigits(d));
        ElfOnChimpN elf = new ElfOnChimpN(128);
        elf.compressParameter(d);
        System.out.println(Long.toBinaryString(elf.getResult()));
        System.out.println(Double.longBitsToDouble(elf.getResult() << elf.getEraser_bits()));
        System.out.println(elf.getDecompressedValue());
        double x = 0.15;
        double y = 0.154;
        elf.addValue(x);
        printByteArray(elf.getOut());
        System.out.println(elf.flag);
        System.out.println(elf.precision);
        System.out.println(Long.toBinaryString(elf.getResult()));
        elf.addValue(y);
        System.out.println(elf.flag);
        System.out.println(elf.precision);
        System.out.println(Long.toBinaryString(elf.getResult()));
        printByteArray(elf.getOut());


    }
}
