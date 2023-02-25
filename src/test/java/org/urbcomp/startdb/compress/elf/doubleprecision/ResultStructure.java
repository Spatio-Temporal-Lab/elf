package org.urbcomp.startdb.compress.elf.doubleprecision;

import sun.misc.DoubleConsts;

import java.util.Comparator;
import java.util.List;

public class ResultStructure {
    private String filename;
    private String compressorName;
    private double compressorRatio;
    private double compressionTime;
    private double maxCompressTime;
    private double minCompressTime;
    private double mediaCompressTime;
    private double decompressionTime;
    private double maxDecompressTime;
    private double minDecompressTime;
    private double mediaDecompressTime;


    public ResultStructure(String filename, String compressorName, double compressorRatio, double compressionTime, double maxCompressTime, double minCompressTime, double mediaCompressTime, double decompressionTime, double maxDecompressTime, double minDecompressTime, double mediaDecompressTime) {
        this.filename = filename;
        this.compressorName = compressorName;
        this.compressorRatio = compressorRatio;
        this.compressionTime = compressionTime;
        this.maxCompressTime = maxCompressTime;
        this.minCompressTime = minCompressTime;
        this.mediaCompressTime = mediaCompressTime;
        this.decompressionTime = decompressionTime;
        this.maxDecompressTime = maxDecompressTime;
        this.minDecompressTime = minDecompressTime;
        this.mediaDecompressTime = mediaDecompressTime;
    }

    public ResultStructure(String filename, String compressorName, double compressorRatio, List<Double> compressionTime, List<Double> decompressionTime) {
        this.filename = filename;
        this.compressorName = compressorName;
        this.compressorRatio = compressorRatio;
        this.compressionTime = avgValue(compressionTime);
        this.maxCompressTime = maxValue(compressionTime);
        this.minCompressTime = minValue(compressionTime);
        this.mediaCompressTime = medianValue(compressionTime);
        this.decompressionTime = avgValue(decompressionTime);
        this.maxDecompressTime = maxValue(decompressionTime);
        this.minDecompressTime = minValue(decompressionTime);
        this.mediaDecompressTime = medianValue(decompressionTime);
    }

    public double getMaxCompressTime() {
        return maxCompressTime;
    }

    public void setMaxCompressTime(double maxCompressTime) {
        this.maxCompressTime = maxCompressTime;
    }

    public double getMinCompressTime() {
        return minCompressTime;
    }

    public void setMinCompressTime(double minCompressTime) {
        this.minCompressTime = minCompressTime;
    }

    public double getMediaCompressTime() {
        return mediaCompressTime;
    }

    public void setMediaCompressTime(double mediaCompressTime) {
        this.mediaCompressTime = mediaCompressTime;
    }

    public double getMaxDecompressTime() {
        return maxDecompressTime;
    }

    public void setMaxDecompressTime(double maxDecompressTime) {
        this.maxDecompressTime = maxDecompressTime;
    }

    public double getMinDecompressTime() {
        return minDecompressTime;
    }

    public void setMinDecompressTime(double minDecompressTime) {
        this.minDecompressTime = minDecompressTime;
    }

    public double getMediaDecompressTime() {
        return mediaDecompressTime;
    }

    public void setMediaDecompressTime(double mediaDecompressTime) {
        this.mediaDecompressTime = mediaDecompressTime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getCompressorName() {
        return compressorName;
    }

    public void setCompressorName(String compressorName) {
        this.compressorName = compressorName;
    }

    public double getCompressorRatio() {
        return compressorRatio;
    }

    public void setCompressorRatio(double compressorRatio) {
        this.compressorRatio = compressorRatio;
    }

    public double getCompressionTime() {
        return compressionTime;
    }

    public void setCompressionTime(double compressionTime) {
        this.compressionTime = compressionTime;
    }

    public double getDecompressionTime() {
        return decompressionTime;
    }

    public void setDecompressionTime(double decompressionTime) {
        this.decompressionTime = decompressionTime;
    }

    public static String getHead() {
        return "FileName\t" +
                "CompressorName\t" +
                "CompressorRatio\t" +
                "CompressionTime\t" +
                "MaxCompressTime\t" +
                "MinCompressTime\t" +
                "MediaCompressTime\t" +
                "DecompressionTime\t" +
                "MaxDecompressTime\t" +
                "MinDecompressTime\t" +
                "MediaDecompressTime\t" +
                '\n';
    }
    @Override
    public String toString() {
        return filename + '\t' +
                compressorName + '\t' +
                compressorRatio + '\t' +
                compressionTime + '\t' +
                maxCompressTime + '\t' +
                minCompressTime + '\t' +
                mediaCompressTime + '\t' +
                decompressionTime + '\t' +
                maxDecompressTime + '\t' +
                minDecompressTime + '\t' +
                mediaDecompressTime + '\t' +
                '\n';
    }

    public double medianValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        if (num % 2 == 1) {
            return ld.get(num / 2);
        } else {
            return (ld.get(num / 2) + ld.get(num / 2 - 1)) / 2;
        }
    }

    public double avgValue(List<Double> ld) {
        int num = ld.size();
        double al = 0;
        for (Double aDouble : ld) {
            al += aDouble;
        }
        return al / num;
    }

    public double maxValue(List<Double> ld) {
        int num = ld.size();
        double max = 0;
        for (Double aDouble : ld) {
            if (aDouble > max) {
                max = aDouble;
            }
        }
        return max;
    }

    public double minValue(List<Double> ld) {
        double min = DoubleConsts.MAX_VALUE;
        for (Double aDouble : ld) {
            if (aDouble < min) {
                min = aDouble;
            }
        }
        return min;
    }

    public double quarterLowValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num / 4);
    }

    public double quarterHighValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num * 3 / 4);
    }
}
