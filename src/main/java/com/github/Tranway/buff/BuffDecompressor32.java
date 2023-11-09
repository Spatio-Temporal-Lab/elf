package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BuffDecompressor32 {
    private final InputBitStream in;
    private int columnCount;
    private int lowerBound;
    private int batch_size;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private byte[][] cols;
    private Map<Integer, Integer> LAST_MASK = new HashMap<>();
    private Map<Integer, Integer> PRECISION_MAP = new HashMap<>();

    public BuffDecompressor32(byte[] bs) {
        in = new InputBitStream(bs);

        PRECISION_MAP.put(0, 0);
        PRECISION_MAP.put(1, 5);
        PRECISION_MAP.put(2, 8);
        PRECISION_MAP.put(3, 11);
        PRECISION_MAP.put(4, 15);
        PRECISION_MAP.put(5, 18);
        PRECISION_MAP.put(6, 21);
        PRECISION_MAP.put(7, 25);
        PRECISION_MAP.put(8, 28);
        PRECISION_MAP.put(9, 31);
        PRECISION_MAP.put(10, 35);
        PRECISION_MAP.put(11, 38);
        PRECISION_MAP.put(12, 50);
        PRECISION_MAP.put(13, 52);
        PRECISION_MAP.put(14, 52);
        PRECISION_MAP.put(15, 52);
        PRECISION_MAP.put(16, 52);
        PRECISION_MAP.put(17, 52);
        PRECISION_MAP.put(18, 52);

        // init LAST_MASK
        LAST_MASK.put(1, 0b1);
        LAST_MASK.put(2, 0b11);
        LAST_MASK.put(3, 0b111);
        LAST_MASK.put(4, 0b1111);
        LAST_MASK.put(5, 0b11111);
        LAST_MASK.put(6, 0b111111);
        LAST_MASK.put(7, 0b1111111);
        LAST_MASK.put(8, 0b11111111);
    }

    public float[] decompress() throws IOException {
        lowerBound = in.readInt(32);
        batch_size = in.readInt(32);
        maxPrec = in.readInt(32);
        intWidth = in.readInt(32);
        decWidth = PRECISION_MAP.get(maxPrec);
        wholeWidth = decWidth + intWidth + 1;
        columnCount = wholeWidth / 8;
        if (wholeWidth % 8 != 0) {
            columnCount++;
        }
//        System.out.println("intWidth" + intWidth);
        cols = new byte[columnCount][batch_size];
        sparseDecode();
        return merge_doubles();
    }

    public SparseResult deserialize() throws IOException {
        SparseResult result = new SparseResult(batch_size);
        result.setFrequent_value(in.readInt(8));
        in.read(result.bitmap, batch_size);
//        System.out.println("deserialize");
//        System.out.println(Arrays.toString(result.bitmap));
        int count = 0;
        for (byte b : result.bitmap) {
            for (int i = 0; i < 8; i++) {
                count += (b >> i) & 1;
            }
        }
        for (int i = 0; i < count; i++) {
            result.getOutliers().add((byte) in.readInt(8));
        }

        return result;
    }

    public void sparseDecode() throws IOException {
        for (int j = 0; j < columnCount; ++j) {
            if (in.readBit() == 0) {
//                System.out.println("FALSE");
                in.read(cols[j], batch_size * 8);
//                System.out.println("batch_size:" + batch_size);
//                System.out.println(Arrays.toString(cols[j]));
            } else {
//                System.out.println("TURE");
                SparseResult result;
                result = deserialize();
                int index, offset, vec_cnt = 0;
                for (int i = 0; i < batch_size; i++) {
                    index = i / 8;
                    offset = i % 8;
                    if ((result.bitmap[index] & (1 << (7 - offset))) == 0) {
                        cols[j][i] = result.frequent_value;
                    } else {

//                        System.out.println("i: " + i);
                        cols[j][i] = result.outliers.get(vec_cnt);
                        vec_cnt++;
                    }
                }
            }
        }
    }


    public static int get_width_needed(int number) {
        if (number == 0) {
            return 0; // 约定0不需要位宽
        }

        int bitCount = 0;
        while (number > 0) {
            bitCount++;
            number = number >> 1; // 右移一位
        }

        return bitCount;
    }


    public float[] merge_doubles() {
//        System.out.println("----------decode----------");
        // dbs = new int[cols[0].length];
        float[] dbs = new float[batch_size];
        for (int i = 0; i < batch_size; i++) {
            // 逐行提取数据
            int bitpack = 0;
            int remain = wholeWidth % 8;
            if (remain == 0) {
                for (int j = 0; j < columnCount; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK.get(8));
                }
            } else {
                for (int j = 0; j < columnCount - 1; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK.get(8));
                }
                bitpack = (bitpack << remain) | (cols[columnCount - 1][i] & LAST_MASK.get(remain));
            }
//            System.out.println("bitpack:"
//                    + String.format("%" + wholeWidth + "s", Integer.toBinaryString(bitpack)).replace(' ', '0'));

            // get the offset
            int offset = (intWidth != 0) ? (bitpack << 33 - wholeWidth >>> 32 - intWidth) : 0;

//            if (intWidth != 0)
//                System.out.println(
//                        "offset:" + String.format("%" + intWidth + "s", Integer.toBinaryString(offset)).replace(' ', '0'));
//            else
//                System.out.println("offset: null");

            // get the integer
            int integer = lowerBound + offset;
//            System.out.println("integer:" + integer);

            // get the decimal
            int decimal = bitpack << (32 - decWidth) >>> (32 - decWidth);
//            System.out.println("decimal:"
//                    + String.format("%" + decWidth + "s", Integer.toBinaryString(decimal)).replace(' ', '0'));

            // modified decimal [used for - exp]
            int modified_decimal = decimal << (decWidth - get_width_needed(decimal));
//            System.out.println("modified_decimal:"
//                    + String.format("%" + decWidth + "s", Integer.toBinaryString(modified_decimal)).replace(' ', '0'));

            // get the mantissa with implicit bit
            int implicit_mantissa = (Math.abs(integer) << (24 - get_width_needed(Math.abs(integer))))
                    | (integer == 0 ? (modified_decimal << (24 - decWidth - get_width_needed(Math.abs(integer))))
                    : (24 - decWidth - get_width_needed(Math.abs(integer))) >= 0
                    ? (decimal << (24 - decWidth - get_width_needed(Math.abs(integer))))
                    : (decimal >>> Math.abs(24 - decWidth - get_width_needed(Math.abs(integer)))));
//            System.out.println("implicit_mantissa:"
//                    + String.format("%24s", Integer.toBinaryString(implicit_mantissa)).replace(' ', '0'));

            // get the mantissa
            int mantissa = implicit_mantissa & 0x7FFFFF;
//            System.out.println("mantissa:"
//                    + String.format("%23s", Integer.toBinaryString(mantissa)).replace(' ', '0'));

            // get the sign
            int sign = bitpack >>> (wholeWidth - 1);

            // get the exp
            int exp = integer != 0 ? (get_width_needed(Math.abs(integer)) + 126)
                    : 127 - (decWidth - get_width_needed(decimal) + 1);
//            System.out.println("exp:" + String.format("%8s", Integer.toBinaryString(exp)).replace(' ', '0'));
//            System.out.println("exp_value:" + exp);

            // get the origin bits in IEEE754
            int bits = (sign << 31) | (exp << 23) | mantissa;
//            System.out.println("bits:" + String.format("%32s", Integer.toBinaryString(bits)).replace(' ', '0'));

            // get the origin value
            float db = Float.intBitsToFloat(bits);
            db = Float.parseFloat(String.format("%." + maxPrec + "f", db));
//            System.out.println("The Origin Value Is : " + String.format("%." + maxPrec + "f", db) + "\n----------");
            dbs[i] = db;
        }
        return dbs;
    }
}
