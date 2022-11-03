package org.urbcomp.startdb.compress.apes.utils;

import sun.misc.DoubleConsts;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoField;
import java.util.BitSet;

public class CompressorHelper {

    /**
     * 将long转化成BitSet类型
     *
     * @param number long数字
     * @return 转化后的BitSet
     */
    public static BitSet longToBitSet(long number) {
        return BitSet.valueOf(new long[]{number});
    }

    /**
     * {@code bitSetOut}从{@code start}位置开始设置{@code length}长度为{@code bitSetIn}
     *
     * @param bitSetOut 将会被修改的bitSet
     * @param bitSetIn  参照的bitSet
     * @param start     开始设置的位置
     * @param length    设置的长度
     * @return 下一个开始的位置
     */
    public static int setBitSet(BitSet bitSetOut, BitSet bitSetIn, int start, int length) {
        setBitSet(bitSetOut, bitSetIn.get(0, length), start);
        start += length;
        return start;
    }

    /**
     * {@code bitSetOut}从{@code start}位置开始设置为{@code bitSetIn}
     *
     * @param bitSetOut 将会被修改的bitSet
     * @param bitSetIn  参照的bitSet
     * @param start     开始设置的位置
     */
    public static int setBitSet(BitSet bitSetOut, BitSet bitSetIn, int start) {
        for (int i = 0; i < bitSetIn.length(); i++) {
            bitSetOut.set(start + i, bitSetIn.get(i));
        }
        return start + bitSetIn.length();
    }

    /**
     * 获得从{@code fromIndex}位置到{@code bitSetIn}结束的{@code bitSet}对应的long值
     *
     * @param bitSet    目标BitSet
     * @param fromIndex 起始位置
     * @param endIndex  结束位置
     * @return 返回对应的long值
     */
    public static long getLong(BitSet bitSet, int fromIndex, int endIndex) {
        long[] resultLongArr = bitSet.get(fromIndex, endIndex).toLongArray();
        return resultLongArr.length != 0 ? resultLongArr[0] : 0L;
    }

    /**
     * 获得从{@code fromIndex}位置到{@code bitSetIn}结束的{@code bitSet}对应的Double值
     *
     * @param bitSet    目标BitSet
     * @param fromIndex 起始位置
     * @param endIndex  结束位置
     * @return 返回对应的Double值
     */
    public static Double getDouble(BitSet bitSet, int fromIndex, int endIndex) {
        long[] resultLongArr = bitSet.get(fromIndex, endIndex).toLongArray();
        return resultLongArr.length != 0 ? Double.longBitsToDouble(resultLongArr[0]) : 0;
    }

    /**
     * 将double转化成BitSet类型
     *
     * @param number double数据
     * @return 转化后的BitSet
     */
    public static BitSet doubleToBitSet(double number) {
        return longToBitSet(Double.doubleToLongBits(number));
    }

    /**
     * 将BitSet转化成二进制字符串，便于可视化
     *
     * @param bitset bitSet变量
     * @return 字符串二进制表示
     */
    public static String bitSetToBinaryString(BitSet bitset) {
        StringBuilder sb = new StringBuilder();
        for (int i = bitset.length() - 1; i >= 0; i--) {
            sb.append(bitset.get(i) ? "1" : "0");
        }
        return sb.toString();
    }

    /**
     * 在bitset位数不到64位的请情况下转为long为正数，修正位有符号数
     *
     * @param unSignLong 原始无符号数
     * @param digit      bit能够表示的范围
     * @return 修正后的有符号数
     */
    public static long getSignLong(long unSignLong, int digit) {
        long range = (long) Math.pow(2, digit);
        if (unSignLong > range / 2) {
            return unSignLong - range;
        } else {
            return unSignLong;
        }
    }

    /**
     * 将时间字符串按照pattern格式转位时间戳
     *
     * @param StrTime 字符串时间
     * @param pattern 格式
     * @return long的时间戳
     */
    public static long stringToTimeStamp(String StrTime, String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        try {
            return df.parse(StrTime).toInstant().getLong(ChronoField.INSTANT_SECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getPrecision(double d) {
        String moneyStr = String.valueOf(d);
        String[] num = moneyStr.split("\\.");
        if (num.length == 2) {
            return num[1].length();
        } else {
            return 0;
        }
    }

    public static int getPrecision1(double d) {
        BigDecimal bd = new BigDecimal(String.valueOf(d));
        String[] ss = bd.toString().split("\\.");
        if (ss.length <= 1){
            return 0;
        }
        return ss[1].length();
    }

    public static long getExpBits(double value) {
        return Double.doubleToRawLongBits(value) & DoubleConsts.EXP_BIT_MASK;
    }

    public static int getExpValue(double value) {
        return (int) (getExpBits(value) >>> DoubleConsts.SIGNIFICAND_WIDTH - 1);
    }

    public static long getSignIfBits(double value) {
        return Double.doubleToRawLongBits(value) & DoubleConsts.SIGNIF_BIT_MASK;
    }

    public static void printLongOfBinary(long value) {
        System.out.println(Long.toBinaryString(value));
    }

    public static int computeFn(double value){
        return computeFn(getNumberDecimalDigits(value));
    }

    public static int computeFn(int precision) {
        return (int) Math.ceil(precision * Math.log(10) / Math.log(2));
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

    public static int getNumberMeaningDigits(double number) {
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

    public static void printByteArray(byte[] bytes){
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<bytes.length;i++){
            sb.append(Long.toBinaryString(Byte.toUnsignedLong(bytes[i])));
        }
        System.out.println(sb.toString());
    }

    public static void main(String[] args) {
        System.out.println(getSignIfBits(0.02));
        printLongOfBinary(Double.doubleToLongBits(0.02));
        printLongOfBinary(getSignIfBits(0.02));
        System.out.println(computeFn(2));
    }

}
