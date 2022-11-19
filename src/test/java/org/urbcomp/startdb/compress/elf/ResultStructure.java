package org.urbcomp.startdb.compress.elf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ResultStructure {
    private String filename;
    private String compressorName;
    private double compressorRatio;
    private double compressionTime;
    private double decompressionTime;

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

    public ResultStructure(String filename, String compressorName, double compressorRatio, double compressionTime, double decompressionTime) {
        this.filename = filename;
        this.compressorName = compressorName;
        this.compressorRatio = compressorRatio;
        this.compressionTime = compressionTime;
        this.decompressionTime = decompressionTime;
    }

    public double medianValue(List<Double> ld) {
        ld.sort(Comparator.naturalOrder());
        return (ld.get(49) + ld.get(50)) / 2;
    }

    public double quarterLowValue(List<Double> ld) {
        ld.sort(Comparator.naturalOrder());
        return ld.get(24);
    }

    public double quarterLowCompressTime(List<Double> ld) {
        ld.sort(Comparator.naturalOrder());
        return ld.get(74);
    }
}
