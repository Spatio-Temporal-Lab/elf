package org.urbcomp.startdb.compress.elf;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class FileReader {
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
