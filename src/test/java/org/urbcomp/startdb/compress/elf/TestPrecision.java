package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.Chimp;
import gr.aueb.delorean.chimp.ChimpDecompressor;
import gr.aueb.delorean.chimp.ChimpN;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPrecision {
    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
    private static String[] FILENAMES = {
//	        "/city_temperature.csv.gz",
//	        "/Stocks-Germany-sample.txt.gz",
//	        "/SSD_HDD_benchmarks.csv.gz",
            "src/test/resources/migration_original_sub.csv",
//            "src/test/resources/SSD_HDD_benchmarks_sub.csv"
//            "/migration_original_sub.csv.gz"
//            "src/test/resources/taxi_data_sub.csv"
    };

    @Test
    public void testChimp() throws IOException {
        for (String filename : FILENAMES) {
            FileReader fileReader = new FileReader();
            List<Double> values= fileReader.readFile(filename,filename,4);
            long totalSize = 0;
            float totalBlocks = 0;
            long encodingDuration = 0;
            long decodingDuration = 0;
            int[] trailingZeros = new int[65];
                Chimp compressor = new Chimp();
                long start = System.nanoTime();
                for (double value : values) {
                    compressor.addValue(value);
                    trailingZeros[compressor.getLeadingZero()]+=1;
                }
                compressor.close();
                encodingDuration += System.nanoTime() - start;
                totalSize += compressor.getSize();
                totalBlocks += 1;
                ChimpDecompressor d = new ChimpDecompressor(compressor.getOut());
                start = System.nanoTime();
                List<Double> uncompressedValues = d.getValues();
                decodingDuration += System.nanoTime() - start;
//                for(int i=0; i<values.size(); i++) {
//                    assertEquals(java.util.Optional.ofNullable(values.get(i)), uncompressedValues.get(i).doubleValue(), "Value did not match");
//                }
//            System.out.println(String.format("Chimp: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (values.size() * 64), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
            File file=new File("D:\\result\\"+"migration_original_sub_leading.csv");
            try {
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw=new BufferedWriter(fw);
                for(int i=0;i<65;i++){
                    bw.write(String.valueOf(trailingZeros[i]));
                    bw.write(",");
                }
                bw.newLine();
                bw.close();
                fw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Test
    public void testChimp128() throws IOException {
        for (String filename : FILENAMES) {
            FileReader fileReader = new FileReader();
            List<Double> values= fileReader.readFile(filename,filename,4);
            long totalSize = 0;
            float totalBlocks = 0;
            long encodingDuration = 0;
            long decodingDuration = 0;
            int[] trailingZeros = new int[65];
            ChimpN compressor = new ChimpN(128);
            long start = System.nanoTime();
            for (double value : values) {
                compressor.addValue(value);
                trailingZeros[compressor.getLeadingZero()]+=1;
            }
            compressor.close();
            encodingDuration += System.nanoTime() - start;
            totalSize += compressor.getSize();
            totalBlocks += 1;
            ChimpDecompressor d = new ChimpDecompressor(compressor.getOut());
            start = System.nanoTime();
//            List<Double> uncompressedValues = d.getValues();
            decodingDuration += System.nanoTime() - start;
//                for(int i=0; i<values.size(); i++) {
//                    assertEquals(java.util.Optional.ofNullable(values.get(i)), uncompressedValues.get(i).doubleValue(), "Value did not match");
//                }
//            System.out.println(String.format("Chimp: %s - Bits/value: %.2f, Compression time per block: %.2f, Decompression time per block: %.2f", filename, totalSize / (values.size() * 64), encodingDuration / totalBlocks, decodingDuration / totalBlocks));
            File file=new File("D:\\result\\"+"migration_original_sub_leading_128.csv");
            try {
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw=new BufferedWriter(fw);
                for(int i=0;i<65;i++){
                    bw.write(String.valueOf(trailingZeros[i]));
                    bw.write(",");
                }
                bw.newLine();
                bw.close();
                fw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
