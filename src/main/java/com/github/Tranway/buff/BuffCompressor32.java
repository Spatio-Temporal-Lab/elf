package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.OutputBitStream;

import java.io.IOException;

public class BuffCompressor32 {
    private long size;
    private final OutputBitStream out;
    private int lowerBound;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private int columnCount;
    private static final int batchSize = 1000;

    private static final int[] PRECISION_MAP = new int[]{
            0, 5, 8, 11, 15, 18, 21, 25, 28, 31, 35, 38, 50, 52, 52, 52, 52, 52, 52
    };
    private static final int[] LAST_MASK = new int[]{
            0b1, 0b11, 0b111, 0b1111, 0b11111, 0b111111, 0b1111111, 0b11111111
    };

    public BuffCompressor32() {
        out = new OutputBitStream(new byte[100000]);
        size = 0;
    }

    public byte[] getOut() {
        return this.out.getBuffer();
    }

    public void compress(float[] values) {
        headSample(values);
        byte[][] cols = encode(values);
        size += out.writeInt(lowerBound, 32);
        size += out.writeInt(batchSize, 32);
        size += out.writeInt(maxPrec, 32);
        size += out.writeInt(intWidth, 32);
        if (wholeWidth >= 32) {
            wholeWidthLongCompress(values);
        } else {
            sparseEncode(cols);
        }
        close();
    }

    public void wholeWidthLongCompress(float[] values) {
        for (float value : values) {
            size += out.writeInt(Float.floatToIntBits(value), 32);
        }
    }

    public void close() {
        out.writeInt(0, 8);
    }

    private static int getWidthNeeded(int number) {
        if (number == 0) {
            return 0;
        }
        int bitCount = 0;
        while (number > 0) {
            bitCount++;
            number = number >>> 1;
        }
        return bitCount;
    }

    public long getSize() {
        return size;
    }

    public void headSample(float[] dbs) {
        lowerBound = Integer.MAX_VALUE;
        int upperBound = Integer.MIN_VALUE;
        for (float db : dbs) {
            // float -> bits
            int bits = Float.floatToIntBits(db);
            // bits -> string

            // get the sign
            int sign = bits >>> 31;

            // get the exp
            int expBinary = bits >>> 23 & 0xFF;
            int exp = expBinary - 127;

            // get the mantissa
            int mantissa = bits & 0x7FFFFF; // 0.11  1   -0.12  -1

            // get the mantissa with implicit bit
            int implicitMantissa = mantissa | (1 << 23);

            // get the precision
            int prec = getDecimalPlace(db);

            // update the max prec
            if (prec > maxPrec) {
                maxPrec = prec;
            }

            // get the integer
            int integer = exp < 0 ? 0 : (implicitMantissa >>> (23 - exp));
            int integerValue = (sign == 0) ? integer : -integer;

            // update the integer bound
            if (integerValue > upperBound) {
                upperBound = integerValue;
            }
            if (integerValue < lowerBound) {
                lowerBound = integerValue;
            }
        }

        // get the int_width
        intWidth = getWidthNeeded(upperBound - lowerBound);

        // get the dec_width
        decWidth = PRECISION_MAP[maxPrec];

        // get the whole_width
        wholeWidth = intWidth + decWidth + 1;

        // get the col/bytes needed
        columnCount = wholeWidth / 8;
        if (wholeWidth % 8 != 0) {
            columnCount++;
        }
    }

    public static int getDecimalPlace(float db) {
        if (db == 0.0) {
            return 0;
        }
        String strDb = Float.toString(db);
        int indexOfDecimalPoint = strDb.indexOf('.');
        int cnt = 0;

        if (indexOfDecimalPoint >= 0) {
            for (int i = indexOfDecimalPoint + 1; i < strDb.length(); ++i) {
                if (strDb.charAt(i) != 'E') {
                    cnt++;
                } else {
                    i++;
                    cnt -= Integer.parseInt(strDb.substring(i));
                    return Math.max(cnt, 0);
                }
            }
            return cnt;
        } else {
            return 0;
        }
    }


