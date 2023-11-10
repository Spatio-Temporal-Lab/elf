package com.github.Tranway.buff;

import java.util.ArrayList;

public class SparseResult {
    boolean flag;
    byte frequent_value;
    byte[] bitmap;
    boolean[] isFrequentValue;
    //    ArrayList<Byte> outliers;
    Byte[] outliers;
    int outliers_cnt = 0;

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public byte getFrequent_value() {
        return frequent_value;
    }

    public void setFrequent_value(int frequent_value) {
        this.frequent_value = (byte) frequent_value;
    }

    public byte[] getBitmap() {
        return bitmap;
    }

    public void setBitmap(byte[] bitmap) {
        this.bitmap = bitmap;
    }

    public boolean[] getIsFrequentValue() {
        return isFrequentValue;
    }

    public void setIsFrequentValue(boolean[] isFrequentValue) {
        this.isFrequentValue = isFrequentValue;
    }

    //    public ArrayList<Byte> getOutliers() {
//        return outliers;
//    }
    public Byte[] getOutliers() {
        return outliers;
    }

//    public void setOutliers(ArrayList<Byte> outliers) {
//        this.outliers = outliers;
//    }

    SparseResult(int batch_size) {
        flag = false;
        bitmap = new byte[batch_size / 8 + 1];
        outliers = new Byte[batch_size];
        isFrequentValue = new boolean[batch_size];
    }
}
