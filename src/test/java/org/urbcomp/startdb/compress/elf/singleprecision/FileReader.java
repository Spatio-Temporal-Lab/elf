package org.urbcomp.startdb.compress.elf.singleprecision;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileReader {
    public static final int DEFAULT_BLOCK_SIZE = 1000;
    private static final String DELIMITER = ",";
    private static final int VALUE_POSITION = 0;
    BufferedReader bufferedReader;
    private int blockSize;

    public FileReader(String filePath, int blockSize) throws FileNotFoundException {
        java.io.FileReader fr = new java.io.FileReader(filePath);
        this.bufferedReader = new BufferedReader(fr);
        this.blockSize = blockSize;
    }

    public FileReader(String filePath) throws FileNotFoundException {
        this(filePath, DEFAULT_BLOCK_SIZE);
    }


    public FileReader() {
    }

    public float[] nextBlock() {
        float[] values = new float[DEFAULT_BLOCK_SIZE];
        String line;
        int counter = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    values[counter++] = Float.parseFloat(line);
                    if (counter == blockSize) {
                        return values;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public float[] nextBlockWithBeta(int beta) {
        float[] values = new float[DEFAULT_BLOCK_SIZE];
        String line;
        int counter = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    values[counter++] = Float.parseFloat(getSubString(line, beta));
                    if (counter == blockSize) {
                        return values;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Double> readFile(String filePath, int number) {
        List<Double> ld = new ArrayList<>();
        int i = 0;
        try (java.io.FileReader fr = new java.io.FileReader(filePath);
             BufferedReader br = new BufferedReader(fr)) {
            String data;
            while ((data = br.readLine()) != null && i < number) {
                ld.add(Double.parseDouble(data));
                i++;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return ld;
    }

    private String getSubString(String str, int beta) {
        if (str.charAt(0) == '-') {
            beta++;
            if (str.charAt(1) == '0') {
                beta++;
            }
        } else if (str.charAt(0) == '0') {
            beta++;
        }
        beta++;
        if (str.length() <= beta) {
            return str;
        } else {
            return str.substring(0, beta);
        }
    }

}
