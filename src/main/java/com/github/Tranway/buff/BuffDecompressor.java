package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.InputBitStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BuffDecompressor {
    private final InputBitStream in;
    private int columnCount;
    private long lowerBound;
    private int batchSize;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private byte[][] cols;

    private static final int[] PRECISION_MAP = new int[]{
            0, 5, 8, 11, 15, 18, 21, 25, 28, 31, 35, 38, 50, 52, 52, 52, 64, 64, 64, 64, 64, 64, 64
    };
    private static final long[] LAST_MASK = new long[]{
            0b1L, 0b11L, 0b111L, 0b1111L, 0b11111L, 0b111111L, 0b1111111L, 0b11111111L
    };

    public BuffDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public double[] decompress() throws IOException {
        lowerBound = in.readLong(64);
        batchSize = in.readInt(32);
        maxPrec = in.readInt(32);
        intWidth = in.readInt(32);
        decWidth = PRECISION_MAP[maxPrec];
        wholeWidth = decWidth + intWidth + 1;
        if (wholeWidth >= 64) {
            double[] result = new double[batchSize];
            for (int i = 0; i < batchSize; i++) {
                result[i] = Double.longBitsToDouble(in.readLong(64));
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


    public static int getWidthNeeded(long number) {
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


    public double[] mergeDoubles() {
        double[] dbs = new double[batchSize];
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
            long offset = (intWidth != 0) ? (bitpack << 65 - wholeWidth >>> 64 - intWidth) : 0;

            // get the integer
            long integer = lowerBound + offset;

            // get the decimal
            long decimal = bitpack << (64 - decWidth) >>> (64 - decWidth);

            // modified decimal [used for - exp]
            long modifiedDecimal = decimal << (decWidth - getWidthNeeded(decimal));

            // get the exp
            long exp = integer != 0 ? (getWidthNeeded(Math.abs(integer)) + 1022)
                    : 1023 - (decWidth - getWidthNeeded(decimal) + 1);
            long expValue = exp - 1023;

            // get the mantissa with implicit bit
            int tmp = 53 - decWidth - getWidthNeeded(Math.abs(integer));

            long implicitMantissa = (Math.abs(integer) << tmp + decWidth)
                    | (expValue < 0 ? (tmp >= 0 ? (modifiedDecimal << tmp) : (modifiedDecimal >>> Math.abs(tmp)))
                    : tmp >= 0
                    ? (decimal << (tmp))
                    : (decimal >>> Math.abs(tmp)));

            // get the mantissa
            long mantissa = implicitMantissa & 0x000fffffffffffffL;

            // get the sign
            long sign = bitpack >>> (wholeWidth - 1);

            // get the origin bits in IEEE754
            long bits = (sign << 63) | (exp << 52) | mantissa;

            // get the origin value
            double db = Double.longBitsToDouble(bits);

            BigDecimal bd = new BigDecimal(db);
            db = bd.setScale(maxPrec, RoundingMode.HALF_UP).doubleValue();
            if (db == 0 && sign == 1) db = -db;
            dbs[i] = db;
        }
        return dbs;
    }
}
