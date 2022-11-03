package org.urbcomp.startdb.compress.elf.utils;

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
        return start+bitSetIn.length();
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
        for (int i = 0; i < bitset.length(); i++) {
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

}
