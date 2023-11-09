package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.InputBitStream;
import gr.aueb.delorean.chimp.OutputBitStream;
import org.urbcomp.startdb.compress.elf.utils.Elf64Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BuffCompressor {
    private long size;
    private final OutputBitStream out;
    private long lowerBound;
    private long upperBound;
    private int maxPrec;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private int columnCount;
    //    Map<Integer, Integer> PRECISION_MAP = new HashMap<>();
//    private Map<Integer, Long> LAST_MASK = new HashMap<>();
    private static final int batchSize = 1000;

    private static int[] PRECISION_MAP = new int[]{
            0, 5, 8, 11, 15, 18, 21, 25, 28, 31, 35, 38, 50, 52, 52, 52, 52, 52, 52
    };
    private static long[] LAST_MASK = new long[]{
            0b1L, 0b11L, 0b111L, 0b1111L, 0b11111L, 0b111111L, 0b1111111L, 0b11111111L
    };

    public BuffCompressor() {

        out = new OutputBitStream(new byte[100000]);
        size = 0;
    }

    public byte[] getOut() {
        return this.out.getBuffer();
    }

    public void compress(double[] values) {
        headSample(values);
        byte[][] cols = encode(values);
        size += out.writeLong(lowerBound, 64);
        size += out.writeInt(batchSize, 32);
        size += out.writeInt(maxPrec, 32);
        size += out.writeInt(intWidth, 32);
        sparseEncode(cols);
        close();
    }

    public void close() {
        out.writeInt(0, 8);
    }

    private static int getWidthNeeded(long number) {
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

    public long getSize() {
        return size;
    }

    public void headSample(double[] dbs) {
        lowerBound = Long.MAX_VALUE;
        upperBound = Long.MIN_VALUE;
        for (double db : dbs) {
            // double -> bits
            long bits = Double.doubleToLongBits(db);
            long sign = bits >>> 63;
            // get the exp
            long exp_binary = bits >>> 52 & 0x7FF;
            long exp = exp_binary - 1023;
            // get the mantissa
            long mantissa = bits & 0x000fffffffffffffL; // 0.11  1   -0.12  -1

            // get the mantissa with implicit bit
            long implicit_mantissa = mantissa | (1L << 52);

            // get the precision
            int prec = get_decimal_place(db);

            // update the max prec
            if (prec > maxPrec) {
                maxPrec = prec;
            }

            // get the integer
            long integer = (52 - exp) > 52 ? 0 : (implicit_mantissa >>> (52 - exp));
            long integer_value = (sign == 0) ? integer : -integer;

            if (integer_value > upperBound) {
                upperBound = integer_value;
            }
            if (integer_value < lowerBound) {
                lowerBound = integer_value;
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

    public static int get_decimal_place(double db) {
        if (db == 0.0) {
            return 0;
        }
        String strDb = Double.toString(db);
        int indexOfDecimalPoint = strDb.indexOf('.');
        int cnt = 0;

        if (indexOfDecimalPoint >= 0) {
            for (int i = indexOfDecimalPoint + 1; i < strDb.length(); ++i) {
                if (strDb.charAt(i) != 'E') {
                    cnt++;
                } else {
                    i ++;
                    cnt -= Integer.parseInt(strDb.substring(i));
                    return cnt;
                }
            }
            return cnt;
        } else {
            return 0; // 没有小数点，小数位数为0
        }
    }


    public byte[][] encode(double[] dbs) {
        byte[][] cols = new byte[columnCount][dbs.length]; // 第一维代表列号，第二维代表行号

        int db_cnt = 0;
        for (double db : dbs) {
            // double -> bits
            long bits = Double.doubleToLongBits(db);
            // bits -> string

            // get the sign
            long sign = bits >>> 63;

            // get the exp
            long exp_binary = bits >>> 52 & 0x7FF; // mask for the last 11 bits
            long exp = exp_binary - 1023;

            // get the mantissa
            long mantissa = bits & 0x000fffffffffffffL; // 0.11  1   -0.12  -1

            // get the mantissa with implicit bit
            long implicit_mantissa = mantissa | (1L << 52);

            long decimal = (exp >= 0) ? (mantissa << (12 + exp) >>> (64 - decWidth))
                    : (implicit_mantissa >>> 53 - decWidth >>> (Math.abs(exp) - 1));

            // get the integer
            long integer = (52 - exp) > 52 ? 0 : (implicit_mantissa >>> (52 - exp));
            long integer_value = (sign == 0) ? integer : -integer;

            // get the offset of integer
            long offset = integer_value - lowerBound;

            // get the bitpack result
            long bitpack = sign << (wholeWidth - 1) | (offset << decWidth) | decimal;


            // encode into cols[][]
            int remain = wholeWidth % 8;
            int bytes_cnt = 0;
            if (remain != 0) {
                bytes_cnt++;
                cols[columnCount - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK[remain-1]);
                bitpack = bitpack >>> remain;
            }
            while (bytes_cnt < columnCount) {
                bytes_cnt++;
                cols[columnCount - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK[7]);
                bitpack = bitpack >>> 8;
            }

            db_cnt++;
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
//                System.out.println(Arrays.toString(testOut.getBuffer()));
            }
        }
    }

    private void serialize(SparseResult sr) {
        size += out.writeInt(sr.frequent_value, 8);
        try {
//            System.out.println("serialize");
//
//            System.out.println(Arrays.toString(sr.bitmap));
            size += out.write(sr.bitmap, batchSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < sr.outliers.size(); i++) {
            size += out.writeInt(sr.outliers.get(i).intValue(), 8);
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
//                System.out.println("is: "+ i);
                result.bitmap[index] = (byte) (result.bitmap[index] | 0b1);
                result.outliers.add(nums[i]);
            }
        }

        if (count >= nums.length * 0.9) {
            result.flag = true;
            result.frequent_value = candidate;
        } else {
            result.flag = false;
            // result.frequent_value = 0;
        }
        return result;
    }
}
