package org.urbcomp.startdb.compress.elf.doubleprecision;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
    public static final int DEFAULT_BLOCK_SIZE = 1000;
    BufferedReader bufferedReader;
    private final int blockSize;

    public FileReader(String filePath, int blockSize) throws FileNotFoundException {
        java.io.FileReader fr = new java.io.FileReader(filePath);
        this.bufferedReader = new BufferedReader(fr);
        this.blockSize = blockSize;
    }

    public FileReader(String filePath) throws FileNotFoundException {
        this(filePath, DEFAULT_BLOCK_SIZE);
    }


    public double[] nextBlock() {
        double[] values = new double[DEFAULT_BLOCK_SIZE];
        String line;
        int counter = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    values[counter++] = Double.parseDouble(line);
                    if (counter == blockSize) {
                        return values;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double[] nextBlockWithBeta(int beta) {
        double[] values = new double[DEFAULT_BLOCK_SIZE];
        String line;
        int counter = 0;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    values[counter++] = Double.parseDouble(getSubString(line, beta));
                    if (counter == blockSize) {
                        return values;
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
