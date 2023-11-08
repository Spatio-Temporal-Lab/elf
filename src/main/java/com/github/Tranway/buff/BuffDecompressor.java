package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BuffDecompressor {
    private final InputBitStream in;
    private int columnCount;
    private long lowerBound;
    private int batch_size;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private byte[][] cols;
    private Map<Integer, Long> LAST_MASK = new HashMap<>();
    private Map<Integer, Integer> PRECISION_MAP = new HashMap<>();

    public BuffDecompressor(byte[] bs) {
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
        LAST_MASK.put(1, 0b1L);
        LAST_MASK.put(2, 0b11L);
        LAST_MASK.put(3, 0b111L);
        LAST_MASK.put(4, 0b1111L);
        LAST_MASK.put(5, 0b11111L);
        LAST_MASK.put(6, 0b111111L);
        LAST_MASK.put(7, 0b1111111L);
        LAST_MASK.put(8, 0b11111111L);
    }

    public double[] decompress() throws IOException {
        lowerBound = in.readLong(64);
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
                System.out.println("FALSE");
                in.read(cols[j], batch_size * 8);
//                System.out.println("batch_size:" + batch_size);
//                System.out.println(Arrays.toString(cols[j]));
            } else {
                System.out.println("TURE");
                SparseResult result;
                result = deserialize();
                int index, offset, vec_cnt = 0;
                for (int i = 0; i < batch_size; i++) {
                    index = i / 8;
                    offset = i % 8;
                    if ((result.bitmap[index] & (1 << offset)) == 0) {
                        cols[j][i] = result.frequent_value;
                    } else {
                        cols[j][i] = result.outliers.get(vec_cnt);
                    }
                }
            }
        }
    }


    // 获取小数位数
    public static int get_decimal_place(String str_db) {
        if (Double.parseDouble(str_db) == 0.0) {
            return 0;
        }
        int indexOfDecimalPoint = str_db.indexOf('.');
        if (indexOfDecimalPoint >= 0) {
            return str_db.length() - indexOfDecimalPoint - 1;
        } else {
            return 0; // 没有小数点，小数位数为0
        }
    }

    public static int get_width_needed(long number) {
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


    public double[] merge_doubles() {
//        System.out.println("----------decode----------");
        // dbs = new double[cols[0].length];
        double[] dbs = new double[batch_size];
        for (int i = 0; i < batch_size; i++) {
            // 逐行提取数据
            long bitpack = 0;
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
//                    + String.format("%" + wholeWidth + "s", Long.toBinaryString(bitpack)).replace(' ', '0'));

            // get the offset
            long offset = (intWidth != 0) ? (bitpack << 65 - wholeWidth >>> 64 - intWidth) : 0;

//            if (intWidth != 0)
//                System.out.println(
//                        "offset:" + String.format("%" + intWidth + "s", Long.toBinaryString(offset)).replace(' ', '0'));
//            else
//                System.out.println("offset: null");

            // get the integer
            long integer = lowerBound + offset;
//            System.out.println("integer:" + integer);

            // get the decimal
            long decimal = bitpack << (64 - decWidth) >>> (64 - decWidth);
//            System.out.println("decimal:"
//                    + String.format("%" + decWidth + "s", Long.toBinaryString(decimal)).replace(' ', '0'));

            // modified decimal [used for - exp]
            long modified_decimal = decimal << (decWidth - get_width_needed(decimal));
//            System.out.println("modified_decimal:"
//                    + String.format("%" + decWidth + "s", Long.toBinaryString(modified_decimal)).replace(' ', '0'));

            // get the mantissa with implicit bit
            long implicit_mantissa = (Math.abs(integer) << (53 - get_width_needed(Math.abs(integer))))
                    | (integer == 0 ? (modified_decimal << (53 - decWidth - get_width_needed(Math.abs(integer))))
                    : (53 - decWidth - get_width_needed(Math.abs(integer))) >= 0
                    ? (decimal << (53 - decWidth - get_width_needed(Math.abs(integer))))
                    : (decimal >>> Math.abs(53 - decWidth - get_width_needed(Math.abs(integer)))));
//            System.out.println("implicit_mantissa:"
//                    + String.format("%53s", Long.toBinaryString(implicit_mantissa)).replace(' ', '0'));

            // get the mantissa
            long mantissa = implicit_mantissa & 0x000fffffffffffffL;
//            System.out.println("mantissa:"
//                    + String.format("%52s", Long.toBinaryString(mantissa)).replace(' ', '0'));

            // get the sign
            long sign = bitpack >>> (wholeWidth - 1);

            // get the exp
            long exp = integer != 0 ? (get_width_needed(Math.abs(integer)) + 1022)
                    : 1023 - (decWidth - get_width_needed(decimal) + 1);
//            System.out.println("exp:" + String.format("%11s", Long.toBinaryString(exp)).replace(' ', '0'));
//            System.out.println("exp_value:" + exp);

            // get the origin bits in IEEE754
            long bits = (sign << 63) | (exp << 52) | mantissa;
//            System.out.println("bits:" + String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0'));

            // get the origin value
            double db = Double.longBitsToDouble(bits);
            db = Double.parseDouble(String.format("%." + maxPrec + "f", db));
//            System.out.println("The Origin Value Is : " + String.format("%." + maxPrec + "f", db) + "\n----------");
            dbs[i] = db;
        }
        return dbs;
    }
}
