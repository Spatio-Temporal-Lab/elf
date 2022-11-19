package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.benchmarks.TimeseriesFileReader;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources";
    private static final String[] FILENAMES = {
            "/AvgTemperature.csv",
            "/circuits_lat.csv",
            "/circuits_lng.csv",
            "/diskCapability.csv",
            "/location-lat.csv",
            "/location-long.csv",
            "/mp_price.csv",
            "/pitStop_duration.csv",
            "/worldcities_latitude.csv",
            "/worldcities_longitude.csv",
            "/x-axis.csv",
            "/y-axis.csv",
            "/z-axis.csv",
            "/NewYork_temperature.csv",
            "/Tokyo_temperature.csv",
            "/l4d2_player_stats.csv",
            "/percentage_of_alcohol.csv",
            "/electric_vehicle_charging.csv",
//        "/ECMWF Interim Full Daily Invariant High Vegetation Cover.csv",
//        "/ECMWF Interim Full Daily Invariant Low Vegetation Cover.csv"
    };
    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;

    private static double TIME_PRECISION = 1000.0;
    List<Map<String, List<ResultStructure>>> allResult = new ArrayList<>();


    public void testELFCompressor(String fileName) throws FileNotFoundException {
        FileReader fileReader = new FileReader(fileName);
        int compressorsNum = 10;
        float totalBlocks = 0;
        long[] totalSize = new long[10];
        long[] encodingDuration = new long[10];
        long[] decodingDuration = new long[10];
        double[] values;
        while ((values = fileReader.nextBlock()) != null) {
            ICompressor[] compressors = new ICompressor[]{
                    //new GorillaCompressorOS(),
                    new ElfOnGorillaCompressorOS(),
                    //new ChimpCompressor(),
                    //new ElfOnChimpCompressor(),
                    new ChimpNCompressor(128),
                    new ElfOnChimpNCompressor(128),
                    new ElfCompressor(128),
                    new ElfCompressor(64),
                    new ElfCompressor(32),
                    new ElfCompressor(16),
                    new ElfCompressor(8),
                    new ElfCompressor(4),
                    new ElfCompressor(2),
            };
            for (int i = 0; i < compressors.length; i++) {
                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration[i] += System.nanoTime() - start;

                totalSize[i] += compressor.getSize();
                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
                        //new GorillaDecompressorOS(result),
                        new ElfOnGorillaDecompressorOS(result),
                        //new ChimpDecompressor(result),
                        //new ElfOnChimpDecompressor(result),
                        new ChimpNDecompressor(result, 128),
                        new ElfOnChimpNDecompressor(result, 128),
                        new ElfDecompressor(result, 128),
                        new ElfDecompressor(result, 64),
                        new ElfDecompressor(result, 32),
                        new ElfDecompressor(result, 16),
                        new ElfDecompressor(result, 8),
                        new ElfDecompressor(result, 4),
                        new ElfDecompressor(result, 2)
                };

                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration[i] += System.nanoTime() - start;

                for (int j = 0; j < values.length; j++) {
                    assertEquals(values[j], uncompressedValues.get(j), "Value did not match");
                }
            }
        }


    }
}
