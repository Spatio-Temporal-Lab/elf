package org.urbcomp.startdb.compress.elf;

import gr.aueb.delorean.chimp.benchmarks.TimeseriesFileReader;
import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources";
    private static final String[] FILENAMES = {
            "/AvgTemperature.csv",
//            "/circuits_lat.csv",
//            "/circuits_lng.csv",
//            "/diskCapability.csv",
//            "/location-lat.csv",
//            "/location-long.csv",
//            "/mp_price.csv",
//            "/pitStop_duration.csv",
//            "/worldcities_latitude.csv",
//            "/worldcities_longitude.csv",
//            "/x-axis.csv",
//            "/y-axis.csv",
//            "/z-axis.csv",
//            "/NewYork_temperature.csv",
//            "/Tokyo_temperature.csv",
//            "/l4d2_player_stats.csv",
//            "/percentage_of_alcohol.csv",
//            "/electric_vehicle_charging.csv",
//        "/ECMWF Interim Full Daily Invariant High Vegetation Cover.csv",
//        "/ECMWF Interim Full Daily Invariant Low Vegetation Cover.csv"
    };
    private static final int MINIMUM_TOTAL_BLOCKS = 50_000;
    private static final String STORE_PATH = "src/test/resources/result";

    private static double TIME_PRECISION = 1000.0;
    List<Map<String, List<ResultStructure>>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        for (String filename : FILENAMES) {
            Map<String, List<ResultStructure>> result = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                testELFCompressor(FILE_PATH + filename, result);
                System.out.println(result.get("ChimpNCompressor_128").get(0).getCompressorRatio());
            }
            allResult.add(result);
        }
        storeResult(STORE_PATH+"/result.dat");
    }

    public void storeResult(String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        for (Map<String, List<ResultStructure>> result : allResult) {
            for (List<ResultStructure> ls : result.values()) {
                double[] param = computeDrawParameter(ls);
                fileWriter.write(ls.get(0).getFilename() + "\t");
                fileWriter.write(ls.get(0).getCompressorName());
                for (double p : param) {
                    fileWriter.write("\t" + p);
                }
                fileWriter.write("\n");
            }
        }
        fileWriter.close();
    }


    public void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(fileName);
        ICompressor[] compressorList = new ICompressor[]{
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
        float totalBlocks = 0;
        long[] totalSize = new long[compressorList.length];
        long[] encodingDuration = new long[compressorList.length];
        long[] decodingDuration = new long[compressorList.length];
        double[] values;
        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
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
        for (int i = 0; i < compressorList.length; i++) {
            String key = compressorList[i].getKey();
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize[i] / (totalBlocks * TimeseriesFileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    encodingDuration[i] / totalBlocks,
                    decodingDuration[i] / totalBlocks
            );
            if (!resultCompressor.containsKey(compressorList[i].getKey())) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public double[] computeDrawParameter(List<ResultStructure> result) {
        List<Double> compressTimeList = new ArrayList<>();
        List<Double> decompressTimeList = new ArrayList<>();
        for (ResultStructure r : result) {
            compressTimeList.add(r.getCompressionTime());
            decompressTimeList.add(r.getDecompressionTime());
        }
        double[] drawParam = new double[7];
        drawParam[0] = result.get(0).getCompressorRatio();
        drawParam[1] = medianValue(compressTimeList);
        drawParam[2] = quarterLowValue(compressTimeList);
        drawParam[3] = quarterHighValue(compressTimeList);
        drawParam[4] = medianValue(decompressTimeList);
        drawParam[5] = quarterLowValue(decompressTimeList);
        drawParam[6] = quarterHighValue(decompressTimeList);
        return drawParam;
    }

    public double medianValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        if(num%2==1){
            return ld.get(num/2);
        }
        else {
            return (ld.get(num/2)+ld.get(num/2-1))/2;
        }
    }

    public double quarterLowValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num/4);
    }

    public double quarterHighValue(List<Double> ld) {
        int num = ld.size();
        ld.sort(Comparator.naturalOrder());
        return ld.get(num*3/4);
    }
}
