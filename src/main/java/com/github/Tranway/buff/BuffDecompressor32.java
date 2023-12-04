package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BuffDecompressor32 {
    private final InputBitStream in;
    private int columnCount;
    private int lowerBound;
    private int batchSize;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private byte[][] cols;

    private static final int[] PRECISION_MAP = new int[]{
            0, 5, 8, 11, 15, 18, 21, 25, 28, 31, 35, 38, 50, 52, 52, 52, 52, 52, 52
    };
    private static final int[] LAST_MASK = new int[]{
            0b1, 0b11, 0b111, 0b1111, 0b11111, 0b111111, 0b1111111, 0b11111111
    };

    public BuffDecompressor32(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public float[] decompress() throws IOException {
        lowerBound = in.readInt(32);
        batchSize = in.readInt(32);
        maxPrec = in.readInt(32);
        intWidth = in.readInt(32);
        decWidth = PRECISION_MAP[maxPrec];
        wholeWidth = decWidth + intWidth + 1;
        if (wholeWidth >= 32) {
            float[] result = new float[batchSize];
            for (int i = 0; i < batchSize; i++) {
                result[i] = Float.intBitsToFloat(in.readInt(32));
            }
            return result;
        }
        columnCount = wholeWidth / 8;
        if (wholeWidth % 8 != 0) {
            columnCount++;
        }
        cols = new byte[columnCount][batchSize];
        sparseDecode();
        return mergeDoubles();
    }

    public SparseResult deserialize() throws IOException {
        SparseResult result = new SparseResult(batchSize);
        result.setFrequentValue(in.readInt(8));
        in.read(result.bitmap, batchSize);
        int count = 0;
        for (byte b : result.bitmap) {
            for (int i = 0; i < 8; i++) {
                count += (b >> i) & 1;
            }
        }
        for (int i = 0; i < count; i++) {
            result.getOutliers()[i] = (byte) in.readInt(8);
        }

        return result;
    }

    public void sparseDecode() throws IOException {
        for (int j = 0; j < columnCount; ++j) {
            if (in.readBit() == 0) {
                in.read(cols[j], batchSize * 8);
            } else {
                SparseResult result;
                result = deserialize();
                int index, offset, vecCnt = 0;
                for (int i = 0; i < batchSize; i++) {
                    index = i / 8;
                    offset = i % 8;
                    if ((result.bitmap[index] & (1 << (7 - offset))) == 0) {
                        cols[j][i] = result.frequentValue;
                    } else {
                        cols[j][i] = result.outliers[vecCnt++];
                    }
                }
            }
        }
    }


    public static int getWidthNeeded(int number) {
        if (number == 0) {
            return 0; // 约定0不需要位宽
        }

        int bitCount = 0;
        while (number > 0) {
            bitCount++;
            number = number >>> 1; // 右移一位
        }

        return bitCount;
    }


    public float[] mergeDoubles() {
        float[] dbs = new float[batchSize];
        for (int i = 0; i < batchSize; i++) {
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
                bitpack = (bitpack << remain) | (cols[columnCount - 1][i] & LAST_MASK[remain - 1]);
            }

            // get the offset
            int offset = (intWidth != 0) ? (int) (bitpack << 65 - wholeWidth >>> 64 - intWidth) : 0;


            // get the integer
            int integer = lowerBound + offset;

            // get the decimal
            int decimal = (int) (bitpack << (64 - decWidth) >>> (64 - decWidth));

            // modified decimal [used for - exp]
            int modifiedDecimal = decimal << (decWidth - getWidthNeeded(decimal));

            // get the mantissa with implicit bit
            int tmp = 24 - decWidth - getWidthNeeded(Math.abs(integer));

            int implicitMantissa = (Math.abs(integer) << tmp + decWidth)
                    | (integer == 0 ? tmp >= 0 ? (modifiedDecimal << tmp) : (modifiedDecimal >>> Math.abs(tmp))
                    : tmp >= 0
                    ? (decimal << (tmp))
                    : (decimal >>> Math.abs(tmp)));

            // get the mantissa
            int mantissa = implicitMantissa & 0x7FFFFF;

            // get the sign
            int sign = (int) (bitpack >>> (wholeWidth - 1));

            // get the exp
            int exp = integer != 0 ? (getWidthNeeded(Math.abs(integer)) + 126)
                    : 127 - (decWidth - getWidthNeeded(decimal) + 1);

            // get the origin bits in IEEE754
            int bits = (sign << 31) | (exp << 23) | mantissa;

            // get the origin value
            float db = Float.intBitsToFloat(bits);

            BigDecimal bd = new BigDecimal(db);
            db = bd.setScale(maxPrec, RoundingMode.HALF_UP).floatValue();
            if (db == 0 && sign == 1) db = -db;
            dbs[i] = db;
        }
        return dbs;
    }
}
