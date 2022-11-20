package org.urbcomp.startdb.compress.elf;

import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleCompressTest {
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

    @Test
    public void testCompress() {
        HashMap<String, Double> totalCompressionRatio = new HashMap<>();
        HashMap<String, Double> totalCompressionTime = new HashMap<>();
        HashMap<String, Double> totalDecompressionTime = new HashMap<>();

        for (String filename : FILENAMES) {
            FileReader fileReader = new FileReader();
            List<Double> values = fileReader.readFile(FILE_PATH + filename);
            ICompressor[] compressors = new ICompressor[]{
                            new GorillaCompressorOS(),
                            new ElfOnGorillaCompressorOS(),
                            new ChimpCompressor(),
                            new ElfOnChimpCompressor(),
                            new ChimpNCompressor(128),
                            new ElfOnChimpNCompressor(128),
                            new ElfCompressor()
            };

            for (int i = 0;i < compressors.length; i++){
                long totalSize = 0;
                long encodingDuration = 0;
                long decodingDuration = 0;
                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration += System.nanoTime() - start;

                totalSize += compressor.getSize();
                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
                                new GorillaDecompressorOS(result),
                                new ElfOnGorillaDecompressorOS(result),
                                new ChimpDecompressor(result),
                                new ElfOnChimpDecompressor(result),
                                new ChimpNDecompressor(result,128),
                                new ElfOnChimpNDecompressor(result,128),
                                new ElfDecompressor(result)
                };
                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration += System.nanoTime() - start;

                for (int j = 0; j < values.size(); j++) {
                    assertEquals(values.get(j), uncompressedValues.get(j), "Value did not match");
                }
                System.out.printf("%s: %s \t Compression Ratio: %.6f, Compression time per block: %.6f, Decompression time per block: %.6f%n", compressor.getKey(), filename, totalSize / (values.size()*64.0), encodingDuration / 1000000.0, decodingDuration / 1000000.0);

                String key = compressor.getKey();
                if(!totalCompressionRatio.containsKey(key)) {
                    totalCompressionRatio.put(key, totalSize / (values.size()*64.0));
                    totalCompressionTime.put(key, encodingDuration / 1000000.0);
                    totalDecompressionTime.put(key, decodingDuration / 1000000.0);
                } else {
                    totalCompressionRatio.put(key, totalCompressionRatio.get(key) + totalSize / (values.size()*64.0));
                    totalCompressionTime.put(key, totalCompressionTime.get(key) + encodingDuration / 1000000.0);
                    totalDecompressionTime.put(key, totalDecompressionTime.get(key) + decodingDuration / 1000000.0);
                }
            }
            System.out.println();
        }

        System.out.println("Avg Compression Ratio");
        for (Map.Entry<String, Double> kv: totalCompressionRatio.entrySet()) {
            System.out.println(kv.getKey() + ": " + (kv.getValue() / FILENAMES.length));
        }

        System.out.println("\nAvg Compression Time");
        for (Map.Entry<String, Double> kv: totalCompressionTime.entrySet()) {
            System.out.println(kv.getKey() + ": " + (kv.getValue() / FILENAMES.length));
        }

        System.out.println("\nAvg Decompression Time");
        for (Map.Entry<String, Double> kv: totalDecompressionTime.entrySet()) {
            System.out.println(kv.getKey() + ": " + (kv.getValue() / FILENAMES.length));
        }
    }
}
