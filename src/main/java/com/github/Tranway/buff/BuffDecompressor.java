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

    private static int[] PRECISION_MAP = new int[]{
            0, 5, 8, 11, 15, 18, 21, 25, 28, 31, 35, 38, 50, 52, 52, 52, 52, 52, 52
    };
    private static long[] LAST_MASK = new long[]{
            0b1L, 0b11L, 0b111L, 0b1111L, 0b11111L, 0b111111L, 0b1111111L, 0b11111111L
    };

    public BuffDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public double[] decompress() throws IOException {
        lowerBound = in.readLong(64);
        batch_size = in.readInt(32);
        maxPrec = in.readInt(32);
        intWidth = in.readInt(32);
        decWidth = PRECISION_MAP[maxPrec];
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
                in.read(cols[j], batch_size * 8);
            } else {
                SparseResult result;
                result = deserialize();
                int index, offset, vec_cnt = 0;
                for (int i = 0; i < batch_size; i++) {
                    index = i / 8;
                    offset = i % 8;
                    if ((result.bitmap[index] & (1 << (7 - offset))) == 0) {
                        cols[j][i] = result.frequent_value;
                    } else {
                        cols[j][i] = result.outliers.get(vec_cnt);
                        vec_cnt++;
                    }
                }
            }
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
        double[] dbs = new double[batch_size];
        for (int i = 0; i < batch_size; i++) {
            // 逐行提取数据
            long bitpack = 0;
            int remain = wholeWidth % 8;
            if (remain == 0) {
                for (int j = 0; j < columnCount; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK[7]);
                }
            } else {
                for (int j = 0; j < columnCount - 1; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK[7]);
                }
                bitpack = (bitpack << remain) | (cols[columnCount - 1][i] & LAST_MASK[remain-1]);
            }

            // get the offset
            long offset = (intWidth != 0) ? (bitpack << 65 - wholeWidth >>> 64 - intWidth) : 0;

            // get the integer
            long integer = lowerBound + offset;

            // get the decimal
            long decimal = bitpack << (64 - decWidth) >>> (64 - decWidth);

            // modified decimal [used for - exp]
            long modified_decimal = decimal << (decWidth - get_width_needed(decimal));

            // get the mantissa with implicit bit
            long implicit_mantissa = (Math.abs(integer) << (53 - get_width_needed(Math.abs(integer))))
                    | (integer == 0 ? (modified_decimal << (53 - decWidth))
                    : (53 - decWidth - get_width_needed(Math.abs(integer))) >= 0
                    ? (decimal << (53 - decWidth - get_width_needed(Math.abs(integer))))
                    : (decimal >>> Math.abs(53 - decWidth - get_width_needed(Math.abs(integer)))));

            // get the mantissa
            long mantissa = implicit_mantissa & 0x000fffffffffffffL;

            // get the sign
            long sign = bitpack >>> (wholeWidth - 1);

            // get the exp
            long exp = integer != 0 ? (get_width_needed(Math.abs(integer)) + 1022)
                    : 1023 - (decWidth - get_width_needed(decimal) + 1);

            // get the origin bits in IEEE754
            long bits = (sign << 63) | (exp << 52) | mantissa;

            // get the origin value
            double db = Double.longBitsToDouble(bits);
            db = Double.parseDouble(String.format("%." + maxPrec + "f", db));
            dbs[i] = db;
        }
        return dbs;
    }
}
