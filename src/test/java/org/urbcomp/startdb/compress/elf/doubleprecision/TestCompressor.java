package org.urbcomp.startdb.compress.elf.doubleprecision;

import com.github.Tranway.buff.BuffCompressor;
import com.github.Tranway.buff.BuffDecompressor;
import com.github.kutschkem.fpc.FpcCompressor;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.io.compress.brotli.BrotliCodec;
import org.apache.hadoop.hbase.io.compress.lz4.Lz4Codec;
import org.apache.hadoop.hbase.io.compress.xerial.SnappyCodec;
import org.apache.hadoop.hbase.io.compress.xz.LzmaCodec;
import org.apache.hadoop.hbase.io.compress.zstd.ZstdCodec;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.CompressionOutputStream;
import org.junit.jupiter.api.Test;
import org.urbcomp.startdb.compress.elf.compressor.*;
import org.urbcomp.startdb.compress.elf.decompressor.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCompressor {
    private static final String FILE_PATH = "src/test/resources/ElfTestData";
    private static final String[] FILENAMES = {
            "/init.csv",    //First run a dataset to ensure the relevant hbase settings of the zstd and snappy compressors
//            "/Air-pressure.csv",
//            "/Air-sensor.csv",
//            "/Basel-temp.csv",
            "/Basel-wind.csv",
            "/Bird-migration.csv",
            "/Bitcoin-price.csv",
//            "/Blockchain-tr.csv",
//            "/City-temp.csv",
            "/City-lat.csv",      //0.2186 -0.2186
//            "/City-lon.csv",        //0.1275 -0.1275
            "/Dew-point-temp.csv",
            "/electric_vehicle_charging.csv",
            "/Food-price.csv",      //1606632.0  1000.0
//            "/IR-bio-temp.csv",
            "/PM10-dust.csv",
            "/SSD-bench.csv",
//            "/POI-lat.csv",
//            "/POI-lon.csv",
            "/Stocks-DE.csv",
//            "/Stocks-UK.csv",
            "/Stocks-USA.csv",
            "/Wind-Speed.csv",
    };
    private static final String STORE_PATH = "src/test/resources/result";

    private static final double TIME_PRECISION = 1000.0;
    List<Map<String, ResultStructure>> allResult = new ArrayList<>();

    @Test
    public void testCompressor() throws IOException {
        for (String filename : FILENAMES) {
            Map<String, List<ResultStructure>> result = new HashMap<>();
            System.out.println(filename);
//            testELFCompressor(filename, result);
            testBuff(filename, result);
//            testFPC(filename, result);
//            testSnappy(filename, result);
//            testZstd(filename, result);
//            testLZ4(filename, result);
//            testBrotli(filename, result);
//            testXz(filename, result);
            for (Map.Entry<String, List<ResultStructure>> kv : result.entrySet()) {
                Map<String, ResultStructure> r = new HashMap<>();
                r.put(kv.getKey(), computeAvg(kv.getValue()));
                allResult.add(r);
            }
            if (result.isEmpty()) {
                System.out.println("The result of the file " + filename +
                        " is empty because the amount of data is less than one block, and the default is at least 1000.");
            }
        }
        storeResult(STORE_PATH + "/resultBuff.csv");
    }


    public void testELFCompressor(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);

        float totalBlocks = 0;
        double[] values;

        HashMap<String, List<Double>> totalCompressionTime = new HashMap<>();
        HashMap<String, List<Double>> totalDecompressionTime = new HashMap<>();
        HashMap<String, List<Integer>> eachBlockCompressionSize = new HashMap<>();
        HashMap<String, Long> key2TotalSize = new HashMap<>();

        while ((values = fileReader.nextBlock()) != null) {
            totalBlocks += 1;
            ICompressor[] compressors = new ICompressor[]{
                    new GorillaCompressorOS(),
                    new ElfOnGorillaPlusCompressorOS(),
                    new ChimpCompressor(),
                    new ElfOnChimpPlusCompressor(),
                    new ChimpNCompressor(128),
                    new ElfOnChimpNPlusCompressor(128),
                    new ElfCompressor(),
                    new ElfPlusCompressor(),
                    new ElfOnChimpNCompressor(128),
                    new ElfOnGorillaCompressorOS(),
                    new ElfOnChimpCompressor(),
            };
            for (int i = 0; i < compressors.length; i++) {
                double encodingDuration;
                double decodingDuration;
                long start = System.nanoTime();
                ICompressor compressor = compressors[i];
                for (double value : values) {
                    compressor.addValue(value);
                }
                compressor.close();

                encodingDuration = System.nanoTime() - start;

                byte[] result = compressor.getBytes();
                IDecompressor[] decompressors = new IDecompressor[]{
                        new GorillaDecompressorOS(result),
                        new ElfOnGorillaPlusDecompressorOS(result),
                        new ChimpDecompressor(result),
                        new ElfOnChimpPlusDecompressor(result),
                        new ChimpNDecompressor(result, 128),
                        new ElfOnChimpNPlusDecompressor(result, 128),
                        new ElfDecompressor(result),
                        new ElfPlusDecompressor(result),
                        new ElfOnChimpNDecompressor(result, 128),
                        new ElfOnGorillaDecompressorOS(result),
                        new ElfOnChimpDecompressor(result)
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
                    eachBlockCompressionSize.put(key, new ArrayList<>());
                    key2TotalSize.put(key, 0L);
                }
                totalCompressionTime.get(key).add(encodingDuration / TIME_PRECISION);
                totalDecompressionTime.get(key).add(decodingDuration / TIME_PRECISION);
                eachBlockCompressionSize.get(key).add(compressor.getSize());
                key2TotalSize.put(key, compressor.getSize() + key2TotalSize.get(key));
            }
        }

        for (Map.Entry<String, Long> kv : key2TotalSize.entrySet()) {
            String key = kv.getKey();
            Long totalSize = kv.getValue();
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize.get(key),
                    totalCompressionTime.get(key),
                    totalDecompressionTime.get(key)
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }


    public void testBuff(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            BuffCompressor compressor = new BuffCompressor();
            // Compress
            long start = System.nanoTime();
            compressor.compress(values);
            encodingDuration += System.nanoTime() - start;

            totalSize += compressor.getSize();
            totalBlocks += 1;

            byte[] result = compressor.getOut();
            BuffDecompressor decompressor = new BuffDecompressor(result);

            double[] dest = new double[FileReader.DEFAULT_BLOCK_SIZE];
            start = System.nanoTime();
            dest = decompressor.decompress();
            decodingDuration += System.nanoTime() - start;
            assertArrayEquals(dest, values);
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add((int) compressor.getSize());
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "FPC";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }


    public void testFPC(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws FileNotFoundException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            FpcCompressor fpc = new FpcCompressor();

            ByteBuffer buffer = ByteBuffer.allocate(FileReader.DEFAULT_BLOCK_SIZE * 10);
            // Compress
            long start = System.nanoTime();
            fpc.compress(buffer, values);
            encodingDuration += System.nanoTime() - start;

            totalSize += buffer.position() * 8L;
            totalBlocks += 1;

            buffer.flip();

            FpcCompressor decompressor = new FpcCompressor();

            double[] dest = new double[FileReader.DEFAULT_BLOCK_SIZE];
            start = System.nanoTime();
            decompressor.decompress(buffer, dest);
            decodingDuration += System.nanoTime() - start;
            assertArrayEquals(dest, values);
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(buffer.position() * 8);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "FPC";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testSnappy(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();

            Configuration conf = HBaseConfiguration.create();
            // ZStandard levels range from 1 to 22.
            // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
            conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
            SnappyCodec codec = new SnappyCodec();
            codec.setConf(conf);

            // Compress
            long start = System.nanoTime();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(compressed.length * 8);

        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Snappy";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testZstd(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();

            Configuration conf = HBaseConfiguration.create();
            // ZStandard levels range from 1 to 22.
            // Level 22 might take up to a minute to complete. 3 is the Hadoop default, and will be fast.
            conf.setInt(CommonConfigurationKeys.IO_COMPRESSION_CODEC_ZSTD_LEVEL_KEY, 3);
            ZstdCodec codec = new ZstdCodec();
            codec.setConf(conf);

            // Compress
            long start = System.nanoTime();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(compressed.length * 8);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Zstd";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testLZ4(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();

            Lz4Codec codec = new Lz4Codec();

            // Compress
            long start = System.nanoTime();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(compressed.length * 8);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "LZ4";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testBrotli(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();

            BrotliCodec codec = new BrotliCodec();

            // Compress
            long start = System.nanoTime();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(compressed.length * 8);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Brotli";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void testXz(String fileName, Map<String, List<ResultStructure>> resultCompressor) throws IOException {
        FileReader fileReader = new FileReader(FILE_PATH + fileName);
        float totalBlocks = 0;
        long totalSize = 0;
        double[] values;
        List<Double> totalCompressionTime = new ArrayList<>();
        List<Double> totalDecompressionTime = new ArrayList<>();
        List<Integer> eachBlockCompressionSize = new ArrayList<>();

        while ((values = fileReader.nextBlock()) != null) {
            double encodingDuration = 0;
            double decodingDuration = 0;
            ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
            for (double d : values) {
                bb.putDouble(d);
            }
            byte[] input = bb.array();

            Configuration conf = new Configuration();
            // LZMA levels range from 1 to 9.
            // Level 9 might take several minutes to complete. 3 is our default. 1 will be fast.
            conf.setInt(LzmaCodec.LZMA_LEVEL_KEY, 3);
            LzmaCodec codec = new LzmaCodec();
            codec.setConf(conf);

            // Compress
            long start = System.nanoTime();
            org.apache.hadoop.io.compress.Compressor compressor = codec.createCompressor();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CompressionOutputStream out = codec.createOutputStream(baos, compressor);
            out.write(input);
            out.close();
            encodingDuration += System.nanoTime() - start;
            final byte[] compressed = baos.toByteArray();
            totalSize += compressed.length * 8L;
            totalBlocks++;

            final byte[] plain = new byte[input.length];
            org.apache.hadoop.io.compress.Decompressor decompressor = codec.createDecompressor();
            start = System.nanoTime();
            CompressionInputStream in = codec.createInputStream(new ByteArrayInputStream(compressed), decompressor);
            IOUtils.readFully(in, plain, 0, plain.length);
            in.close();
            double[] uncompressed = toDoubleArray(plain);
            decodingDuration += System.nanoTime() - start;
            // Decompressed bytes should equal the original
            for (int i = 0; i < values.length; i++) {
                assertEquals(values[i], uncompressed[i], "Value did not match");
            }
            totalCompressionTime.add(encodingDuration / TIME_PRECISION);
            totalDecompressionTime.add(decodingDuration / TIME_PRECISION);
            eachBlockCompressionSize.add(compressed.length * 8);
        }
        if (!totalCompressionTime.isEmpty()) {
            String key = "Xz";
            ResultStructure r = new ResultStructure(fileName, key,
                    totalSize / (totalBlocks * FileReader.DEFAULT_BLOCK_SIZE * 64.0),
                    eachBlockCompressionSize,
                    totalCompressionTime,
                    totalDecompressionTime
            );
            if (!resultCompressor.containsKey(key)) {
                resultCompressor.put(key, new ArrayList<>());
            }
            resultCompressor.get(key).add(r);
        }
    }

    public void storeResult(String filePath) throws IOException {
        File file = new File(filePath).getParentFile();
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Create directory failed: " + file);
        }
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
                lr.get(0).getMaxCompressRatio(),
                lr.get(0).getMinCompressRatio(),
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
