package org.urbcomp.startdb.compress.apes;

import org.urbcomp.startdb.compress.apes.utils.CompressorHelper;
import sun.misc.DoubleConsts;

import java.util.BitSet;

import static org.urbcomp.startdb.compress.apes.utils.CompressorHelper.*;

public class Elf {
    private final int EXPONENTIAL_DIGIT = 52;
    private final int SIGN_DIGIT = 63;
    private BitSet rawBitSet;
    private int flag;
    private int sign;
    private int exp;
    private int fn;
    private int eraser_bits;
    private int meaning;
    private int precision;
    private long result;

    public void addValue(double value) {
        //TODO
    }

    public void compress(double value) {
        //TODO
    }

    public boolean isValid(double value) {
        //TODO
        return true;
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
            eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
            if (eraser_bits < 4) {
                flag = 0;
                result = Double.doubleToLongBits(value);
            }else {
                flag = 1;
            }
        } else {
            getNormalParameter(value);
            eraser_bits = EXPONENTIAL_DIGIT - (exp - 1023 + fn);
            if (eraser_bits < 4) {
                flag = 0;
                result = Double.doubleToLongBits(value);
            } else {
                flag = 1;
                result = Double.doubleToLongBits(value)>>>eraser_bits;
            }
        }
    }

    public void getNormalParameter(double value) {
        exp = getExpValue(value);
        fn = computeFn(value);
    }

    public void getSubNormalParameter(double value) {
        exp = 1;
        fn = computeFn(value);
    }


    public static void main(String[] args) {
        System.out.println(getNumberDecimalDigits(0.23123));
        System.out.println(getNumberDecimalDigits(1234.1231));
        System.out.println(getNumberDecimalDigits(0.000012434));
        System.out.println(Double.doubleToRawLongBits(Double.NaN));
        System.out.println(Double.doubleToRawLongBits(Double.NaN));
        System.out.println(Double.doubleToRawLongBits(0.123d / 0.0d));
        System.out.println(CompressorHelper.bitSetToBinaryString(doubleToBitSet(-0.0)));
        BitSet a = doubleToBitSet(Double.NaN);
        System.out.println(CompressorHelper.bitSetToBinaryString(a));
        a.set(52, 63, false);
        System.out.println(CompressorHelper.bitSetToBinaryString(a));
        boolean b;
        double d = 0.15;
        System.out.println(CompressorHelper.bitSetToBinaryString(doubleToBitSet(d)));
        System.out.println(Double.doubleToLongBits(d) & DoubleConsts.EXP_BIT_MASK);
        System.out.println(1);
        System.out.println(Double.isInfinite(d));
        System.out.println(getPrecision(d));
        System.out.println(getPrecision1(d));
        System.out.println(getNumberDecimalDigits(d));
        System.out.println(getNumberMeaningDigits(d));
        Elf elf = new Elf();
        elf.compressParameter(d);
    }

}