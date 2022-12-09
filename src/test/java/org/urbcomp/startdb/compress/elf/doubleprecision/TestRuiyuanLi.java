package org.urbcomp.startdb.compress.elf.doubleprecision;

import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.eraser.Eraser;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TestRuiyuanLi {
    private static final String FILE_PATH = "src/test/resources/ElfTestData";
    private static final String[] FILENAMES = {
                    //"/init.csv",    //First run a dataset to ensure the relevant hbase settings of the zstd and snappy compressors
                    "/Air-pressure.csv",
                    "/Air-sensor.csv",
                    "/Basel-temp.csv",
                    "/Basel-wind.csv",
                    "/Bird-migration.csv",
                    "/Bitcoin-price.csv",
                    "/Blockchain-tr.csv",
                    "/City-temp.csv",
                    "/City-lat.csv",
                    "/City-lon.csv",
                    "/Dew-point-temp.csv",
                    "/electric_vehicle_charging.csv",
                    "/Food-price.csv",
                    "/IR-bio-temp.csv",
                    "/PM10-dust.csv",
                    "/SSD-bench.csv",
                    "/POI-lat.csv",
                    "/POI-lon.csv",
                    "/Stocks-DE.csv",
                    "/Stocks-UK.csv",
                    "/Stocks-USA.csv",
                    "/Wind-Speed.csv",
    };

    @Test
    public void testCompressor() throws IOException {
        getMantissaLeadingZerosDistribution();
    }

    private void getMantissaLeadingZerosDistribution() throws FileNotFoundException {
        for(String fileName : FILENAMES) {
            FileReader fileReader = new FileReader(FILE_PATH + fileName);
            int[] counts = new int[53];
            long total = 0;
            double[] values;
            while((values = fileReader.nextBlock()) != null) {
                double last = 0.0;
                for(double value : values) {
                    value = Eraser.erase(value);
                    long xor = Double.doubleToLongBits(last) ^ Double.doubleToLongBits(value);
                    last = value;
                    long mantissaBits = (xor << 12) >>> 12;
                    int leadingZerosNum = Long.numberOfLeadingZeros(mantissaBits) - 12;
                    counts[leadingZerosNum]++;
                }
                total += values.length;
            }
            System.out.print(fileName);
            for(int count : counts) {
                System.out.print("\t" + (count * 1.0 / total));
            }
            System.out.println();
        }
    }

    private void getNonMantissaLeadingZerosDistribution() throws FileNotFoundException {
        for(String fileName : FILENAMES) {
            FileReader fileReader = new FileReader(FILE_PATH + fileName);
            int[] counts = new int[13];
            long total = 0;
            double[] values;
            while((values = fileReader.nextBlock()) != null) {
                double last = 0.0;
                for(double value : values) {
                    long xor = Double.doubleToLongBits(last) ^ Double.doubleToLongBits(value);
                    last = value;
                    long nonMantissaBits = xor >>> 52;
                    int leadingZerosNum = Long.numberOfLeadingZeros(nonMantissaBits) - 52;
                    counts[leadingZerosNum]++;
                }
                total += values.length;
            }
            System.out.print(fileName);
            for(int count : counts) {
                System.out.print("\t" + (count * 1.0 / total));
            }
            System.out.println();
        }
    }
}
