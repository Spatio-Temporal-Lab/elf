package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.CompressorHelper;

import java.util.BitSet;

import static org.urbcomp.startdb.compress.elf.utils.CompressorHelper.*;

public class Elf {
    boolean sign;
    int offset;
    int flag;
    int meaning;
    BitSet rawBitSet;
    OutputBitStream out;
    public void addValue(double value) {
        //TODO
    }

    public void compress(double value) {
        //TODO

        if(Double.isNaN(value)){
        }
    }

    public boolean isValid(double value) {
        //TODO
        return true;
    }

    private void getSignBit(double value){
        sign = rawBitSet.get(63);
    }

    private void getParameter(double value) {
        rawBitSet = CompressorHelper.doubleToBitSet(value);
        offset = (int) CompressorHelper.getLong(rawBitSet, 52, 63) - 1023;
        if (offset < 0) {
            flag = 0;
            meaning = getMeaningDigits(value);
        } else {
            flag = 1;
            meaning = getNumberDecimalDigits(value);
        }
    }

    public static int getNumberDecimalDigits(double number) {
        if (number == (long) number) {
            return 0;
        }
        int i = 0;
        while (true) {
            i++;
            if (number * Math.pow(10, i) % 1 == 0) {
                return i;
            }
        }
    }

    public static int getMeaningDigits(double number) {
        if (number == (long) number) {
            return 0;
        }
        int i = 0;
        int j=0;
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
        System.out.println(bitSetToBinaryString(doubleToBitSet(0.25)));
        System.out.println(((Double.doubleToLongBits(0.23) >>> 52)));
        System.out.println(getNumberDecimalDigits(0.087));
        System.out.println(getMeaningDigits(0.078));
        System.out.println(getMeaningDigits(0.0000000001));
        System.out.println(getMeaningDigits(1.2131231));
        System.out.println(getMeaningDigits(0.111));
        System.out.println(getMeaningDigits(1.11101));
        System.out.println(getMeaningDigits(123.2345));
        System.out.println(CompressorHelper.doubleToBitSet(0.11).get(63));
        System.out.println(CompressorHelper.doubleToBitSet(-0.11).get(63));
        OutputBitStream out = new OutputBitStream(new byte[1000*8]);
        out.writeLong(25,6);
        System.out.println(out);
    }

}
