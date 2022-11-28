package org.urbcomp.startdb.compress.elf.singleprecision;

import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.compressor32.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;
import org.urbcomp.startdb.compress.elf.decompressor32.*;
import org.urbcomp.startdb.compress.elf.doubleprecision.FileReader;
import org.urbcomp.startdb.compress.elf.doubleprecision.ResultStructure;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources/ElfTestData";
    private static final String[] FILENAMES = {
            "/init.csv",    //First run a dataset to ensure the relevant hbase settings of the zstd and snappy compressors
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
    private static final String STORE_PATH = "src/test/resources/result32";

    private static final double TIME_PRECISION = 1000.0;
    List<Map<String, ResultStructure>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        for (String filename : FILENAMES) {
            Map<String, List<ResultStructure>> result = new HashMap<>();
            testELFCompressor(filename, result);
            for (Map.Entry<String, List<ResultStructure>> kv : result.entrySet()) {
                Map<String, ResultStructure> r = new HashMap<>();
                r.put(kv.getKey(), computeAvg(kv.getValue()));
                allResult.add(r);
            }
        }
        storeResult(STORE_PATH + "/result.dat");
    }

    public void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        org.urbcomp.startdb.compress.elf.singleprecision.FileReader fileReader = new org.urbcomp.startdb.compress.elf.singleprecision.FileReader(FILE_PATH + fileName);
        ICompressor32[] compressorList = new ICompressor32[]{
//                new GorillaCompressorOS(),
//                new ElfOnGorillaCompressorOS(),
//                new ChimpCompressor32(),
//                new ElfOnChimpCompressor32(),
//                new ChimpNCompressor32(64),
                new ElfOnChimpNCompressor32(64),
                new ElfCompressor32(),
        };
        float totalBlocks = 0;
        long[] totalSize = new long[compressorList.length];
        float[] values;

        HashMap<String, List<Double>> totalCompressionTime = new HashMap<>();
        HashMap<String, List<Double>> totalDecompressionTime = new HashMap<>();
        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
            ICompressor32[] compressors = new ICompressor32[]{
//                    new GorillaCompressorOS(),
//                    new ElfOnGorillaCompressorOS(),
//                    new ChimpCompressor32(),
//                    new ElfOnChimpCompressor32(),
//                    new ChimpNCompressor32(64),
                    new ElfOnChimpNCompressor32(64),
                    new ElfCompressor32(),
            };
            for (int i = 0; i < compressors.length; i++) {
                double encodingDuration;
                double decodingDuration;
                long start = System.nanoTime();
                ICompressor32 compressor = compressors[i];
                for (float value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration = System.nanoTime() - start;

                totalSize[i] += compressor.getSize();

                byte[] result = compressor.getBytes();
                IDecompressor32[] decompressors = new IDecompressor32[]{
//                        new GorillaDecompressorOS(result),
//                        new ElfOnGorillaDecompressorOS(result),
//                        new ChimpDecompressor32(result),
//                        new ElfOnChimpDecompressor32(result),
//                        new ChimpNDecompressor32(result, 64),
                        new ElfOnChimpNDecompressor32(result, 64),
                        new ElfDecompressor32(result)
                };

                IDecompressor32 decompressor = decompressors[i];

                start = System.nanoTime();
                List<Float> uncompressedValues = decompressor.decompress();
                decodingDuration = System.nanoTime() - start;
                for (int j = 0; j < values.length; j++) {
                    assertEquals(values[j], uncompressedValues.get(j), "Value did not match" + compressor.getKey());
                }
                String key = compressor.getKey();
                if (!totalCompressionTime.containsKey(key)) {
                    totalCompressionTime.put(key, new ArrayList<>());
                    totalDecompressionTime.put(key, new ArrayList<>());
                }
                totalCompressionTime.get(key).add(encodingDuration / TIME_PRECISION);
                totalDecompressionTime.get(key).add(decodingDuration / TIME_PRECISION);
            }
        }
        for (int i = 0; i < compressorList.length; i++) {
            String key = compressorList[i].getKey();
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize[i] / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    totalCompressionTime.get(key),
                    totalDecompressionTime.get(key)
            );
            if (!resultCompressor.containsKey(compressorList[i].getKey())) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }


    public void storeResult(String filePath) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(ResultStructure.getHead());
            for (Map<String, ResultStructure> result : allResult) {
                for (ResultStructure ls : result.values()) {
                    fileWriter.write(ls.toString());
                }
            }
        }
    }

    public ResultStructure computeAvg(List<ResultStructure> lr) {
        int num = lr.size();
        double compressionTime = 0;
        double maxCompressTime = 0;
        double minCompressTime = 0;
        double mediaCompressTime = 0;
        double decompressionTime = 0;
        double maxDecompressTime = 0;
        double minDecompressTime = 0;
        double mediaDecompressTime = 0;
        for (ResultStructure resultStructure : lr) {
            compressionTime += resultStructure.getCompressionTime();
            maxCompressTime += resultStructure.getMaxCompressTime();
            minCompressTime += resultStructure.getMinCompressTime();
            mediaCompressTime += resultStructure.getMediaCompressTime();
            decompressionTime += resultStructure.getDecompressionTime();
            maxDecompressTime += resultStructure.getMaxDecompressTime();
            minDecompressTime += resultStructure.getMinDecompressTime();
            mediaDecompressTime += resultStructure.getMediaDecompressTime();
        }
        return new ResultStructure(lr.get(0).getFilename(),
                lr.get(0).getCompressorName(),
                lr.get(0).getCompressorRatio(),
                compressionTime / num,
                maxCompressTime / num,
                minCompressTime / num,
                mediaCompressTime / num,
                decompressionTime / num,
                maxDecompressTime / num,
                minDecompressTime / num,
                mediaDecompressTime / num
        );
    }

    public static double[] toDoubleArray(byte[] byteArray) {
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).getDouble();
        }
        return doubles;
    }
}
