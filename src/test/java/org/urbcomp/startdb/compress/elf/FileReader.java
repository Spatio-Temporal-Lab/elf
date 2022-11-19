package org.urbcomp.startdb.compress.elf;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class FileReader {
    public static final int DEFAULT_BLOCK_SIZE = 1_000;
    private static final String DELIMITER = ",";
    private static final int VALUE_POSITION = 2;
    BufferedReader bufferedReader;
    private int blockSize;
    public FileReader(String filePath,int blockSize) throws FileNotFoundException {
        java.io.FileReader fr = new java.io.FileReader(filePath);
        this.bufferedReader = new BufferedReader(fr);
        this.blockSize = blockSize;
    }

    public FileReader(String filePath) throws FileNotFoundException {
        this(filePath, DEFAULT_BLOCK_SIZE);
    }

    public FileReader(){}

    public double[] nextBlock() {
        double[] values = new double[DEFAULT_BLOCK_SIZE];
        String line;
        int counter = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    double value = Double.parseDouble(line.split(DELIMITER)[VALUE_POSITION]);
                    values[counter++] = value;
                    if (counter == blockSize) {
                        return values;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    continue;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Double> readFile(String filePath) {
        List<Double> ld = new ArrayList<>();
        try (java.io.FileReader fr = new java.io.FileReader(filePath);
                        BufferedReader br = new BufferedReader(fr)) {
            String data;
            while ((data = br.readLine()) != null) {
                ld.add(Double.parseDouble(data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ld;
    }

}
