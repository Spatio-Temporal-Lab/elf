package com.github.Tranway.buff;

import java.util.HashMap;
import java.util.Map;

public class Buff {
    Map<Integer, Integer> PRECISION_MAP = new HashMap<>();
    Map<Integer, Long> LAST_MASK = new HashMap<>();
    long lower_bound, upper_bound;
    int int_width, dec_width, whole_width;
    int col_cnt;
    int max_prec = 0;
    int batch_size = 1000;

    public Buff() {
        // init PRECISION_MAP
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

    public void head_sample(String[] str_dbs) {
        lower_bound = Long.MAX_VALUE;
        upper_bound = Long.MIN_VALUE;
        for (String str_db : str_dbs) {
            double db = Double.parseDouble(str_db);
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
            int prec = get_decimal_place(str_db);
            System.out.println("prec:" + prec);

            // update the max prec
            if (prec > max_prec) {
                max_prec = prec;
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
            if (integer_value > upper_bound) {
                upper_bound = integer_value;
            }
            if (integer_value < lower_bound) {
                lower_bound = integer_value;
            }
        }

        System.out.println("--------HEAD SAMPLE RESULT--------begin");
        System.out.println("lower_bound:" + lower_bound);
        System.out.println("upper_bound:" + upper_bound);
        System.out.println("max_prec:" + max_prec);

        // get the int_width
        int_width = get_width_needed(upper_bound - lower_bound);
        System.out.println("int_width:" + int_width);

        // get the dec_width
        dec_width = PRECISION_MAP.get(max_prec);
        System.out.println("dec_width:" + dec_width);

        // get the whole_width
        whole_width = int_width + dec_width;
        System.out.println("whole_width:" + whole_width);

        // get the col/bytes needed
        col_cnt = whole_width / 8;
        if (whole_width % 8 != 0) {
            col_cnt++;
        }
        System.out.println("col_cnt:" + col_cnt);
        System.out.println("--------HEAD SAMPLE RESULT--------end");
    }

    public byte[][] encode(String[] str_dbs) {
        byte[][] cols = new byte[col_cnt][str_dbs.length]; // 第一维代表列号，第二维代表行号

        int db_cnt = 0;
        for (String str_db : str_dbs) {
            double db = Double.parseDouble(str_db);
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
            int prec = get_decimal_place(str_db);
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
            long decimal = (exp >= 0) ? (mantissa << (12 + exp) >>> (64 - dec_width))
                    : (implicit_mantissa >>> 53 - dec_width >>> (Math.abs(exp) - 1));
            if (dec_width != 0)
                System.out.println(
                        "decimal:"
                                + String.format("%" + dec_width + "s", Long.toBinaryString(decimal)).replace(' ', '0'));

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
            long offset = integer_value - lower_bound;
            System.out.println(
                    "offset:" + String.format("%" + int_width + "s", Long.toBinaryString(offset)).replace(' ', '0'));

            // get the bitpack result
            long bitpack = (offset << dec_width) | decimal;
            System.out.println("bitpack:"
                    + String.format("%" + whole_width + "s", Long.toBinaryString(bitpack)).replace(' ', '0'));

            // encode into cols[][]
            int remain = whole_width % 8;
            int bytes_cnt = 0;
            if (remain != 0) {
                bytes_cnt++;
                cols[col_cnt - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK.get(remain));
                System.out.println((col_cnt) - bytes_cnt + "/"
                        + String.format("%" + remain + "s", Long.toBinaryString((bitpack & LAST_MASK.get(remain))))
                                .replace(' ', '0'));
                bitpack = bitpack >>> remain;
            }
            while (bytes_cnt < col_cnt) {
                bytes_cnt++;
                cols[col_cnt - bytes_cnt][db_cnt] = (byte) (bitpack & LAST_MASK.get(8));
                System.out.println(String.format((col_cnt - bytes_cnt) + "/" + "%" + 8 + "s",
                        Long.toBinaryString((bitpack & LAST_MASK.get(8)))).replace(' ', '0'));
                bitpack = bitpack >>> 8;
            }

            db_cnt++;
        }
        return cols;
    }

    public double[] decode(byte[][] cols) {
        System.out.println("----------decode----------");
        double[] dbs = new double[cols[0].length];
        for (int i = 0; i < batch_size; i++) {
            // 逐行提取数据
            long bitpack = 0;
            int remain = whole_width % 8;
            if (remain == 0) {
                for (int j = 0; j < col_cnt; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK.get(8));
                }
            } else {
                for (int j = 0; j < col_cnt - 1; j++) {
                    bitpack = (bitpack << 8) | (cols[j][i] & LAST_MASK.get(8));
                }
                bitpack = (bitpack << remain) | (cols[col_cnt - 1][i] & LAST_MASK.get(remain));
            }
            System.out.println("bitpack:"
                    + String.format("%" + whole_width + "s", Long.toBinaryString(bitpack)).replace(' ', '0'));

            // get the offset
            long offset = bitpack >>> dec_width;
            System.out.println("offset:"
                    + String.format("%" + int_width + "s", Long.toBinaryString(offset)).replace(' ', '0'));

            // get the integer
            long integer = lower_bound + offset;
            System.out.println("integer:" + integer);

            // get the decimal
            long decimal = bitpack << (64 - dec_width) >>> (64 - dec_width);
            System.out.println("decimal:"
                    + String.format("%" + dec_width + "s", Long.toBinaryString(decimal)).replace(' ', '0'));

            // modified decimal [used for - exp]
            long modified_decimal = decimal << (dec_width - get_width_needed(decimal));
            System.out.println("modified_decimal:"
                    + String.format("%" + dec_width + "s", Long.toBinaryString(modified_decimal)).replace(' ', '0'));

            // get the mantissa with implicit bit
            long implicit_mantissa = (integer << (53 - get_width_needed(integer)))
                    | (integer == 0 ? (modified_decimal << (53 - dec_width - get_width_needed(integer)))
                            : (decimal << (53 - dec_width - get_width_needed(integer))));
            System.out.println("implicit_mantissa:"
                    + String.format("%53s", Long.toBinaryString(implicit_mantissa)).replace(' ', '0'));

            // get the mantissa
            long mantissa = implicit_mantissa << 12 >>> 12;
            System.out.println("mantissa:"
                    + String.format("%52s", Long.toBinaryString(mantissa)).replace(' ', '0'));

            // get the sign
            long sign = integer >= 0 ? 0 : 1;

            // get the exp
            long exp = integer != 0 ? (get_width_needed(integer) + 1022)
                    : 1023 - (dec_width - get_width_needed(decimal) + 1);
            System.out.println("exp:" + String.format("%11s", Long.toBinaryString(exp)).replace(' ', '0'));
            System.out.println("exp_value:" + exp);

            // get the origin bits in IEEE754
            long bits = (sign << 63) | (exp << 52) | mantissa;
            System.out.println("bits:" + String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0'));

            // get the origin value
            double db = Double.longBitsToDouble(bits);
            System.out.println(db);
            System.out.println("The Origin Value Is : " + String.format("%." + max_prec + "f", db) + "\n----------");
        }
        return null;
    }

    public static void main(String[] args) {
        Buff buff = new Buff();
        String[] str_dbs = { "0.00007", "10.00001", "0.1415", "199.12", "0.0123", "0.0001", "0",};//
        // };
        // double[] dbs = { 199.12,0.000123};//23.1415, 20.1, 29.12311 ,
        buff.head_sample(str_dbs);
        byte[][] cols = buff.encode(str_dbs);
        buff.decode(cols);
    }
}
