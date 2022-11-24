package org.urbcomp.startdb.compress.elf;

import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBeta {
    private static final String FILE_PATH = "src/test/resources/ElfTestData";
    private static final String[] FILENAMES = {
            "/POI-lat.csv",
            "/POI_long.csv",
    };
    private static final String STORE_PATH = "src/test/resources/result";

    private static double TIME_PRECISION = 1000.0;
    List<Map<String, ResultStructure>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        for (String filename : FILENAMES) {
            for (int j = 1; j <= 16; j++) {
                Map<String, List<ResultStructure>> result = new HashMap<>();
                for (int i = 0; i < 100; i++) {
                    testELFCompressor(filename, result, j);
                }
                for (Map.Entry<String, List<ResultStructure>> kv : result.entrySet()) {
                    Map<String, ResultStructure> r = new HashMap<>();
                    r.put(kv.getKey(), computeAvg(kv.getValue()));
                    allResult.add(r);
                }
            }

        }
        storeResult(STORE_PATH + "/resultBeta.dat");
    }

    public void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor, int beta) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        ICompressor[] compressorList = new ICompressor[]{
                new ElfOnGorillaCompressorOS(),
                new ChimpNCompressor(128),
                new ElfCompressor(),
        };
        float totalBlocks = 0;
        long[] totalSize = new long[compressorList.length];
        double[] values;

        HashMap<String, List<Double>> totalCompressionTime = new HashMap<>();
        HashMap<String, List<Double>> totalDecompressionTime = new HashMap<>();
        while ((values = fileReader.nextBlockWithBeta(beta)) != null) {
            totalBlocks += 1;
            ICompressor[] compressors = new ICompressor[]{
                    new ElfOnGorillaCompressorOS(),
                    new ChimpNCompressor(128),
                    new ElfCompressor(),
            };
            for (int i = 0; i < compressors.length; i++) {
                double encodingDuration = 0;
                double decodingDuration = 0;
                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration = System.nanoTime() - start;

                totalSize[i] += compressor.getSize();

                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
//                        new GorillaDecompressorOS(result),
                        new ElfOnGorillaDecompressorOS(result),
                        new ChimpNDecompressor(result, 128),
                        new ElfDecompressor(result)
                };
                IDecompressor decompressor = decompressors[i];

                start = System.nanoTime();
                List<Double> uncompressedValues = decompressor.decompress();
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
            ResultStructure r = new ResultStructure(fileName + " " + beta, key,
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
        FileWriter fileWriter = new FileWriter(filePath);
        for (Map<String, ResultStructure> result : allResult) {
            for (ResultStructure ls : result.values()) {
                fileWriter.write(ls.toString());
            }
        }
        fileWriter.close();
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
}
