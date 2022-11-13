package org.urbcomp.startdb.compress.elf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileReader {
    private static final String DELIMITER = ",";
    public List<Double> readFile(String filePath, String fileName, int raw) {
        java.io.FileReader fr = null;
        BufferedReader br = null;
        List<Double> ld = new ArrayList<>();
        try {
            fr = new java.io.FileReader(filePath);
            br = new BufferedReader(fr);

            String data = null;
            boolean first = true;
            while ((data = br.readLine()) != null ) {
                String[] param = data.split(DELIMITER);
                double meta = Double.parseDouble(param[raw]);
                ld.add(meta);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ld;
    }

    public List<String> readFileString(String filePath, int raw) {
        java.io.FileReader fr = null;
        BufferedReader br = null;
        List<String> ld = new ArrayList<>();
        try {
            fr = new java.io.FileReader(filePath);
            br = new BufferedReader(fr);

            String data = null;
            boolean first = true;
            while ((data = br.readLine()) != null ) {
                String[] param = data.split(DELIMITER);
                ld.add(param[raw]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ld;
    }
}
