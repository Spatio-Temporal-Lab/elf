package org.urbcomp.startdb.compress.elf;

import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.util.List;

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
        "/ECMWF Interim Full Daily Invariant High Vegetation Cover.csv",
        "/ECMWF Interim Full Daily Invariant Low Vegetation Cover.csv"
    };

    @Test
    public void testCompress() {
        for (String filename : FILENAMES) {
            FileReader fileReader = new FileReader();
            List<Double> values = fileReader.readFile(FILE_PATH + filename);
            ICompressor[] compressors = new ICompressor[]{
//                            new GorillaCompressorOS(),
//                            new ElfOnGorillaCompressorOS(),
//                            new ChimpCompressor(),
//                            new ElfOnChimpCompressor(),
                            new ChimpNCompressor(128),
                            new ElfOnChimpNCompressor(128),
                            new ElfOnChimpNCompressorO(128),
                            new ElfOnChimpNCompressorO(64),
                            new ElfOnChimpNCompressorO(32),
                            new ElfOnChimpNCompressorO(16),
                            new ElfOnChimpNCompressorO(8),
                            new ElfOnChimpNCompressorO(4)
            };
            for(int i = 0;i < compressors.length; i++){
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
//                                new GorillaDecompressorOS(result),
//                                new ElfOnGorillaDecompressorOS(result),
//                                new ChimpDecompressor(result),
//                                new ElfOnChimpDecompressor(result),
                                new ChimpNDecompressor(result,128),
                                new ElfOnChimpNDecompressor(result,128),
                                new ElfOnChimpNDecompressorO(result, 128),
                                new ElfOnChimpNDecompressorO(result, 64),
                                new ElfOnChimpNDecompressorO(result, 32),
                                new ElfOnChimpNDecompressorO(result, 16),
                                new ElfOnChimpNDecompressorO(result, 8),
                                new ElfOnChimpNDecompressorO(result, 4)
                };
                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration += System.nanoTime() - start;

                for (int j = 0; j < values.size(); j++) {
                    assertEquals(values.get(j), uncompressedValues.get(j), "Value did not match");
                }
                System.out.printf("%s: %s \t Compression time per block: %.6f, Decompression time per block: %.6f, Compression Ratio: %.6f%n", compressor.getClass().getSimpleName(), filename, encodingDuration / 1000000.0, decodingDuration / 1000000.0, totalSize / (values.size()*64.0));
            }
            System.out.println();
        }
    }
}
