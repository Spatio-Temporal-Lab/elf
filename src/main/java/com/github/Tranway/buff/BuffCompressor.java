package com.github.Tranway.buff;

import gr.aueb.delorean.chimp.OutputBitStream;

import java.util.HashMap;
import java.util.Map;

public class BuffCompressor {
    private int size;
    private final OutputBitStream out;
    private long lowerBound;
    private long upperBound;
    private int maxPrec = 0;
    private int decWidth;
    private int intWidth;
    private int wholeWidth;
    private int columnCount;
    Map<Integer, Integer> PRECISION_MAP = new HashMap<>();
    Map<Integer, Long> LAST_MASK = new HashMap<>();
    private static int batchSize = 1000;

    public BuffCompressor() {
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
        PRECISION_MAP.put(13, 10);
        PRECISION_MAP.put(14, 10);
        PRECISION_MAP.put(15, 10);

        // init LAST_MASK
        LAST_MASK.put(1, 0b1L);
        LAST_MASK.put(2, 0b11L);
        LAST_MASK.put(3, 0b111L);
        LAST_MASK.put(4, 0b1111L);
        LAST_MASK.put(5, 0b11111L);
        LAST_MASK.put(6, 0b111111L);
        LAST_MASK.put(7, 0b1111111L);
        LAST_MASK.put(8, 0b11111111L);
        out = new OutputBitStream(new byte[10000]);
        size = 0;
    }

    public OutputBitStream getOutputStream() {
        return this.out;
    }

    public int compress(double[] values) {

    }

    // 获取小数位数
    public static int getDecimalPlace(double db) {
        if (db == 0.0) {
            return 0;
        }
        String str_db = String.valueOf(db);
        int indexOfDecimalPoint = str_db.indexOf('.');
        if (indexOfDecimalPoint >= 0) {
            return str_db.length() - indexOfDecimalPoint - 1;
        } else {
            return 0; // 没有小数点，小数位数为0
        }
    }

