package org.urbcomp.startdb.compress.elf.doubleprecision;

import sun.misc.DoubleConsts;

import java.util.Comparator;
import java.util.List;

public class ResultStructure {
    private String filename;
    private final String compressorName;
    private final double compressorRatio;
    private final double maxCompressRatio;
    private final double minCompressRatio;
    private final double compressionTime;
    private final double maxCompressTime;
    private final double minCompressTime;
    private final double mediaCompressTime;
    private final double decompressionTime;
    private final double maxDecompressTime;
    private final double minDecompressTime;
    private final double mediaDecompressTime;

    public ResultStructure(String filename, String compressorName, double compressorRatio, double maxCompressRatio, double minCompressRatio, double compressionTime, double maxCompressTime, double minCompressTime, double mediaCompressTime, double decompressionTime, double maxDecompressTime, double minDecompressTime, double mediaDecompressTime) {
        this.filename = filename;
        this.compressorName = compressorName;
        this.compressorRatio = compressorRatio;
        this.maxCompressRatio = maxCompressRatio;
        this.minCompressRatio = minCompressRatio;
        this.compressionTime = compressionTime;
        this.maxCompressTime = maxCompressTime;
        this.minCompressTime = minCompressTime;
        this.mediaCompressTime = mediaCompressTime;
        this.decompressionTime = decompressionTime;
        this.maxDecompressTime = maxDecompressTime;
        this.minDecompressTime = minDecompressTime;
        this.mediaDecompressTime = mediaDecompressTime;
    }


    public ResultStructure(String filename, String compressorName, double compressorRatio, List<Integer> compressionSize, List<Double> compressionTime, List<Double> decompressionTime) {
        this.filename = filename;
        this.compressorName = compressorName;
        this.compressorRatio = compressorRatio;
        this.maxCompressRatio = maxInt(compressionSize) / (FileReader.DEFAULT_BLOCK_SIZE * 64.0);
        this.minCompressRatio = minInt(compressionSize) / (FileReader.DEFAULT_BLOCK_SIZE * 64.0);
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

    public double getMinCompressTime() {
        return minCompressTime;
    }

    public double getMediaCompressTime() {
        return mediaCompressTime;
    }

    public double getMaxDecompressTime() {
        return maxDecompressTime;
    }

    public double getMinDecompressTime() {
        return minDecompressTime;
    }

    public double getMediaDecompressTime() {
        return mediaDecompressTime;
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

    public double getCompressorRatio() {
        return compressorRatio;
    }

    public double getCompressionTime() {
        return compressionTime;
    }

    public double getDecompressionTime() {
        return decompressionTime;
    }

    public double getMaxCompressRatio() {
        return maxCompressRatio;
    }


    public double getMinCompressRatio() {
        return minCompressRatio;
    }


    public static String getHead() {
        return "FileName\t" +
                "CompressorName\t" +
                "CompressorRatio\t" +
                "MaxCompressRatio\t" +
                "MinCompressRatio\t" +
                "CompressionTime\t" +
                "MaxCompressTime\t" +
                "MinCompressTime\t" +
                "MediaCompressTime\t" +
                "DecompressionTime\t" +
                "MaxDecompressTime\t" +
                "MinDecompressTime\t" +
                "MediaDecompressTime\n";
    }

    @Override
    public String toString() {
        return filename + '\t' +
                compressorName + '\t' +
                compressorRatio + '\t' +
                maxCompressRatio + '\t' +
                minCompressRatio + '\t' +
                compressionTime + '\t' +
                maxCompressTime + '\t' +
                minCompressTime + '\t' +
                mediaCompressTime + '\t' +
                decompressionTime + '\t' +
                maxDecompressTime + '\t' +
                minDecompressTime + '\t' +
                mediaDecompressTime + '\n';
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

    public int maxInt(List<Integer> ld) {
        int max = 0;
        for (Integer aInt : ld) {
            if (aInt > max) {
                max = aInt;
            }
        }
        return max;
    }

    public int minInt(List<Integer> ld) {
        int min = Integer.MAX_VALUE;
        for (Integer aInt : ld) {
            if (aInt < min) {
                min = aInt;
            }
        }
        return min;
    }
}
