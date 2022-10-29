package org.urbcomp.startdb.compress.apes;

import org.urbcomp.startdb.compress.apes.utils.CompressorHelper;
import sun.misc.DoubleConsts;

import java.util.BitSet;

import static org.urbcomp.startdb.compress.apes.utils.CompressorHelper.doubleToBitSet;
import static org.urbcomp.startdb.compress.apes.utils.CompressorHelper.getLong;

public class apes {
    private final int EXPONENTIAL_DIGIT = 52;
    private final int SIGN_DIGIT = 63;
    private BitSet rawBitSet;
    private int flag;
    private int sign;
    private int offset;
    private int meaning;
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

    public void compressValue(double value) {
        rawBitSet = doubleToBitSet(value);
        if (Double.isNaN(value)) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        }
        if (Double.isInfinite(value)) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        }
        if (value == 0) {
            flag = 0;
            result = Double.doubleToLongBits(value);
        }
        if ((Double.doubleToLongBits(value)& DoubleConsts.EXP_BIT_MASK)==0){

        }

    }

    public void getParameter(double value) {

    }

    public static int getNumberDecimalDigits(double number) {
        if (number == (long) number) {
            return 0;
        }
        int i = 0;
        int j = 0;
        while (true) {
            i++;
            if (number * Math.pow(10, i) >= 1) {
                j++;
                if (number * Math.pow(10, i) % 1 == 0) {
                    return j;
                }

            }

        }
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
        double d= 1.012832363282407e-308;
        System.out.println(CompressorHelper.bitSetToBinaryString(doubleToBitSet(d)));
        System.out.println(Double.doubleToLongBits(d)& DoubleConsts.EXP_BIT_MASK);
        System.out.println(1);
        System.out.println(Double.isInfinite(d));
    }

}
