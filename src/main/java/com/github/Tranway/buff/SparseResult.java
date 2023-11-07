package com.github.Tranway.buff;

import java.util.ArrayList;
public class SparseResult {
    boolean flag;
    byte frequent_value;
    byte[] bitmap;
    boolean[] isFrequentValue;
    ArrayList<Byte> outliers;
    SparseResult(int batch_size) {
        flag = false;
        bitmap = new byte[batch_size / 8];
        outliers = new ArrayList<>();
        isFrequentValue = new boolean[batch_size];
    }
}