    public static int getWidthNeeded(long number) {
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

    public void headSample(double[] dbs) {
        lowerBound = Long.MAX_VALUE;
        upperBound = Long.MIN_VALUE;
        for (double db : dbs) {
            // double -> bits
            long bits = Double.doubleToLongBits(db);
            // bits -> string
            String binaryString = String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0');
            System.out.println("二进制表示：" + binaryString + "\n长度：" + binaryString.length());

            // get the sign
            long sign = bits >>> 63;
            System.out.println("sign:" + sign);

            // get the exp
            long exp_binary = bits >>> 52 & 0x7FF;
            System.out
                    .println("exp_binary:" + String.format("%11s", Long.toBinaryString(exp_binary)).replace(' ', '0'));
            long exp = exp_binary - 1023;
            System.out.println("exp:" + exp);

            // get the mantissa
            long mantissa = bits << 12 >>> 12;
            System.out.println("mantissa:" + String.format("%52s", Long.toBinaryString(mantissa)).replace(' ', '0'));

            // get the mantissa with implicit bit
            long implicit_mantissa = mantissa | (1L << 52);
            System.out.println("implicit_mantissa:"
                    + String.format("%53s", Long.toBinaryString(implicit_mantissa)).replace(' ', '0'));

            // get the precision
            int prec = getDecimalPlace(db);
            System.out.println("prec:" + prec);

            // update the max prec
            if (prec > maxPrec) {
                maxPrec = prec;
            }

            // get the int_len
            int int_len = ((int) exp + 1) > 0 ? ((int) exp + 1) : 0;
            System.out.println("int_len:" + int_len);

            // get the integer
            long integer = (52 - exp) > 63 ? 0 : (implicit_mantissa >>> (52 - exp));
            long integer_value = integer;
            if (sign != 0) {
                integer_value = -integer;
            }
            if (int_len != 0)
                System.out.println(
                        "integer:"
                                + String.format("%" + int_len + "s", Long.toBinaryString(integer)).replace(' ', '0'));
            else
                System.out.println("integer: null");
            // update the integer bound
            if (integer_value > upperBound) {
                upperBound = integer_value;
            }
            if (integer_value < lowerBound) {
                lowerBound = integer_value;
            }
        }

        System.out.println("--------HEAD SAMPLE RESULT--------begin");
        System.out.println("lower_bound:" + lowerBound);
        System.out.println("upper_bound:" + upperBound);
        System.out.println("max_prec:" + maxPrec);

        // get the int_width
        intWidth = getWidthNeeded(upperBound - lowerBound);
        System.out.println("int_width:" + intWidth);

        // get the dec_width
        decWidth = PRECISION_MAP.get(maxPrec);
        System.out.println("dec_width:" + decWidth);

        // get the whole_width
        wholeWidth = intWidth + decWidth;
        System.out.println("whole_width:" + wholeWidth);

        // get the col/bytes needed
        columnCount = wholeWidth / 8;
        if (wholeWidth % 8 != 0) {
            columnCount++;
        }
        System.out.println("columnCount:" + columnCount);
        System.out.println("--------HEAD SAMPLE RESULT--------end");
    }


    public byte[][] encode(double[] dbs) {
        byte[][] cols = new byte[columnCount][dbs.length]; // 第一维代表列号，第二维代表行号

        int db_cnt = 0;
        for (double db : dbs) {
            // double -> bits
            long bits = Double.doubleToLongBits(db);
            // bits -> string
            String binaryString = String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0');
            System.out.println("二进制表示：" + binaryString + "\n长度：" + binaryString.length());

            // get the sign
            long sign = bits >>> 63;
            System.out.println("sign:" + sign);

            // get the exp
            long exp_binary = bits >>> 52 & 0x7FF; // mask for the last 11 bits
            System.out
                    .println("exp_binary:" + String.format("%11s", Long.toBinaryString(exp_binary)).replace(' ', '0'));
            long exp = exp_binary - 1023;
            System.out.println("exp:" + exp);

            // get the mantissa
            long mantissa = bits << 12 >>> 12;
            System.out.println("mantissa:" + String.format("%52s", Long.toBinaryString(mantissa)).replace(' ', '0'));

            // get the mantissa with implicit bit
            long implicit_mantissa = mantissa | (1L << 52);
            System.out.println("implicit_mantissa:"
                    + String.format("%53s", Long.toBinaryString(implicit_mantissa)).replace(' ', '0'));

            // get the precision
            int prec = getDecimalPlace(db);
            System.out.println("prec:" + prec);

            // 以下改用dec_width
            // get the dec_len
            // int dec_len = PRECISION_MAP.get(prec);
            // System.out.println("dec_len:" + dec_len);

            // get the decimal
            // long decimal = mantissa << (12 + exp) >>> (12 + exp) >>> (64 - 12 - exp -
            // dec_len);

            // long decimal = mantissa << (12 + exp) >>> (64 - dec_len);
            // if (dec_len != 0)
            // System.out.println(
            // "decimal:"
            // + String.format("%" + dec_len + "s", Long.toBinaryString(decimal)).replace('
            // ', '0'));

            // long decimal = (12 + exp)>=0 ? (mantissa << (12 + exp) >>> (64 - dec_width) )
            // : (mantissa >>> Math.abs(12 + exp)>>> (64 - dec_width - Math.abs(12 + exp)));
            long decimal = (exp >= 0) ? (mantissa << (12 + exp) >>> (64 - decWidth))
                    : (implicit_mantissa >>> 53 - decWidth >>> (Math.abs(exp) - 1));
            if (decWidth != 0)
                System.out.println(
                        "decimal:"
                                + String.format("%" + decWidth + "s", Long.toBinaryString(decimal)).replace(' ', '0'));

            // get the int_len
            int int_len = ((int) exp + 1) > 0 ? ((int) exp + 1) : 0;
            System.out.println("int_len:" + int_len);

            // get the integer
            long integer = (52 - exp) > 63 ? 0 : (implicit_mantissa >>> (52 - exp));
            long integer_value = integer;
            if (sign != 0) {
                integer_value = -integer;
            }
            if (int_len != 0)
                System.out.println(
                        "integer:"
                                + String.format("%" + int_len + "s", Long.toBinaryString(integer)).replace(' ', '0'));
            else
                System.out.println("integer: null");

            // get the offset of integer
            long offset = integer_value - lowerBound;
            System.out.println(
                    "offset:" + String.format("%" + intWidth + "s", Long.toBinaryString(offset)).replace(' ', '0'));

            // get the bitpack result
            long bitpack = (offset << decWidth) | decimal;
            System.out.println("bitpack:"
                    + String.format("%" + wholeWidth + "s", Long.toBinaryString(bitpack)).replace(' ', '0'));

            // encode into cols[][]
            int remain = wholeWidth % 8;
            int bytes_cnt = 0;
            if (remain != 0) {
                bytes_cnt++;
                cols[columnCount - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK.get(remain));
                System.out.println((columnCount) - bytes_cnt + "/"
                        + String.format("%" + remain + "s", Long.toBinaryString((bitpack & LAST_MASK.get(remain))))
                        .replace(' ', '0'));
                bitpack = bitpack >>> remain;
            }
            while (bytes_cnt < columnCount) {
                bytes_cnt++;
                cols[columnCount - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK.get(8));
                System.out.println(String.format((columnCount - bytes_cnt) + "/" + "%" + 8 + "s",
                        Long.toBinaryString((bitpack & LAST_MASK.get(8)))).replace(' ', '0'));
                bitpack = bitpack >>> 8;
            }

            db_cnt++;
        }
        return cols;
    }

    public int sparseEncode(byte[][] cols) {
        SparseResult result;
        for (int j = 0; j < columnCount; ++j) {
            // 遍历每一列，查找频繁项
            result = findMajority(cols[j]);

            // col serilize
            if (result.flag == true) {
                out.writeBit(true);
                SparseResult.serialize();
            } else {
                // TODO write: flag = 0
                // TODO write: col[j]
            }
        }
    }

    public int serialize(SparseResult sr) {
        size += out.writeInt(sr.frequent_value, 8);
        for()
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
