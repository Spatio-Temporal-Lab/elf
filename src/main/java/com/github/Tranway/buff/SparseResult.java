package com.github.Tranway.buff;

import java.util.Vector;

public class SparseResult {
    boolean flag;
    byte frequent_value;
    byte[] bitmap;
    Vector<Byte> outliers;
    SparseResult(int batch_size) {
        flag = false;
        bitmap = new byte[batch_size / 8];
        outliers = new Vector<>();
    }
}
