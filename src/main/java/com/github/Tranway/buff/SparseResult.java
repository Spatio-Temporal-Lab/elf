package com.github.Tranway.buff;

public class SparseResult {
    public boolean flag;
    public byte frequentValue;
    public byte[] bitmap;
    public boolean[] isFrequentValue;
    public Byte[] outliers;
    public int outliersCnt = 0;

    public void setFrequentValue(int frequentValue) {
        this.frequentValue = (byte) frequentValue;
    }

    public Byte[] getOutliers() {
        return outliers;
    }

    SparseResult(int batch_size) {
        flag = false;
        bitmap = new byte[batch_size / 8 + 1];
        outliers = new Byte[batch_size];
        isFrequentValue = new boolean[batch_size];
    }
}
