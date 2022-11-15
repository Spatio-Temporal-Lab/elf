package org.urbcomp.startdb.compress.elf;


import gr.aueb.delorean.chimp.benchmarks.TimeseriesFileReader;
import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoubleCompressTest {
    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
    private static String FILE_PATH = "src/test/resources";
    private static String[] FILENAMES = {
            "/Average_cost.csv",
            "/AvgTemperature.csv",
            "/circuits_lat.csv",
            "/circuits_lng.csv",
            "/diskCapability.csv",
            "/ECMWF Interim Full Daily Invariant High Vegetation Cover.csv",
            "/ECMWF Interim Full Daily Invariant Low Vegetation Cover.csv",
            "/FLUX_1.csv",
            "/latitude_radian.csv",
            "/location-lat.csv",
            "/location-long.csv",
            "/longitude_radian.csv",
            "/mp_price.csv",
            "/pitStop_duration.csv",
            "/Revenue.csv",
            "/worldcities_latitude.csv",
            "/worldcities_longitude.csv",
            "/x-axis.csv",
            "/y-axis.csv",
            "/z-axis.csv"
    };



    @Test
    public void testCompress() throws IOException {
        for (String filename : FILENAMES) {
            FileReader fileReader = new FileReader();
            List<Double> values;
            values = fileReader.readFile(FILE_PATH+filename, 0);
            ICompressor[] compressors = new ICompressor[]{
                    new ChimpCompressor(),
                    new ChimpNCompressor(128),
                    new GorillaCompressor(),
                    new ElfOnChimpCompressor(),
                    new ElfOnChimpNCompressor(128),
                    new ElfOnGorillaCompressor()
            };
            for(int i=0;i<compressors.length;i++){
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
                        new ChimpDecompressor(result),
                        new ChimpNDecompressor(result,128),
                        new GorillaDecompressor(result),
                        new ElfOnChimpDecompressor(result),
                        new ElfOnChimpNDecompressor(result,128),
                        new ElfOnGorillaDecompressor(result)
                };
                IDecompressor decompressor = decompressors[i];
                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
                decodingDuration += System.nanoTime() - start;
                for (int j = 0; j < values.size(); j++) {
                    assertEquals(values.get(j), uncompressedValues.get(j), "Value did not match");
                }
                System.out.printf("%s: %s - Bits/value: %.6f, Compression time per block: %.6f, Decompression time per block: %.6f%n", compressor.getClass().getSimpleName(),filename, totalSize / (values.size()*64.0), encodingDuration / 1000000.0, decodingDuration / 1000000.0);
            }
        }
    }
}