    public byte[][] encode(float[] dbs) {
        byte[][] cols = new byte[columnCount][dbs.length]; // 第一维代表列号，第二维代表行号

        int dbCnt = 0;
        for (float db : dbs) {
            // float -> bits
            int bits = Float.floatToIntBits(db);
            // bits -> string

            // get the sign
            int sign = bits >>> 31;

            // get the exp
            int expBinary = bits >>> 23 & 0xFF; // mask for the last 8 bits
            int exp = expBinary - 127;

            // get the mantissa
            int mantissa = bits & 0x7FFFFF; // 0.11  1   -0.12  -1

            // get the mantissa with implicit bit
            int implicitMantissa = mantissa | (1 << 23);

            int decimal;
            if (exp >= 0) {
                decimal = mantissa << (9 + exp) >>> (32 - decWidth);
            } else {
                if (24 - decWidth >= 0) {
                    decimal = implicitMantissa >>> 24 - decWidth >>> (-exp - 1);
//                    decimal = implicitMantissa >>> 23 - decWidth - exp;
                } else {
                    decimal = implicitMantissa << decWidth - 24 >>> (-exp - 1);
                }
            }

            // get the integer
            int integer = exp < 0 ? 0 : (implicitMantissa >>> (23 - exp));
            int integerValue = (sign == 0) ? integer : -integer;

            // get the offset of integer
            int offset = integerValue - lowerBound;

            // get the bitpack result
            long bitpack = ((long) sign) << (wholeWidth - 1) | ((long) offset << decWidth) | decimal;

            // encode into cols[][]
            int remain = wholeWidth % 8;
            int bytesCnt = 0;
            if (remain != 0) {
                bytesCnt++;
                cols[columnCount - bytesCnt][dbCnt] = (byte) (bitpack & LAST_MASK[remain - 1]);
                bitpack = bitpack >>> remain;
            }
            while (bytesCnt < columnCount) {
                bytesCnt++;
                cols[columnCount - bytesCnt][dbCnt] = (byte) (bitpack & LAST_MASK[7]);
                bitpack = bitpack >>> 8;
            }

            dbCnt++;
        }
        return cols;
    }


    public void sparseEncode(byte[][] cols) {
        SparseResult result;
        for (int j = 0; j < columnCount; ++j) {
            // 遍历每一列，查找频繁项
            result = findMajority(cols[j]);

            // col serilize
            if (result.flag) {
                size += out.writeBit(true);
                serialize(result);
            } else {
                size += out.writeBit(false);
                try {
                    size += out.write(cols[j], batchSize * 8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void serialize(SparseResult sr) {
        size += out.writeInt(sr.frequentValue, 8);
        try {
            size += out.write(sr.bitmap, batchSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < sr.outliersCnt; i++) {
            size += out.writeInt(sr.outliers[i], 8);
        }
    }


    public static SparseResult findMajority(byte[] nums) {
        SparseResult result = new SparseResult(batchSize);
        byte candidate = 0;
        int count = 0;

        for (byte num : nums) {
            if (count == 0) {
                candidate = num;
                count = 1;
            } else if (num == candidate) {
                count++;
            } else {
                count--;
            }
        }

        // 验证候选元素是否确实出现频率达到90%以上
        count = 0;
        for (int i = 0; i < nums.length; ++i) {
            int index = i / 8; // 当前行所处的byte下标
            result.bitmap[index] = (byte) (result.bitmap[index] << 1);
            if (nums[i] == candidate) {
                count++;
            } else {
                result.bitmap[index] = (byte) (result.bitmap[index] | 0b1);
                result.outliers[result.outliersCnt++] = nums[i];
            }
        }

        if (count >= nums.length * 0.9) {
            result.flag = true;
            result.frequentValue = candidate;
        } else {
            result.flag = false;
        }
        return result;
    }
}
